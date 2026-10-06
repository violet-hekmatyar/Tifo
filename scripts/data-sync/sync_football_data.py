#!/usr/bin/env python3
"""football-data.org -> south_stand 数据同步脚本（只依赖标准库）。

与《football-data.org 数据同步方案 v2》一致的行为：

- 业务主键直接使用 football-data.org 的全局 ID（league/team/player/match），天然幂等。
- 输出 SQL 全部是 UPSERT，只覆盖 API 来源字段；follower_count 等用户行为字段不触碰。
- 榜单类纯镜像表按范围先删后插：football_standing / football_player_competition_stat /
  football_competition_stage / team_player；比赛、球队、球员、联赛只 upsert 不删除。
- 用户域 / 内容域 / 互动域（sys_user、content、follow_record 等）完全不在脚本范围内。
- 全局请求间隔 >= 6.5s，时间戳写入 request_log.json，中断重跑会自动补足等待；
  响应按 URL 缓存到 cache/，复跑不重复请求；429 退避 60s 重试，连续 3 次失败即中止。

示例：

    py -3 sync_football_data.py --date-from 2026-01-01 --date-to 2026-10-01 \
        --out ../sql/seed_football_data.sql
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import zlib
from datetime import datetime, timedelta, timezone
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent
CACHE_DIR = BASE_DIR / "cache"
NAME_MAP_PATH = BASE_DIR / "name_map_zh.json"
REQUEST_LOG_PATH = BASE_DIR / "request_log.json"
DEFAULT_OUT = BASE_DIR.parent / "sql" / "seed_football_data.sql"

API_BASE = "https://api.football-data.org/v4"
API_TOKEN_ENV = "FOOTBALL_DATA_API_KEY"
API_TOKEN_ARG = "FOOTBALL_DATA_TOKEN"
# 出现 403 时用来确认 token 是否有效的基础资源（免费套餐内固定包含）
PROBE_PATH = "/competitions/PL"

DB_NAME = "south_stand"
SOURCE_LABEL = "football-data.org"
SOURCE_VALUE = "FOOTBALL_DATA"

# 数据库连接串使用 serverTimezone=Asia/Shanghai，库内 DATETIME 一律按北京时间写入。
CN_TZ = timezone(timedelta(hours=8))

MIN_INTERVAL_SECONDS = 6.5
RATE_LIMIT_SLEEP_SECONDS = 60
MAX_ATTEMPTS = 3
TTL_SHORT = 600
TTL_LONG = 7 * 24 * 3600

COMPETITIONS = {
    "PL": (2021, "LEAGUE"),
    "PD": (2014, "LEAGUE"),
    "BL1": (2002, "LEAGUE"),
    "SA": (2019, "LEAGUE"),
    "FL1": (2015, "LEAGUE"),
    "CL": (2001, "CUP"),
    "WC": (2000, "CUP"),
    "EC": (2018, "CUP"),
}
SORT_ORDER = {code: index + 1 for index, code in enumerate(COMPETITIONS)}

STAGE_ORDER = [
    "PRELIMINARY_ROUND", "REGULAR_SEASON", "LEAGUE_STAGE", "GROUP_STAGE", "ROUND_OF_32", "PLAYOFF",
    "ROUND_OF_16", "QUARTER_FINALS", "SEMI_FINALS", "THIRD_PLACE_PLAY_OFF", "FINAL",
]
# 注意：本列表决定 sort_order 与阶段 ID 的序号位。在中间插入阶段会让其后的阶段换 ID
# （榜单绑定的是 LEAGUE_STAGE/REGULAR_SEASON/GROUP_STAGE，序号不变，所以重跑安全）。
STAGE_ZH = {
    "PRELIMINARY_ROUND": "资格赛",
    "REGULAR_SEASON": "常规赛",
    "LEAGUE_STAGE": "联赛阶段",
    "GROUP_STAGE": "小组赛",
    "ROUND_OF_32": "32强",
    "PLAYOFF": "附加赛",
    "ROUND_OF_16": "16强",
    "QUARTER_FINALS": "8强",
    "SEMI_FINALS": "4强",
    "THIRD_PLACE_PLAY_OFF": "三四名决赛",
    "FINAL": "决赛",
}
# 真实接口里出现过多种写法（已按 cache/ 里的实际响应核对）：
#   欧冠 2025/26 用 PLAYOFFS + LAST_16，世界杯 2026 用 LAST_32 + LAST_16 + THIRD_PLACE。
STAGE_ALIASES = {
    "PLAY_OFF": "PLAYOFF",
    "PLAYOFFS": "PLAYOFF",
    "LAST_16": "ROUND_OF_16",
    "LAST_32": "ROUND_OF_32",
    "THIRD_PLACE": "THIRD_PLACE_PLAY_OFF",
    "QUALIFICATION": "PRELIMINARY_ROUND",
    "QUALIFYING_ROUNDS": "PRELIMINARY_ROUND",
    "PLAY_OFF_ROUND": "PRELIMINARY_ROUND",
}

MATCH_STATUS = {
    "SCHEDULED": "SCHEDULED",
    "TIMED": "SCHEDULED",
    "IN_PLAY": "LIVE",
    "PAUSED": "LIVE",
    "FINISHED": "FINISHED",
    "AWARDED": "FINISHED",
    "SUSPENDED": "SCHEDULED",
    "POSTPONED": "SCHEDULED",
    "CANCELLED": "SCHEDULED",
}

POSITION_EXACT = {
    "Goalkeeper": "GK", "Defence": "DF", "Defender": "DF", "Midfield": "MF", "Midfielder": "MF",
    "Offence": "FW", "Forward": "FW", "Attacker": "FW", "Striker": "FW",
}

GOAL_TYPE = {"REGULAR": "GOAL", "OWN": "OWN_GOAL", "PENALTY": "PENALTY_GOAL"}
CARD_TYPE = {"YELLOW": "YELLOW_CARD", "YELLOW_RED": "YELLOW_RED_CARD", "RED": "RED_CARD"}

# 「重要」tab 依赖 match_info.important_level > 0，脚本按启发式生成（零额外请求）：
#   3 = 欧冠/世界杯/欧洲杯 半决赛及之后；2 = 这些赛事的其他淘汰赛；1 = 联赛积分榜前 6 名之间的对阵。
IMPORTANT_KNOCKOUT_STAGES = {"PLAYOFF", "ROUND_OF_32", "ROUND_OF_16", "QUARTER_FINALS"}
IMPORTANT_LATE_STAGES = {"SEMI_FINALS", "THIRD_PLACE_PLAY_OFF", "FINAL"}
IMPORTANT_LATE_LEVEL = 3
IMPORTANT_KNOCKOUT_LEVEL = 2
IMPORTANT_LEAGUE_CLASH_LEVEL = 1
IMPORTANT_ELITE_RANK = 6
# 某个赛事的阵容接口连续受限多少次后，停止为该赛事剩余球队浪费请求
RESTRICTED_SQUAD_STREAK_LIMIT = 2

# 这些列在 UPDATE 时保留库内已有值，避免增量运行用 NULL 覆盖人工/历史数据。
KEEP_IF_NULL_COLUMNS = {
    "league_name_en", "country", "logo_url", "short_name", "home_stadium", "founded_year",
    "coach_name", "nationality", "position", "birth_date", "round_name", "venue",
}

LEAGUE_COLUMNS = ["id", "league_name", "league_name_en", "country", "logo_url", "season",
                  "league_type", "sort_order"]
SEASON_COLUMNS = ["id", "league_id", "season_code", "season_name", "start_date", "end_date",
                  "current_flag", "source", "source_record_id", "source_updated_at", "synced_at"]
STAGE_COLUMNS = ["id", "league_id", "season_id", "stage_type", "stage_name", "group_code", "sort_order"]
TEAM_COLUMNS = ["id", "team_name", "team_name_en", "short_name", "logo_url", "country",
                "home_stadium", "founded_year", "coach_name", "extra_json"]
PLAYER_COLUMNS = ["id", "player_name", "player_name_en", "nationality", "position", "birth_date",
                  "extra_json"]
TEAM_PLAYER_COLUMNS = ["id", "team_id", "player_id", "team_type", "season", "shirt_number",
                       "position", "status"]
TEAM_SEASON_PLAYER_COLUMNS = ["id", "league_id", "season_id", "team_id", "player_id", "position",
                              "shirt_number", "source", "source_updated_at"]
MATCH_COLUMNS = ["id", "league_id", "season", "round_name", "home_team_id", "away_team_id",
                 "home_score", "away_score", "match_time", "venue", "match_status", "extra_json",
                 "important_level"]
STANDING_COLUMNS = ["id", "league_id", "season_id", "stage_id", "group_code", "team_id", "rank_no",
                    "played", "won", "drawn", "lost", "goals_for", "goals_against",
                    "goal_difference", "points", "form_text", "source", "source_updated_at"]
TEAM_STAT_COLUMNS = ["id", "league_id", "season_id", "stage_id", "team_id", "played", "goals_for",
                     "goals_against", "source", "source_updated_at"]
PLAYER_STAT_COLUMNS = ["id", "league_id", "season_id", "stage_id", "player_id", "team_id",
                       "appearances", "goals", "assists", "source", "source_updated_at"]
EVENT_COLUMNS = ["id", "match_id", "team_id", "player_id", "assist_player_id", "event_type",
                 "minute", "extra_minute", "score_after", "description"]


class Retryable(Exception):
    pass


class Fatal(Exception):
    pass


class Restricted(Exception):
    """HTTP 403：token 无效，或该资源不在当前套餐内。"""


class NotFound(Exception):
    pass


# --------------------------------------------------------------------------- #
# 基础工具
# --------------------------------------------------------------------------- #

def sql_literal(value):
    if value is None:
        return "NULL"
    if isinstance(value, bool):
        return "1" if value else "0"
    if isinstance(value, (int, float)):
        return str(value)
    if isinstance(value, (dict, list)):
        value = json.dumps(value, ensure_ascii=False, sort_keys=True)
    return "'" + str(value).replace("\\", "\\\\").replace("'", "''") + "'"


def rows_from_dicts(columns, items):
    return [[sql_literal(item.get(column)) for column in columns] for item in items]


def render_insert(table, columns, rows, update_columns=None, chunk=500):
    if not rows:
        return ""
    statements = []
    for start in range(0, len(rows), chunk):
        block = rows[start:start + chunk]
        sql = [f"INSERT INTO {table} ({', '.join(columns)}) VALUES"]
        sql.append(",\n".join("  (" + ", ".join(row) + ")" for row in block))
        if update_columns:
            assignments = []
            for column in update_columns:
                if column in KEEP_IF_NULL_COLUMNS:
                    assignments.append(f"{column}=COALESCE(VALUES({column}), {column})")
                else:
                    assignments.append(f"{column}=VALUES({column})")
            sql.append("ON DUPLICATE KEY UPDATE " + ", ".join(assignments))
        else:
            sql.append("ON DUPLICATE KEY UPDATE id=id")
        statements.append("\n".join(sql) + ";")
    return "\n\n".join(statements)


def in_list(values):
    return ", ".join(str(value) for value in sorted(set(values)))


def normalize_stage(raw):
    stage = (raw or "REGULAR_SEASON").upper()
    return STAGE_ALIASES.get(stage, stage)


def stage_seq(stage):
    if stage in STAGE_ORDER:
        return STAGE_ORDER.index(stage) + 1
    # 未知阶段用 CRC32 落到 40..79，避免与固定列表或其他未知阶段撞号。
    return 40 + zlib.crc32(stage.encode("utf-8")) % 40


def group_index(group_code):
    if not group_code:
        return 0
    letter = group_code.replace("GROUP_", "").strip().upper()
    return ord(letter) - ord("A") + 1 if len(letter) == 1 and "A" <= letter <= "Z" else 0


def stage_id_of(competition_id, year, stage):
    return int(f"{competition_id:05d}{year:04d}{stage_seq(stage):02d}00")


def standing_id_of(competition_id, year, stage, group_code, rank):
    return int(f"{competition_id:05d}{year:04d}{stage_seq(stage):02d}{group_index(group_code):02d}1{rank:03d}")


def team_stat_id_of(competition_id, year, stage, group_code, rank):
    return int(f"{competition_id:05d}{year:04d}{stage_seq(stage):02d}{group_index(group_code):02d}3{rank:03d}")


def player_stat_id_of(competition_id, year, stage, index):
    return int(f"{competition_id:05d}{year:04d}{stage_seq(stage):02d}002{index:04d}")


def team_player_id_of(team_id, sequence):
    return int(f"9{team_id:06d}{sequence:04d}")


def team_season_player_id_of(team_id, season_id, player_id):
    """football_team_season_player 的代理主键。表上有 uk_team_season_player(season_id,team_id,player_id)，
    所以这个 ID 只用于新行；宽度不足时直接报错，避免静默撞键。"""
    if team_id >= 10 ** 6 or season_id >= 10 ** 5 or player_id >= 10 ** 6:
        raise ValueError(f"ID 宽度不足：team={team_id} season={season_id} player={player_id}")
    return int(f"7{team_id:06d}{season_id:05d}{player_id:06d}")


def match_event_id_of(match_id, sequence):
    return int(f"9{match_id:09d}{sequence:04d}")


def season_year(season):
    start = str((season or {}).get("startDate") or "")
    return int(start[:4]) if len(start) >= 4 and start[:4].isdigit() else None


def season_name_of(season, year):
    end = str((season or {}).get("endDate") or "")
    end_year = int(end[:4]) if len(end) >= 4 and end[:4].isdigit() else year
    return str(year) if end_year == year else f"{year}/{str(end_year)[-2:]}"


def int_or_zero(value):
    """接口偶尔省略计数字段（例如某射手的 assists 为 null），而库里这些列是 NOT NULL：
    显式写 NULL 会绕过列默认值让整批导入失败，所以缺值一律按 0 写。"""
    try:
        return int(value)
    except (TypeError, ValueError):
        return 0


def map_position(raw):
    if not raw:
        return None
    if raw in POSITION_EXACT:
        return POSITION_EXACT[raw]
    text = raw.lower()
    if "keeper" in text:
        return "GK"
    if "back" in text or "defen" in text:
        return "DF"
    if "midfield" in text:
        return "MF"
    if "forward" in text or "winger" in text or "striker" in text or "offence" in text or "attack" in text:
        return "FW"
    return None


def to_local_time(utc_iso):
    try:
        moment = datetime.fromisoformat(str(utc_iso).replace("Z", "+00:00"))
    except ValueError:
        return None
    if moment.tzinfo is None:
        moment = moment.replace(tzinfo=timezone.utc)
    return moment.astimezone(CN_TZ).strftime("%Y-%m-%d %H:%M:%S")


def round_name_of(stage, matchday, group_code):
    if stage == "REGULAR_SEASON" and matchday:
        return f"第{matchday}轮"
    if stage in ("GROUP_STAGE", "LEAGUE_STAGE") and group_code and matchday:
        return f"{group_code}组 第{matchday}轮"
    return STAGE_ZH.get(stage, stage)


def ttl_for(path):
    parts = path.strip("/").split("/")
    if parts[0] == "teams":
        return TTL_LONG
    if parts[0] == "competitions" and len(parts) == 2:
        return TTL_LONG
    if len(parts) == 3 and parts[0] == "competitions" and parts[2] == "teams":
        return TTL_LONG
    return TTL_SHORT


# --------------------------------------------------------------------------- #
# 限速 + 缓存 + HTTP
# --------------------------------------------------------------------------- #

class Throttle:
    """以 request_log.json 为依据保证全局请求间隔，崩溃重跑也不会突破限速。"""

    def __init__(self, path=None):
        self.path = Path(path or REQUEST_LOG_PATH)
        self.stamps = self._load()

    def _load(self):
        try:
            data = json.loads(self.path.read_text(encoding="utf-8"))
            return [float(item) for item in data][-500:]
        except (OSError, ValueError, TypeError):
            return []

    def wait(self):
        if self.stamps:
            gap = MIN_INTERVAL_SECONDS - (time.time() - self.stamps[-1])
            if gap > 0:
                print(f"    限速：等待 {gap:.1f}s")
                time.sleep(gap)
        self.stamps = (self.stamps + [time.time()])[-500:]
        self.path.write_text(json.dumps(self.stamps), encoding="utf-8")


class Client:
    def __init__(self, token, refresh=False):
        self.token = token
        self.refresh = refresh
        self.throttle = Throttle(REQUEST_LOG_PATH)
        self.http_requests = 0
        self.cache_hits = 0
        self.successes = 0
        self.skipped = []
        self.token_verified = False
        self.probing = False
        self.last_skip_status = None

    def get(self, path, params=None, ttl=None, optional=False):
        url = API_BASE + path + ("?" + urllib.parse.urlencode(params) if params else "")
        ttl = ttl_for(path) if ttl is None else ttl
        self.last_skip_status = None
        if not self.refresh:
            cached = self._read_cache(url, ttl)
            if cached is not None:
                self.cache_hits += 1
                print(f"[cache] {path}")
                return cached
        last_error = None
        for attempt in range(1, MAX_ATTEMPTS + 1):
            self.throttle.wait()
            self.http_requests += 1
            try:
                payload = self._http(url)
                self.successes += 1
            except NotFound:
                if optional:
                    print(f"[skip ] {path} -> 404，本账号/本赛季无数据")
                    self.last_skip_status = 404
                    return None
                raise
            except Restricted as error:
                # 403 有两种成因：token 无效（所有请求都 403）或该资源不在套餐内（只有这个资源 403）。
                # 先确认 token 有效再跳过；确认不了就中止，避免悄悄产出一份空 SQL。
                if optional and self._token_is_usable():
                    print(f"[skip ] {path} -> 403 不在当前套餐内，已跳过")
                    self.skipped.append({"path": path, "status": 403, "detail": str(error)[:200]})
                    self.last_skip_status = 403
                    return None
                raise SystemExit(self._restricted_hint(path, error))
            except Fatal as error:
                raise SystemExit(f"请求被拒绝，已中止：{path} -> {error}")
            except Retryable as error:
                last_error = error
                print(f"[retry] {path} 第 {attempt}/{MAX_ATTEMPTS} 次失败：{error}")
                if attempt < MAX_ATTEMPTS:
                    time.sleep(RATE_LIMIT_SLEEP_SECONDS)
                continue
            self._write_cache(url, payload)
            print(f"[api  ] {path}")
            return payload
        raise SystemExit(f"连续 {MAX_ATTEMPTS} 次请求失败，已中止（已获取的数据与缓存保留）：{last_error}")

    def _token_is_usable(self):
        """出现 403 后判断这是「单个资源受限」还是「token 不可用」。

        本轮已有成功响应就直接判定可用；否则用套餐内的基础资源探测一次（不走缓存，
        因为缓存命中不能证明 token 有效）。
        """
        if self.probing:
            return False
        if self.token_verified or self.successes > 0:
            return True
        self.probing = True
        try:
            self.throttle.wait()
            self.http_requests += 1
            self._http(API_BASE + PROBE_PATH)
        except Restricted:
            return False
        except (Retryable, Fatal, NotFound):
            return True  # 探测本身失败（网络/该赛事不在套餐）：不据此中止整轮
        finally:
            self.probing = False
        self.token_verified = True
        self.successes += 1
        print(f"[probe] {PROBE_PATH} 可达，token 可用，按「单个资源受限」处理")
        return True

    def _restricted_hint(self, path, error):
        return (
            f"HTTP 403 且无法确认 token 可用，已中止：{path} -> {error}\n"
            f"       403 有两种成因：\n"
            f"       ① 环境变量 {API_TOKEN_ENV} 里的 token 无效或已过期——此时所有请求都会 403；\n"
            f"       ② 该资源不在你的套餐内——免费套餐只覆盖固定的 13 个赛事。\n"
            f"       脚本已用 {PROBE_PATH} 做过探测（也失败），所以更可能是 token 问题：\n"
            f"       请确认 token 是否正确/未过期，再重跑；已缓存的响应不会重复请求。")

    def _http(self, url):
        request = urllib.request.Request(url, headers={
            "X-Auth-Token": self.token,
            "Accept": "application/json",
            "User-Agent": "south-stand-data-sync/1.0",
        })
        try:
            with urllib.request.urlopen(request, timeout=30) as response:
                return json.loads(response.read().decode("utf-8"))
        except urllib.error.HTTPError as error:
            detail = error.read().decode("utf-8", "replace")[:200].replace("\n", " ")
            if error.code == 429:
                raise Retryable(f"HTTP 429 触发限速：{detail}")
            if error.code == 404:
                raise NotFound(detail)
            if error.code == 403:
                raise Restricted(f"HTTP 403 {detail}")
            if error.code in (400, 401):
                raise Fatal(f"HTTP {error.code} {detail}")
            raise Retryable(f"HTTP {error.code} {detail}")
        except urllib.error.URLError as error:
            raise Retryable(f"网络错误：{error.reason}")
        except (TimeoutError, OSError) as error:
            raise Retryable(f"连接失败：{error}")
        except json.JSONDecodeError as error:
            raise Retryable(f"响应不是合法 JSON：{error}")

    def _cache_file(self, url):
        return CACHE_DIR / (hashlib.sha1(url.encode("utf-8")).hexdigest() + ".json")

    def _read_cache(self, url, ttl):
        path = self._cache_file(url)
        try:
            payload = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, ValueError):
            return None
        if ttl is not None and time.time() - float(payload.get("fetched_at") or 0) > ttl:
            return None
        return payload.get("data")

    def _write_cache(self, url, payload):
        CACHE_DIR.mkdir(parents=True, exist_ok=True)
        self._cache_file(url).write_text(
            json.dumps({"url": url, "fetched_at": time.time(), "data": payload}, ensure_ascii=False),
            encoding="utf-8")


class NameMap:
    """中英对照表。缺失的 ID 自动补空值，SQL 先用英文名兜底，人工再补译。"""

    def __init__(self, path=None):
        self.path = Path(path or NAME_MAP_PATH)
        self.data = self._load()
        self.dirty = False

    def _load(self):
        try:
            data = json.loads(self.path.read_text(encoding="utf-8"))
            if not isinstance(data, dict):
                data = {}
        except (OSError, ValueError):
            data = {}
        for key in ("competitions", "teams", "players"):
            data.setdefault(key, {})
        return data

    def get(self, kind, key, fallback):
        table = self.data[kind]
        key = str(key)
        if key not in table:
            table[key] = None
            self.dirty = True
        return table[key] or fallback

    def save(self):
        if not self.dirty:
            return
        self.path.write_text(json.dumps(self.data, ensure_ascii=False, indent=2) + "\n",
                             encoding="utf-8", newline="\n")
        print("[map  ] name_map_zh.json 已追加缺失 ID（中文名置空，待人工补译）")


# --------------------------------------------------------------------------- #
# 同步主体
# --------------------------------------------------------------------------- #

class Sync:
    def __init__(self, client, names, date_from, date_to, with_events=False, events_limit=0):
        self.client = client
        self.names = names
        self.date_from = date_from
        self.date_to = date_to
        self.with_events = with_events
        self.events_limit = events_limit

        self.leagues = {}
        self.seasons = {}
        self.stages = {}
        self.teams = {}
        self.players = {}
        self.rosters = {}
        self.season_players = {}
        self.season_player_scopes = {}
        self.season_index = {}   # (league_id, season_code) -> season_id
        self.matches = {}
        self.standings = {}
        self.team_stats = {}
        self.player_stats = {}
        self.events = {}
        self.touched_seasons = {}
        self.roster_scopes = {}
        self.event_matches = []
        self.season_years = {}
        self.now = datetime.now(CN_TZ).strftime("%Y-%m-%d %H:%M:%S")

    # ---------------- 采集 ----------------

    def run(self, codes):
        for code in codes:
            self.sync_competition(code)
        self.apply_important_levels()
        if self.with_events:
            self.sync_events()

    def apply_important_levels(self):
        """按启发式给比赛打「重要」等级，数据都已在内存里，不产生额外请求。"""
        elite = set()
        for row in self.standings.values():
            if row["rank_no"] > IMPORTANT_ELITE_RANK:
                continue
            league = self.leagues.get(row["league_id"]) or {}
            season = self.seasons.get(row["season_id"])
            if league.get("league_type") == "LEAGUE" and season:
                elite.add((row["league_id"], season["season_code"], row["team_id"]))
        counts = {0: 0, 1: 0, 2: 0, 3: 0}
        for match in self.matches.values():
            match["important_level"] = self.important_level_of(match, elite)
            counts[match["important_level"]] += 1
        print(f"\n[重要 ] 重要等级分布 {counts}（3=半决赛及之后 2=其他淘汰赛 1=联赛前6对阵 0=普通）")

    def important_level_of(self, match, elite):
        league = self.leagues.get(match["league_id"]) or {}
        if league.get("league_type") == "CUP":
            stage = self.stages.get((match.get("extra_json") or {}).get("stageId")) or {}
            stage_type = stage.get("stage_type")
            if stage_type in IMPORTANT_LATE_STAGES:
                return IMPORTANT_LATE_LEVEL
            if stage_type in IMPORTANT_KNOCKOUT_STAGES:
                return IMPORTANT_KNOCKOUT_LEVEL
        clash = lambda team_id: (match["league_id"], match["season"], team_id) in elite
        if clash(match["home_team_id"]) and clash(match["away_team_id"]):
            return IMPORTANT_LEAGUE_CLASH_LEVEL
        return 0

    def sync_competition(self, code):
        competition_id, league_type = COMPETITIONS[code]
        print(f"\n=== {code} (competition {competition_id}) ===")
        competition = self.client.get(f"/competitions/{code}", optional=True)
        if competition is None:
            return
        competition_id = competition.get("id") or competition_id
        current_season = competition.get("currentSeason") or {}

        self.leagues[competition_id] = {
            "id": competition_id,
            "league_name": self.names.get("competitions", code, competition.get("name")),
            "league_name_en": competition.get("name"),
            "country": (competition.get("area") or {}).get("name"),
            "logo_url": competition.get("emblem"),
            "season": str(season_year(current_season) or ""),
            "league_type": league_type,
            "sort_order": SORT_ORDER[code],
        }

        season_keys = {}
        matches_payload = self.client.get(
            f"/competitions/{code}/matches",
            {"dateFrom": self.date_from, "dateTo": self.date_to},
            ttl=TTL_SHORT,
            optional=True)
        matches = (matches_payload or {}).get("matches") or []
        for match in matches:
            year = season_year(match.get("season"))
            if not year:
                continue
            season_keys[year] = match.get("season")
            self.add_match(match, competition_id)
        if not season_keys and season_year(current_season):
            season_keys[season_year(current_season)] = current_season

        for year in sorted(season_keys):
            self.ensure_season(competition_id, season_keys[year], year)

        teams_payload = self.client.get(f"/competitions/{code}/teams", optional=True)
        team_ids = []
        roster_year = season_year(current_season)
        for team in (teams_payload or {}).get("teams") or []:
            team_id = self.add_team(team)
            if not team_id:
                continue
            team_ids.append(team_id)
            # /competitions/{code}/teams 的 payload 自带完整阵容（含国家队），
            # 有它就零额外请求；CL 等少数赛事的 payload 不带阵容，稍后按需调 /teams/{id}。
            if roster_year and team.get("squad"):
                self.roster_rows_of(team_id, str(roster_year), team["squad"])

        for year in sorted(season_keys):
            self.sync_standings(code, competition_id, season_keys[year], year)
        for year in sorted(season_keys):
            self.sync_scorers(code, competition_id, season_keys[year], year)

        if roster_year:
            self.sync_squads(competition_id, team_ids, roster_year)

        print(f"    比赛 {len(matches)} 场 / 球队 {len(team_ids)} 支 / 赛季 {len(season_keys)} 个")

    def sync_standings(self, code, competition_id, season, fallback_year):
        payload = self.client.get(f"/competitions/{code}/standings", {"season": fallback_year},
                                  ttl=TTL_SHORT, optional=True)
        if payload is None:
            return
        resolved = self.ensure_season(competition_id, payload.get("season") or season, fallback_year)
        if resolved is None:
            return
        season_id, year = resolved
        for table in payload.get("standings") or []:
            if (table.get("type") or "TOTAL") != "TOTAL":
                continue
            stage = normalize_stage(table.get("stage"))
            group_code = (table.get("group") or "").replace("GROUP_", "")
            stage_id = self.ensure_stage(competition_id, season_id, stage)
            for row in table.get("table") or []:
                team = row.get("team") or {}
                team_id = team.get("id")
                rank = row.get("position")
                if not team_id or not rank:
                    continue
                self.add_team(team)
                self.standings[(competition_id, season_id, stage_id, group_code, team_id)] = {
                    "id": standing_id_of(competition_id, year, stage, group_code, rank),
                    "league_id": competition_id,
                    "season_id": season_id,
                    "stage_id": stage_id,
                    "group_code": group_code,
                    "team_id": team_id,
                    "rank_no": rank,
                    "played": int_or_zero(row.get("playedGames")),
                    "won": int_or_zero(row.get("won")),
                    "drawn": int_or_zero(row.get("draw")),
                    "lost": int_or_zero(row.get("lost")),
                    "goals_for": int_or_zero(row.get("goalsFor")),
                    "goals_against": int_or_zero(row.get("goalsAgainst")),
                    "goal_difference": int_or_zero(row.get("goalDifference")),
                    "points": int_or_zero(row.get("points")),
                    "form_text": (row.get("form") or "")[:32] or None,
                    "source": SOURCE_VALUE,
                    "source_updated_at": self.now,
                }
                # 球队榜：积分榜 payload 自带场次/进失球，顺手生成，零额外请求。
                self.team_stats[(competition_id, season_id, stage_id, team_id)] = {
                    "id": team_stat_id_of(competition_id, year, stage, group_code, rank),
                    "league_id": competition_id,
                    "season_id": season_id,
                    "stage_id": stage_id,
                    "team_id": team_id,
                    "played": int_or_zero(row.get("playedGames")),
                    "goals_for": int_or_zero(row.get("goalsFor")),
                    "goals_against": int_or_zero(row.get("goalsAgainst")),
                    "source": SOURCE_VALUE,
                    "source_updated_at": self.now,
                }

    def sync_scorers(self, code, competition_id, season, fallback_year):
        payload = self.client.get(f"/competitions/{code}/scorers", {"season": fallback_year, "limit": 100},
                                  ttl=TTL_SHORT, optional=True)
        if payload is None:
            return
        resolved = self.ensure_season(competition_id, payload.get("season") or season, fallback_year)
        if resolved is None:
            return
        season_id, year = resolved
        # 射手榜是赛季汇总，挂到该赛季的首个阶段，与 App 默认阶段一致。
        stage_id, stage_key = self.season_stage(competition_id, season_id)
        for index, row in enumerate(payload.get("scorers") or [], start=1):
            player = row.get("player") or {}
            team = row.get("team") or {}
            player_id = player.get("id")
            team_id = team.get("id")
            if not player_id:
                continue
            self.add_player(player)
            if team_id:
                self.add_team(team)
            self.player_stats[(competition_id, season_id, stage_id, player_id, team_id or 0)] = {
                "id": player_stat_id_of(competition_id, year, stage_key, index),
                "league_id": competition_id,
                "season_id": season_id,
                "stage_id": stage_id,
                "player_id": player_id,
                "team_id": team_id or 0,
                "appearances": int_or_zero(row.get("playedMatches")),
                "goals": int_or_zero(row.get("goals")),
                "assists": int_or_zero(row.get("assists")),
                "source": SOURCE_VALUE,
                "source_updated_at": self.now,
            }

    def roster_rows_of(self, team_id, season_code, squad):
        """把 squad 列表落成 team_player 行（按 (队伍,赛季) 缓存，跨赛事复用）。"""
        key = (team_id, season_code)
        if key in self.rosters:
            return self.rosters[key]
        rows = []
        for sequence, person in enumerate(squad or [], start=1):
            player_id = person.get("id")
            if not player_id:
                continue
            self.add_player(person)
            rows.append({
                "id": team_player_id_of(team_id, sequence),
                "team_id": team_id,
                "player_id": player_id,
                "team_type": "CLUB",
                "season": season_code,
                "shirt_number": person.get("shirtNumber"),
                "position": map_position(person.get("position")),
                "status": "ACTIVE",
            })
        self.rosters[key] = rows
        return rows

    def attach_roster(self, competition_id, team_id, year):
        """把该队该赛季的阵容挂到本赛事的赛季上。

        除了 plan 里的 team_player，还要写 football_team_season_player：
        App 的球队详情/球员详情读的是后者（FootballDetailService.roster）。
        """
        season_code = str(year)
        rows = self.rosters.get((team_id, season_code))
        season_id = self.season_index.get((competition_id, season_code))
        if not rows or season_id is None:
            return
        scope = self.season_player_scopes.setdefault((competition_id, season_id), [])
        if team_id not in scope:
            scope.append(team_id)
        self.roster_scopes.setdefault((season_code, competition_id), []).append(team_id)
        for row in rows:
            self.season_players[(season_id, team_id, row["player_id"])] = {
                "id": team_season_player_id_of(team_id, season_id, row["player_id"]),
                "league_id": competition_id,
                "season_id": season_id,
                "team_id": team_id,
                "player_id": row["player_id"],
                # position 在库里是 NOT NULL；接口没给位置的极少数情况按中场兜底
                "position": row["position"] or "MF",
                "shirt_number": row["shirt_number"],
                "status": "ACTIVE",
                "source": SOURCE_VALUE,
                "source_updated_at": self.now,
            }

    def sync_squads(self, competition_id, team_ids, year):
        season_code = str(year)
        restricted_streak = 0
        for index, team_id in enumerate(team_ids):
            key = (team_id, season_code)
            if key in self.rosters:
                # None = 本次拿不到（受限/无权限），既不算命中也不写空阵容
                if self.rosters[key] is not None:
                    self.attach_roster(competition_id, team_id, year)
                continue
            payload = self.client.get(f"/teams/{team_id}", ttl=TTL_LONG, optional=True)
            if payload is None:
                self.rosters[key] = None
                if self.client.last_skip_status == 403:
                    restricted_streak += 1
                    # 一次受限基本就是整批受限（例如国家队），连续 2 次后不再浪费请求。
                    if restricted_streak >= RESTRICTED_SQUAD_STREAK_LIMIT:
                        print(f"    该赛事阵容接口连续受限，跳过剩余 {len(team_ids) - index - 1} 支球队的阵容请求")
                        break
                continue
            restricted_streak = 0
            self.roster_rows_of(team_id, season_code, payload.get("squad"))
            self.attach_roster(competition_id, team_id, year)

    def sync_events(self):
        pending = [match for match in sorted(self.matches.values(), key=lambda item: item["match_time"])
                   if match["match_status"] in ("FINISHED", "LIVE")]
        if self.events_limit:
            pending = pending[:self.events_limit]
        print(f"\n=== 比赛事件（{len(pending)} 场，每场 1 次请求）===")
        for index, match in enumerate(pending, start=1):
            if index % 25 == 1:
                print(f"    事件进度 {index}/{len(pending)}")
            try:
                detail = self.client.get(f"/matches/{match['id']}", ttl=TTL_SHORT)
            except (Fatal, Restricted) as error:
                print(f"[skip ] match {match['id']} 事件不可用：{error}")
                continue
            if detail is None:
                continue
            self.event_matches.append(match["id"])
            for sequence, event in enumerate(self.extract_events(detail), start=1):
                self.events[(match["id"], sequence)] = event

    def extract_events(self, detail):
        match_id = detail.get("id")
        rows = []
        for goal in detail.get("goals") or []:
            score = goal.get("score") or {}
            scorer = goal.get("scorer") or {}
            assist = goal.get("assist") or {}
            rows.append({
                "match_id": match_id,
                "team_id": (goal.get("team") or {}).get("id"),
                "player_id": self.add_event_player(scorer),
                "assist_player_id": self.add_event_player(assist),
                "event_type": GOAL_TYPE.get(goal.get("type"), "GOAL"),
                "minute": goal.get("minute") or 0,
                "extra_minute": goal.get("injuryTime"),
                "score_after": f"{score.get('home')}-{score.get('away')}" if score.get("home") is not None else None,
                "description": None,
            })
        for booking in detail.get("bookings") or []:
            rows.append({
                "match_id": match_id,
                "team_id": (booking.get("team") or {}).get("id"),
                "player_id": self.add_event_player(booking.get("player") or {}),
                "assist_player_id": None,
                "event_type": CARD_TYPE.get(booking.get("card"), "YELLOW_CARD"),
                "minute": booking.get("minute") or 0,
                "extra_minute": None,
                "score_after": None,
                "description": None,
            })
        for change in detail.get("substitutions") or []:
            out_player = change.get("playerOut") or {}
            in_player = change.get("playerIn") or {}
            self.add_event_player(out_player)
            rows.append({
                "match_id": match_id,
                "team_id": (change.get("team") or {}).get("id"),
                "player_id": self.add_event_player(in_player),
                "assist_player_id": None,
                "event_type": "SUBSTITUTION",
                "minute": change.get("minute") or 0,
                "extra_minute": None,
                "score_after": None,
                "description": f"{out_player.get('name')} → {in_player.get('name')}",
            })
        rows.sort(key=lambda item: (item["minute"], item["event_type"]))
        for sequence, row in enumerate(rows, start=1):
            row["id"] = match_event_id_of(match_id, sequence)
        return rows

    # ---------------- 采集辅助 ----------------

    def ensure_season(self, competition_id, season, fallback_year=None):
        """写入/复用赛季行。年份一律取自赛季对象自身，避免调用方的年份与之错位。"""
        season_id = (season or {}).get("id")
        if not season_id:
            return None
        year = season_year(season) or fallback_year
        if not year:
            return None
        start = season.get("startDate")
        end = season.get("endDate")
        today = datetime.now(CN_TZ).date().isoformat()
        self.seasons[season_id] = {
            "id": season_id,
            "league_id": competition_id,
            "season_code": str(year),
            "season_name": season_name_of(season, year),
            "start_date": start,
            "end_date": end,
            "current_flag": 1 if start and end and start <= today <= end else 0,
            "source": SOURCE_VALUE,
            "source_record_id": str(season_id),
            "source_updated_at": self.now,
            "synced_at": self.now,
        }
        self.season_years[season_id] = year
        self.season_index[(competition_id, str(year))] = season_id
        self.touched_seasons.setdefault(competition_id, set()).add(season_id)
        return season_id, year

    def ensure_stage(self, competition_id, season_id, stage):
        stage_id = stage_id_of(competition_id, self.season_years[season_id], stage)
        self.stages[stage_id] = {
            "id": stage_id,
            "league_id": competition_id,
            "season_id": season_id,
            "stage_type": stage[:32],
            "stage_name": STAGE_ZH.get(stage, stage),
            "group_code": None,
            "sort_order": stage_seq(stage),
        }
        return stage_id

    def season_stage(self, competition_id, season_id):
        """返回该赛季首个阶段（App 默认阶段）的 (stage_id, stage_key)。"""
        known = [row for row in self.stages.values()
                 if row["season_id"] == season_id and row["league_id"] == competition_id]
        if known:
            row = min(known, key=lambda item: (item["sort_order"], item["id"]))
            return row["id"], row["stage_type"]
        stage_id = self.ensure_stage(competition_id, season_id, "REGULAR_SEASON")
        return stage_id, "REGULAR_SEASON"

    def add_team(self, team):
        """合并写入：standings/matches 里的球队对象只有 5 个字段，
        不能让它们把 teams/scorers 里的 country/venue/coach 覆盖成 NULL。"""
        team_id = team.get("id")
        if not team_id:
            return None
        row = self.teams.get(team_id) or {}
        name = team.get("name")
        coach = (team.get("coach") or {}).get("name")
        area = (team.get("area") or {}).get("name")
        self.teams[team_id] = {
            "id": team_id,
            "team_name": self.names.get("teams", team_id, name) if name else row.get("team_name"),
            "team_name_en": name or row.get("team_name_en"),
            "short_name": team.get("shortName") or team.get("tla") or row.get("short_name"),
            "logo_url": team.get("crest") or row.get("logo_url"),
            "country": area or row.get("country"),
            "home_stadium": team.get("venue") or row.get("home_stadium"),
            "founded_year": team.get("founded") or row.get("founded_year"),
            "coach_name": coach or row.get("coach_name"),
            "extra_json": {"source": SOURCE_LABEL, "lastUpdated": team.get("lastUpdated")},
        }
        return team_id

    def add_player(self, person):
        player_id = person.get("id")
        if not player_id:
            return None
        row = self.players.get(player_id) or {}
        name = person.get("name")
        merged = {
            "player_name": row.get("player_name") or self.names.get("players", player_id, name),
            "player_name_en": row.get("player_name_en") or name,
            "nationality": person.get("nationality") or row.get("nationality"),
            "position": map_position(person.get("position")) or row.get("position"),
            "birth_date": person.get("dateOfBirth") or row.get("birth_date"),
            "extra_json": {"source": SOURCE_LABEL},
        }
        merged["id"] = player_id
        self.players[player_id] = merged
        return player_id

    def add_event_player(self, person):
        player_id = person.get("id")
        if not player_id:
            return None
        if player_id in self.players:
            return player_id
        self.players[player_id] = {
            "id": player_id,
            "player_name": self.names.get("players", player_id, person.get("name")),
            "player_name_en": person.get("name"),
            "nationality": person.get("nationality"),
            "position": None,
            "birth_date": None,
            "extra_json": {"source": SOURCE_LABEL},
        }
        return player_id

    def add_match(self, match, competition_id):
        match_id = match.get("id")
        home = match.get("homeTeam") or {}
        away = match.get("awayTeam") or {}
        if not match_id or not home.get("id") or not away.get("id"):
            return
        resolved = self.ensure_season(competition_id, match.get("season"))
        if resolved is None:
            return
        season_id, year = resolved
        stage = normalize_stage(match.get("stage"))
        group_code = (match.get("group") or "").replace("GROUP_", "")
        stage_id = self.ensure_stage(competition_id, season_id, stage)
        score = (match.get("score") or {}).get("fullTime") or {}
        status_raw = match.get("status")
        self.add_team(home)
        self.add_team(away)
        self.matches[match_id] = {
            "id": match_id,
            "league_id": competition_id,
            "season": str(year),
            "round_name": round_name_of(stage, match.get("matchday"), group_code),
            "home_team_id": home["id"],
            "away_team_id": away["id"],
            "home_score": score.get("home"),
            "away_score": score.get("away"),
            "match_time": to_local_time(match.get("utcDate")),
            "venue": None,
            "match_status": MATCH_STATUS.get(status_raw, "SCHEDULED"),
            "extra_json": {"source": SOURCE_LABEL, "apiStatus": status_raw, "stage": stage,
                           "stageId": stage_id, "matchday": match.get("matchday"),
                           "group": group_code or None, "lastUpdated": match.get("lastUpdated")},
            "important_level": 0,
        }

    # ---------------- 渲染 SQL ----------------

    def render(self, meta):
        sections = []
        header = [
            "-- 由 scripts/data-sync/sync_football_data.py 生成，请勿手工编辑。",
            f"-- 数据源：{SOURCE_LABEL}（免费套餐，请求间隔 >= {MIN_INTERVAL_SECONDS}s）",
            f"-- 生成时间：{self.now}（北京时间）",
            f"-- 比赛时间范围：{self.date_from} ~ {self.date_to}",
            f"-- 赛事：{', '.join(meta['competitions'])}",
            f"-- HTTP 请求 {meta['http_requests']} 次（缓存命中 {meta['cache_hits']} 次）",
            "-- 幂等：可直接重复执行；只覆盖 API 来源字段，不触碰 follower_count 等用户数据。",
            "-- 重要等级：3=半决赛及之后 2=其他淘汰赛 1=联赛前6对阵，其余为 0。",
            "-- 若需退役演示数据，见 scripts/sql/retire-demo-data.sql。",
        ]
        skipped = meta.get("skipped") or []
        if skipped:
            header.append(f"-- 本次跳过 {len(skipped)} 个受限资源（HTTP 403，免费套餐不含）；"
                          f"这些球队只有基础信息、没有阵容：")
            for item in skipped[:10]:
                header.append(f"--   {item['status']} {item['path']}")
        sections.append("\n".join(header))
        sections.append(f"USE {DB_NAME};\n\nSET NAMES utf8mb4;\n\nSTART TRANSACTION;")

        sections.append(render_insert(
            "football_league", LEAGUE_COLUMNS,
            rows_from_dicts(LEAGUE_COLUMNS, [self.leagues[key] for key in sorted(self.leagues)]),
            ["league_name", "league_name_en", "country", "logo_url", "season", "league_type", "sort_order"]))

        sections.append(render_insert(
            "football_season", SEASON_COLUMNS,
            rows_from_dicts(SEASON_COLUMNS, [self.seasons[key] for key in sorted(self.seasons)]),
            ["league_id", "season_code", "season_name", "start_date", "end_date", "current_flag",
             "source", "source_record_id", "source_updated_at", "synced_at"]))

        deletes = []
        for competition_id in sorted(self.touched_seasons):
            season_ids = in_list(self.touched_seasons[competition_id])
            for table in ("football_competition_stage", "football_standing",
                          "football_player_competition_stat"):
                deletes.append(f"DELETE FROM {table} WHERE league_id = {competition_id} "
                               f"AND season_id IN ({season_ids});")
        sections.append("\n".join(deletes))

        sections.append(render_insert(
            "football_competition_stage", STAGE_COLUMNS,
            rows_from_dicts(STAGE_COLUMNS, [self.stages[key] for key in sorted(self.stages)]),
            ["league_id", "season_id", "stage_type", "stage_name", "group_code", "sort_order"]))

        sections.append(render_insert(
            "football_team", TEAM_COLUMNS,
            rows_from_dicts(TEAM_COLUMNS, [self.teams[key] for key in sorted(self.teams)]),
            ["team_name", "team_name_en", "short_name", "logo_url", "country", "home_stadium",
             "founded_year", "coach_name", "extra_json"]))

        sections.append(render_insert(
            "football_player", PLAYER_COLUMNS,
            rows_from_dicts(PLAYER_COLUMNS, [self.players[key] for key in sorted(self.players)]),
            ["player_name", "player_name_en", "nationality", "position", "birth_date", "extra_json"]))

        roster_rows = [row for key in sorted(self.rosters) for row in (self.rosters[key] or [])]
        roster_deletes = []
        for (season_code, competition_id), team_ids in sorted(self.roster_scopes.items()):
            roster_deletes.append(f"DELETE FROM team_player WHERE season = '{season_code}' "
                                  f"AND team_id IN ({in_list(team_ids)});")
        sections.append("\n".join(roster_deletes))
        sections.append(render_insert(
            "team_player", TEAM_PLAYER_COLUMNS,
            rows_from_dicts(TEAM_PLAYER_COLUMNS, roster_rows)))

        # App 的球队/球员详情读这张表（FootballTeamSeasonPlayer），按 (赛事,赛季,球队) 先删后插，
        # 这样转会窗后离队球员会随刷新消失。
        season_player_deletes = []
        for (competition_id, season_id), team_ids in sorted(self.season_player_scopes.items()):
            season_player_deletes.append(
                f"DELETE FROM football_team_season_player WHERE league_id = {competition_id} "
                f"AND season_id = {season_id} AND team_id IN ({in_list(team_ids)});")
        sections.append("\n".join(season_player_deletes))
        sections.append(render_insert(
            "football_team_season_player", TEAM_SEASON_PLAYER_COLUMNS,
            rows_from_dicts(TEAM_SEASON_PLAYER_COLUMNS,
                            [self.season_players[key] for key in sorted(self.season_players)])))

        sections.append(render_insert(
            "match_info", MATCH_COLUMNS,
            rows_from_dicts(MATCH_COLUMNS, [self.matches[key] for key in sorted(self.matches)]),
            ["league_id", "season", "round_name", "home_team_id", "away_team_id", "home_score",
             "away_score", "match_time", "venue", "match_status", "extra_json", "important_level"]))

        sections.append(render_insert(
            "football_standing", STANDING_COLUMNS,
            rows_from_dicts(STANDING_COLUMNS, [self.standings[key] for key in sorted(self.standings)])))

        # 球队榜只 upsert API 提供的三列，库里已有的角球/犯规/评分等明细不被清零。
        sections.append(render_insert(
            "football_team_competition_stat", TEAM_STAT_COLUMNS,
            rows_from_dicts(TEAM_STAT_COLUMNS, [self.team_stats[key] for key in sorted(self.team_stats)]),
            ["played", "goals_for", "goals_against", "source", "source_updated_at"]))

        sections.append(render_insert(
            "football_player_competition_stat", PLAYER_STAT_COLUMNS,
            rows_from_dicts(PLAYER_STAT_COLUMNS, [self.player_stats[key] for key in sorted(self.player_stats)])))

        if self.event_matches:
            sections.append(f"DELETE FROM match_event WHERE match_id IN ({in_list(self.event_matches)});")
        sections.append(render_insert(
            "match_event", EVENT_COLUMNS,
            rows_from_dicts(EVENT_COLUMNS, [self.events[key] for key in sorted(self.events)])))

        sections.append("COMMIT;")
        return "\n\n".join(section for section in sections if section)


# --------------------------------------------------------------------------- #
# 自检
# --------------------------------------------------------------------------- #

def run_self_test():
    assert normalize_stage("play_off") == "PLAYOFF"
    assert normalize_stage("PLAYOFFS") == "PLAYOFF"
    assert normalize_stage("LAST_16") == "ROUND_OF_16"
    assert normalize_stage("LAST_32") == "ROUND_OF_32"
    assert normalize_stage("THIRD_PLACE") == "THIRD_PLACE_PLAY_OFF"
    assert STAGE_ZH["ROUND_OF_32"] == "32强" and STAGE_ZH["ROUND_OF_16"] == "16强"
    assert stage_seq("ROUND_OF_32") < stage_seq("ROUND_OF_16") < stage_seq("QUARTER_FINALS") < stage_seq("FINAL")
    assert "ROUND_OF_32" in IMPORTANT_KNOCKOUT_STAGES
    assert normalize_stage(None) == "REGULAR_SEASON"
    assert map_position("Goalkeeper") == "GK"
    assert map_position("Right Winger") == "FW"
    assert map_position("Centre-Back") == "DF"
    assert map_position("Midfielder") == "MF"
    assert map_position("Nonsense") is None
    assert MATCH_STATUS["TIMED"] == "SCHEDULED"
    assert MATCH_STATUS["IN_PLAY"] == "LIVE"
    assert MATCH_STATUS["POSTPONED"] == "SCHEDULED"
    assert stage_seq("REGULAR_SEASON") == 2
    assert 40 <= stage_seq("SOMETHING_NEW") < 80
    assert stage_id_of(2001, 2025, "FINAL") != stage_id_of(2001, 2026, "FINAL")
    assert stage_id_of(2001, 2025, "FINAL") != stage_id_of(2001, 2025, "SEMI_FINALS")
    # 小组赛同一排名不同小组不能撞主键
    ids = {standing_id_of(2000, 2026, "GROUP_STAGE", group, 1) for group in ("A", "B", "C")}
    assert len(ids) == 3, ids
    assert standing_id_of(2000, 2026, "GROUP_STAGE", "A", 1) != standing_id_of(2000, 2026, "GROUP_STAGE", "A", 2)
    assert team_player_id_of(57, 1) != match_event_id_of(57, 1)
    assert to_local_time("2026-10-01T19:00:00Z") == "2026-10-02 03:00:00"
    assert to_local_time(None) is None
    assert round_name_of("REGULAR_SEASON", 3, "") == "第3轮"
    assert round_name_of("FINAL", 1, "") == "决赛"
    assert round_name_of("GROUP_STAGE", 2, "A") == "A组 第2轮"
    assert season_year({"startDate": "2025-08-15"}) == 2025
    assert season_name_of({"startDate": "2025-08-15", "endDate": "2026-05-24"}, 2025) == "2025/26"
    assert season_name_of({"startDate": "2026-06-11", "endDate": "2026-07-19"}, 2026) == "2026"
    assert sql_literal("O'Brien\\x") == "'O''Brien\\\\x'"
    assert sql_literal(None) == "NULL"
    assert sql_literal({"b": 1, "a": None}) == "'{\"a\": null, \"b\": 1}'"
    assert ttl_for("/teams/57") == TTL_LONG
    assert ttl_for("/competitions/PL") == TTL_LONG
    assert ttl_for("/competitions/PL/teams") == TTL_LONG
    assert ttl_for("/competitions/PL/matches") == TTL_SHORT

    sql = render_insert("football_team", ["id", "team_name", "coach_name"],
                        [["1", "'阿森纳'", "NULL"]], ["team_name", "coach_name"])
    assert "team_name=VALUES(team_name)" in sql
    assert "coach_name=COALESCE(VALUES(coach_name), coach_name)" in sql

    # 球队榜 ID 与积分榜 ID 同一命名空间但不同 kind 位，不能撞主键
    assert team_stat_id_of(2021, 2026, "REGULAR_SEASON", "", 1) == 2021202602003001
    assert standing_id_of(2021, 2026, "REGULAR_SEASON", "", 1) == 2021202602001001
    assert team_stat_id_of(2021, 2026, "REGULAR_SEASON", "", 1) != standing_id_of(2021, 2026, "GROUP_STAGE", "", 1)

    # 重要等级启发式：不依赖网络，直接构造最小状态
    probe = Sync.__new__(Sync)
    probe.leagues = {2021: {"league_type": "LEAGUE"}, 2001: {"league_type": "CUP"}}
    probe.stages = {11: {"stage_type": "FINAL"}, 12: {"stage_type": "QUARTER_FINALS"},
                    13: {"stage_type": "LEAGUE_STAGE"}}
    elite = {(2021, "2026", 57), (2021, "2026", 65)}

    def probe_match(league_id, stage_id, home, away):
        return {"league_id": league_id, "season": "2026", "home_team_id": home,
                "away_team_id": away, "extra_json": {"stageId": stage_id}}

    assert probe.important_level_of(probe_match(2001, 11, 57, 65), elite) == IMPORTANT_LATE_LEVEL
    assert probe.important_level_of(probe_match(2001, 12, 57, 65), elite) == IMPORTANT_KNOCKOUT_LEVEL
    assert probe.important_level_of(probe_match(2001, 13, 57, 65), elite) == 0
    assert probe.important_level_of(probe_match(2021, 13, 57, 65), elite) == IMPORTANT_LEAGUE_CLASH_LEVEL
    assert probe.important_level_of(probe_match(2021, 13, 57, 66), elite) == 0
    assert probe.important_level_of(probe_match(2021, 13, 66, 68), set()) == 0

    import tempfile
    with tempfile.TemporaryDirectory() as tmp:
        path = Path(tmp) / "name_map_zh.json"
        path.write_text(json.dumps({"competitions": {"PL": "英超"}, "teams": {}, "players": {}}),
                        encoding="utf-8")
        names = NameMap(path)
        assert names.get("teams", 57, "Arsenal FC") == "Arsenal FC"
        assert names.get("competitions", "PL", "Premier League") == "英超"
        names.save()
        saved = json.loads(path.read_text(encoding="utf-8"))
        assert saved["teams"]["57"] is None
        assert NameMap(path).get("teams", 57, "Arsenal FC") == "Arsenal FC"

    print("self-test OK")


# --------------------------------------------------------------------------- #
# CLI
# --------------------------------------------------------------------------- #

def parse_args(argv=None):
    parser = argparse.ArgumentParser(description="football-data.org -> south_stand SQL 同步")
    parser.add_argument("--date-from", help="比赛起始日期 YYYY-MM-DD")
    parser.add_argument("--date-to", help="比赛结束日期 YYYY-MM-DD")
    parser.add_argument("--out", default=str(DEFAULT_OUT), help=f"输出 SQL 路径（默认 {DEFAULT_OUT}）")
    parser.add_argument("--competitions", default=",".join(COMPETITIONS),
                        help="赛事代码，逗号分隔，默认全部 8 个")
    parser.add_argument("--api-key", help=f"football-data.org token，默认读环境变量 {API_TOKEN_ENV}")
    parser.add_argument("--with-events", action="store_true", help="抓取已结束比赛的进球/红黄牌/换人事件")
    parser.add_argument("--events-limit", type=int, default=0, help="限制抓取事件的比赛场数，0 表示不限制")
    parser.add_argument("--refresh", action="store_true", help="忽略缓存 TTL，强制重新请求")
    parser.add_argument("--self-test", action="store_true", help="离线自检，不请求网络")
    return parser.parse_args(argv)


def main(argv=None):
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(errors="replace")
    args = parse_args(argv)
    if args.self_test:
        run_self_test()
        return 0
    if not args.date_from or not args.date_to:
        raise SystemExit("必须提供 --date-from 与 --date-to（例如 --date-from 2026-01-01 --date-to 2026-10-01）")
    for value in (args.date_from, args.date_to):
        try:
            datetime.strptime(value, "%Y-%m-%d")
        except ValueError:
            raise SystemExit(f"日期格式必须是 YYYY-MM-DD：{value}")
    if args.date_from > args.date_to:
        raise SystemExit("--date-from 不能晚于 --date-to")

    codes = [code.strip().upper() for code in args.competitions.split(",") if code.strip()]
    unknown = [code for code in codes if code not in COMPETITIONS]
    if unknown:
        raise SystemExit(f"不支持的赛事代码：{', '.join(unknown)}（可选 {', '.join(COMPETITIONS)}）")

    token = args.api_key or os.environ.get(API_TOKEN_ENV) or os.environ.get(API_TOKEN_ARG)
    if not token:
        raise SystemExit(f"缺少 API token：请设置环境变量 {API_TOKEN_ENV}，或使用 --api-key 传入。")

    out_path = Path(args.out)
    names = NameMap()
    client = Client(token, refresh=args.refresh)
    sync = Sync(client, names, args.date_from, args.date_to,
                with_events=args.with_events, events_limit=args.events_limit)

    started = time.time()
    sync.run(codes)
    names.save()

    sql = sync.render({
        "competitions": codes,
        "http_requests": client.http_requests,
        "cache_hits": client.cache_hits,
        "skipped": client.skipped,
    })
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(sql, encoding="utf-8", newline="\n")

    print(f"\n[out  ] {out_path}（{len(sql)} 字符）")
    print(f"[stat ] 联赛 {len(sync.leagues)} / 赛季 {len(sync.seasons)} / 阶段 {len(sync.stages)} / "
          f"球队 {len(sync.teams)} / 球员 {len(sync.players)} / "
          f"阵容 {sum(len(rows or []) for rows in sync.rosters.values())} / "
          f"赛季阵容 {len(sync.season_players)} / "
          f"比赛 {len(sync.matches)} / 积分榜 {len(sync.standings)} / 球队榜 {len(sync.team_stats)} / "
          f"射手榜 {len(sync.player_stats)} / 事件 {len(sync.events)}")
    print(f"[stat ] HTTP 请求 {client.http_requests} 次，缓存命中 {client.cache_hits} 次，"
          f"耗时 {time.time() - started:.0f}s")
    if client.skipped:
        print(f"[warn ] 跳过 {len(client.skipped)} 个受限资源（HTTP 403，免费套餐不含），"
              f"这些球队只写基础信息、不写阵容：")
        for item in client.skipped[:10]:
            print(f"        {item['status']} {item['path']}")
    print("[next ] 导入：mysql --default-character-set=utf8mb4 -uroot -p < " + str(out_path))
    return 0


if __name__ == "__main__":
    sys.exit(main())
