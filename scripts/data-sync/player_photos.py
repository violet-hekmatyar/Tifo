#!/usr/bin/env python3
"""给真实球员补头像：优先用 TheSportsDB 的真实照片，匹配不上的保持为空（App 端用本地首字母头像兜底）。

为什么用 TheSportsDB：
- football-data.org 免费套餐**不提供**球员照片（实测 https://crests.football-data.org/{playerId}.png 全部 404）；
- Wikidata/Wikipedia 在国内网络不可达（实测 query.wikidata.org 超时）；
- TheSportsDB 免费接口可按**球队**批量取球员（含 strCutout 头像、dateBorn 生日、CC 授权字段），
  每支球队 2 次请求（找队 + 取阵容），192 支球队 ≈ 384 次请求。

匹配策略（宁缺毋滥，错头像比没头像更糟）：
1. 先按队名在 TheSportsDB 找球队：同名优先、排除 U21/女足/预备队，其次比国家；
2. 取该队球员列表；
3. 球员姓名归一化后必须完全一致（去音标/标点/大小写）；
4. 双方都有生日时必须相等（强校验）；只有一方有生日时，要求该队内同名球员唯一。

用法：
    py -3 player_photos.py                      # 抓取 + 生成 update_player_avatars.sql
    py -3 player_photos.py --teams-limit 5      # 先小范围试跑
    py -3 player_photos.py --refresh            # 忽略缓存
    py -3 player_photos.py --self-test          # 离线自检（不联网）
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import subprocess
import sys
import time
import unicodedata
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent
CACHE_DIR = BASE_DIR / "cache" / "thesportsdb"
DEFAULT_OUT = BASE_DIR.parent / "sql" / "update_player_avatars.sql"

TSDB_BASE = "https://www.thesportsdb.com/api/v1/json/3"
TSDB_KEY_ENV = "THESPORTSDB_KEY"
UA = "south-stand-data-sync/1.0 (+local development)"
MIN_INTERVAL_SECONDS = 2.1
MAX_ATTEMPTS = 3
TEAM_NOISE = ("u19", "u20", "u21", "u23", "under", "women", " w ", "academy", "reserves",
              "youth", "b team", "ii")
# 队名里这些词是「俱乐部类型」标记，TheSportsDB 的队名常不带，搜索时要能退化
CLUB_TOKENS = {"fc", "afc", "cf", "ac", "as", "ssc", "sc", "sv", "vfb", "vfl", "tsg", "bsc", "fsv",
               "us", "ud", "rc", "cd", "club", "de", "futbol", "ff", "fk", "if", "bk", "sk", "cfc",
               "krc", "rsc", "fk", "cf"} 
DB_NAME = "south_stand"
MYSQL_CONTAINER = os.environ.get("MYSQL_CONTAINER", "apihub-mysql")

TEAM_COLUMNS = ["id", "name", "country"]
PLAYER_COLUMNS = ["id", "name", "dob", "team_id"]


# --------------------------------------------------------------------------- #
# 数据准备：从库里导出我们的球队 / 球员
# --------------------------------------------------------------------------- #

TEAMS_QUERY = (
    "SELECT JSON_ARRAYAGG(JSON_OBJECT('id', id, 'name', team_name_en, 'country', country)) "
    "FROM football_team WHERE is_deleted = 0 AND id < 100000 AND team_name_en IS NOT NULL"
)
PLAYERS_QUERY = (
    "SELECT JSON_ARRAYAGG(JSON_OBJECT('id', p.id, 'name', p.player_name_en, "
    "'dob', DATE_FORMAT(p.birth_date, '%Y-%m-%d'), 'team_id', tp.team_id, "
    "'in_ranks', EXISTS(SELECT 1 FROM football_player_competition_stat s WHERE s.player_id = p.id))) "
    "FROM football_player p JOIN team_player tp ON tp.player_id = p.id "
    "WHERE p.is_deleted = 0 AND tp.is_deleted = 0 AND p.id < 1000000 AND p.player_name_en IS NOT NULL"
)


def load_from_db(container=MYSQL_CONTAINER, database=DB_NAME):
    """用容器里的 mysql 客户端导出 JSON，避免再引一个数据库驱动。"""
    def run(query):
        # 必须带 --default-character-set=utf8mb4：否则客户端按 latin1 输出，欧洲球员名会解码失败
        script = (f'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot -N -B --default-character-set=utf8mb4 '
                  f'{database} -e "{query}"')
        result = subprocess.run(["docker", "exec", container, "sh", "-c", script],
                                capture_output=True, text=True, encoding="utf-8",
                                errors="replace")
        if result.returncode != 0:
            raise SystemExit(f"导出失败：{(result.stderr or '').strip()[:300]}")
        rows = [line for line in (result.stdout or "").splitlines() if line.strip()]
        return json.loads(rows[0]) if rows else []

    teams = [row for row in run(TEAMS_QUERY) if row]
    players = [row for row in run(PLAYERS_QUERY) if row]
    return teams, players


# --------------------------------------------------------------------------- #
# 匹配逻辑（可离线自检）
# --------------------------------------------------------------------------- #

def normalize_name(value):
    """去音标、去标点、压空白、转小写；'Kovačić' 与 'Kovacic' 视为同名。"""
    if not value:
        return ""
    text = unicodedata.normalize("NFKD", value)
    text = "".join(ch for ch in text if not unicodedata.combining(ch))
    text = text.lower().replace(".", " ").replace("-", " ").replace("'", "").replace("’", "")
    return " ".join(text.split())


def is_noise_team(name):
    """梯队/女足/预备队一律排除（'Arsenal U21'、'Chelsea Women'、'Espanyol B'）。"""
    lowered = f" {normalize_name(name)} "
    if any(token in lowered for token in TEAM_NOISE):
        return True
    return lowered.rstrip().endswith((" b", " ii", " iii"))


def significant_tokens(name):
    """去掉俱乐部类型词与赛季数字后的队名关键词（'FC Bayern München' -> {bayern, munchen}）。"""
    return {normalize_name(w) for w in (name or "").split()
            if normalize_name(w) not in CLUB_TOKENS and not w.rstrip(".").isdigit()}


def pick_team(candidates, search_term, our_country, require_country=False):
    """从 TheSportsDB 的候选球队里挑最合适的，挑不到返回 None。

    硬性只接受足球（strSport=Soccer）：同名条目可能是篮球队/电竞队/冰球队
    （实测 'Bayern München' 搜索命中的是篮球队，'Hamburger SV' 命中的是冰球队）。
    相似度：完全同名 > 互相包含 > 共用显著词（TheSportsDB 常用英文名，如 'Bayern Munich'）。
    """
    target = normalize_name(search_term)
    target_tokens = significant_tokens(search_term)
    country = normalize_name(our_country)
    scored = []
    for team in candidates or []:
        name = normalize_name(team.get("strTeam"))
        if not name or is_noise_team(team.get("strTeam")):
            continue
        sport = normalize_name(team.get("strSport"))
        if sport and sport != "soccer":
            continue
        team_country = normalize_name(team.get("strCountry"))
        country_match = bool(country) and team_country == country
        shared = target_tokens & significant_tokens(team.get("strTeam"))
        if name == target:
            score = 8
        elif target in name or name in target:
            score = 6
        elif shared:
            # 只在国家一致时接受「共用关键词」这种弱匹配，避免匹配到同名的别国球队
            if not country_match:
                continue
            score = 3 + 2 * len(shared)
        else:
            continue
        if require_country and country and team_country and not country_match:
            continue
        if country_match:
            score += 2
        scored.append((score, team))
    if not scored:
        return None
    scored.sort(key=lambda item: (-item[0], item[1].get("strTeam") or ""))
    return scored[0][1]


def search_terms(name):
    """把队名拆成几个可尝试的搜索词：全名 → 去掉俱乐部类型词 → 末两词 → 去音标版本。

    TheSportsDB 的队名往往不带 'FC/CF/…' 前缀（我们的 'FC Bayern München' 要退化成 'Bayern München'），
    而且带音标的写法和小写去音标的写法命中结果不同，两种都试。
    """
    words = [w for w in (name or "").split() if w]
    trimmed = [w for w in words if normalize_name(w) not in CLUB_TOKENS and not w.rstrip(".").isdigit()]
    variants = [" ".join(words), " ".join(trimmed)]
    if len(trimmed) > 2:
        variants.append(" ".join(trimmed[-2:]))
    terms, seen = [], set()
    for variant in variants + [normalize_name(v) for v in variants]:
        key = normalize_name(variant)
        # 去音标后的写法与原文写法归一化结果相同，但搜索命中不同，所以按原样去重
        if variant and variant not in seen:
            seen.add(variant)
            terms.append((variant, (len(key.split()) == 1) if key else False))
    return terms


def match_players(our_players, tsdb_players):
    """按「姓名归一化后一致 + 生日交叉校验」匹配，返回 {our_id: tsdb_player}。"""
    by_name = {}
    for tsdb in tsdb_players or []:
        by_name.setdefault(normalize_name(tsdb.get("strPlayer")), []).append(tsdb)

    matched = {}
    for ours in our_players:
        key = normalize_name(ours.get("name"))
        if not key:
            continue
        candidates = by_name.get(key, [])
        if not candidates:
            continue
        our_dob = (ours.get("dob") or "").strip()
        if our_dob:
            exact = [c for c in candidates if (c.get("dateBorn") or "").strip() == our_dob]
            if len(exact) == 1:
                matched[ours["id"]] = exact[0]
            continue
        # 我们没生日：只有该队内同名唯一时才接受，而且对方最好也没生日
        if len(candidates) == 1:
            matched[ours["id"]] = candidates[0]
    return matched


def resized_url(url, size):
    """把 TheSportsDB 的原图换成小尺寸变体：cutout 原图 228KB，small 变体只要 39KB。

    形如 /images/media/player/cutout/<file> → /images/media/player/cutout/small/<file>；
    cutout 与 thumb 都支持这个前缀。
    """
    marker = "/images/media/"
    if not url or marker not in url or f"/{size}/" in url:
        return url
    head, _, tail = url.partition(marker)
    parts = tail.split("/")
    if len(parts) < 3:
        return url
    return f"{head}{marker}{parts[0]}/{parts[1]}/{size}/{parts[-1]}"


def photo_url(tsdb_player):
    raw = (tsdb_player.get("strCutout") or tsdb_player.get("strThumb") or "").strip()
    return resized_url(raw, "small") or None


def attribution(tsdb_player):
    parts = [f"photo:TheSportsDB idPlayer={tsdb_player.get('idPlayer')}"]
    cc = (tsdb_player.get("strCreativeCommons") or "").strip()
    credit = (tsdb_player.get("strCreativeCommonsAttribution") or "").strip()
    if cc:
        parts.append(f"license={cc}")
    if credit:
        parts.append(f"credit={credit}")
    return " ".join(parts)[:500]


# --------------------------------------------------------------------------- #
# TheSportsDB 客户端（限速 + 缓存）
# --------------------------------------------------------------------------- #

class Client:
    def __init__(self, key, refresh=False, delay=MIN_INTERVAL_SECONDS):
        self.key = key
        self.refresh = refresh
        self.delay = delay
        self.requests = 0
        self.cache_hits = 0
        self._last_call = 0.0
        CACHE_DIR.mkdir(parents=True, exist_ok=True)

    def _cache_file(self, path, params):
        raw = path + "?" + urllib.parse.urlencode(sorted(params.items()))
        return CACHE_DIR / (hashlib.sha1(raw.encode("utf-8")).hexdigest() + ".json")

    def get(self, path, params):
        cache = self._cache_file(path, params)
        if not self.refresh and cache.exists():
            self.cache_hits += 1
            return json.loads(cache.read_text(encoding="utf-8"))
        url = f"{TSDB_BASE}/{path}?{urllib.parse.urlencode(params)}"
        last_error = None
        for attempt in range(1, MAX_ATTEMPTS + 1):
            wait = self.delay - (time.time() - self._last_call)
            if wait > 0:
                time.sleep(wait)
            self._last_call = time.time()
            self.requests += 1
            try:
                request = urllib.request.Request(url, headers={"User-Agent": UA,
                                                               "Accept": "application/json"})
                with urllib.request.urlopen(request, timeout=30) as response:
                    payload = json.loads(response.read().decode("utf-8"))
            except urllib.error.HTTPError as error:
                last_error = f"HTTP {error.code}"
                if error.code in (429, 500, 502, 503):
                    time.sleep(5 * attempt)
                    continue
                break
            except (urllib.error.URLError, TimeoutError, OSError) as error:
                last_error = str(error)
                time.sleep(3 * attempt)
                continue
            except json.JSONDecodeError as error:
                last_error = f"响应不是 JSON（key 可能无效）：{error}"
                break
            cache.write_text(json.dumps(payload, ensure_ascii=False), encoding="utf-8")
            return payload
        raise SystemExit(f"TheSportsDB 请求失败：{path} {params} -> {last_error}")


# --------------------------------------------------------------------------- #
# 主流程
# --------------------------------------------------------------------------- #

def visibility_order(players):
    """按「越可能被看到」排序：先射手榜里的球员，再按 id；同一球员出现在多队时只留一条。

    这样即使第二遍（按球员搜索）没跑完，先补上的也是榜单/阵容里最容易看到的人。
    """
    unique = {}
    for player in players:
        unique.setdefault(player["id"], player)
    return sorted(unique.values(), key=lambda item: (0 if item.get("in_ranks") else 1, item["id"]))


def player_pass(client, players, limit, rows, stats):
    """第二遍：按球员姓名搜索（球队接口的球员列表被截断到 10 人，靠这一遍补齐）。"""
    pending = [p for p in players if p["id"] not in rows]
    total = len(pending) if not limit else min(limit, len(pending))
    print(f"\n=== 第二遍：按球员搜索补头像（共 {len(pending)} 人待补，本次跑 {total} 人）===")
    for index, player in enumerate(pending[:total], start=1):
        payload = client.get("searchplayers.php", {"p": player["name"]})
        matched = match_players([player], payload.get("player"))
        tsdb = matched.get(player["id"])
        if not tsdb:
            stats["player_pass_unmatched"] += 1
            continue
        url = photo_url(tsdb)
        if not url:
            stats["no_photo"] += 1
            continue
        if player["id"] in rows:
            continue
        rows[player["id"]] = {"url": url, "remark": attribution(tsdb), "name": tsdb.get("strPlayer")}
        stats["matched"] += 1
        stats["player_pass_matched"] += 1
        if index % 100 == 0 or index == total:
            print(f"    进度 {index}/{total}，累计有头像 {len(rows)} 人")
    return rows


def sync(options):
    teams, players = load_from_db()
    by_team = {}
    for player in players:
        by_team.setdefault(player["team_id"], []).append(player)
    if options.teams_limit:
        teams = teams[:options.teams_limit]

    client = Client(options.key, refresh=options.refresh, delay=options.delay)
    rows = {}
    stats = {"teams": 0, "teams_resolved": 0, "tsdb_players": 0, "matched": 0, "no_photo": 0,
             "unmatched": 0, "player_pass_matched": 0, "player_pass_unmatched": 0}
    started = time.time()
    print(f"=== 第一遍：按球队取阵容（{len(teams)} 支球队）===")
    for index, team in enumerate(teams, start=1):
        stats["teams"] += 1
        ours = by_team.get(team["id"], [])
        if not ours:
            continue
        tsdb_team = None
        used_term = None
        for term, single_word in search_terms(team["name"]):
            found = client.get("searchteams.php", {"t": term})
            tsdb_team = pick_team(found.get("teams"), term, team.get("country"),
                                  require_country=single_word)
            if tsdb_team:
                used_term = term
                break
        if not tsdb_team:
            print(f"[{index}/{len(teams)}] {team['name']}: TheSportsDB 没有对应球队，跳过")
            continue
        stats["teams_resolved"] += 1
        if normalize_name(used_term) != normalize_name(team["name"]):
            print(f"    队名退化搜索：'{team['name']}' -> '{used_term}' -> {tsdb_team['strTeam']}")
        squad = client.get("lookup_all_players.php", {"id": tsdb_team["idTeam"]})
        tsdb_players = squad.get("player") or []
        stats["tsdb_players"] += len(tsdb_players)
        matched = match_players(ours, tsdb_players)
        for player_id, tsdb in matched.items():
            url = photo_url(tsdb)
            if not url:
                stats["no_photo"] += 1
                continue
            rows[player_id] = {"url": url, "remark": attribution(tsdb), "name": tsdb.get("strPlayer")}
            stats["matched"] += 1
        stats["unmatched"] += len(ours) - len(matched)
        print(f"[{index}/{len(teams)}] {team['name']} -> {tsdb_team['strTeam']}: "
              f"阵容 {len(tsdb_players)} 人，匹配上 {len(matched)} 人，累计有头像 {len(rows)}")

    if options.player_pass:
        player_pass(client, visibility_order(players), options.players_limit, rows, stats)

    if not options.no_validate:
        rows = validate_images(rows)

    sql = render_sql(rows)
    options.out.parent.mkdir(parents=True, exist_ok=True)
    options.out.write_text(sql, encoding="utf-8", newline="\n")
    print(f"\n[out  ] {options.out}（{len(rows)} 名球员写入头像）")
    print(f"[stat ] 覆盖球队 {stats['teams_resolved']}/{stats['teams']}，"
          f"TheSportsDB 球队阵容里共 {stats['tsdb_players']} 人，"
          f"有头像 {len(rows)} 人（无图 {stats['no_photo']}，"
          f"其中第二遍补上 {stats['player_pass_matched']} 人）")
    print(f"[stat ] HTTP 请求 {client.requests} 次，缓存命中 {client.cache_hits} 次，"
          f"耗时 {time.time() - started:.0f}s")
    print("[next ] 导入：mysql --default-character-set=utf8mb4 -uroot -p < " + str(options.out))
    return 0


def validate_images(rows, workers=8):
    """检查每个图片 URL 是否真的可用（状态 200 + image/*），坏图直接剔除。

    历史上出现过 App 报 `Invalid image data`：TheSportsDB 会保留失效的文件名，
    返回的不是图片。校验结果缓存在 cache/thesportsdb/image_checks.json。
    """
    cache_path = CACHE_DIR / "image_checks.json"
    try:
        checks = json.loads(cache_path.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        checks = {}

    pending = sorted(url for url in {row["url"] for row in rows.values()} if url not in checks)
    if pending:
        print(f"[check] 校验 {len(pending)} 个图片 URL ...")
        from concurrent.futures import ThreadPoolExecutor

        def probe(url):
            request = urllib.request.Request(url, headers={"User-Agent": UA})
            try:
                with urllib.request.urlopen(request, timeout=20) as response:
                    kind = response.headers.get("Content-Type") or ""
                    return url, response.status == 200 and kind.startswith("image/")
            except Exception:
                return url, False

        with ThreadPoolExecutor(max_workers=workers) as pool:
            for url, ok in pool.map(probe, pending):
                checks[url] = ok
        CACHE_DIR.mkdir(parents=True, exist_ok=True)
        cache_path.write_text(json.dumps(checks, ensure_ascii=False), encoding="utf-8")

    bad = {pid for pid, row in rows.items() if not checks.get(row["url"], True)}
    for player_id in bad:
        rows.pop(player_id, None)
    broken_total = sum(1 for url in checks if not checks[url])
    print(f"[check] 可用图片 {sum(1 for v in checks.values() if v)}/{len(checks)}，"
          f"本次剔除坏图 {len(bad)} 个（累计发现坏图 {broken_total} 个）")
    return rows


def render_sql(rows):
    header = [
        "-- 由 scripts/data-sync/player_photos.py 生成，请勿手工编辑。",
        f"-- 来源：TheSportsDB 免费接口（球员照片），共 {len(rows)} 名球员。",
        "-- 匹配规则：球队名归一化 + 球员姓名归一化 + 生日交叉校验。",
        "-- 没匹配到的球员 avatar_url 保持 NULL，App 端用本地生成的首字母头像兜底。",
        "-- remark 里保留了 TheSportsDB idPlayer 与 CC 授权信息，便于署名核查。",
        "",
        f"USE {DB_NAME};",
        "",
        "SET NAMES utf8mb4;",
        "",
        "START TRANSACTION;",
        "",
    ]
    body = []
    for player_id in sorted(rows):
        row = rows[player_id]
        remark = row["remark"].replace("\\", "\\\\").replace("'", "''")
        url = row["url"].replace("'", "''")
        body.append(f"UPDATE football_player SET avatar_url = '{url}', remark = '{remark}' "
                    f"WHERE id = {player_id};")
    footer = ["", "COMMIT;", ""]
    return "\n".join(header + body + footer)


# --------------------------------------------------------------------------- #
# 自检
# --------------------------------------------------------------------------- #

def run_self_test():
    assert normalize_name("Kovačić") == "kovacic"
    assert normalize_name("João Pedro") == "joao pedro"
    assert normalize_name("O'Brien-White") == "obrien white"
    assert normalize_name("  Erling   Haaland ") == "erling haaland"
    assert is_noise_team("Arsenal U21") and is_noise_team("Chelsea Women")
    assert is_noise_team("Espanyol B") and is_noise_team("Borussia Dortmund II")
    assert not is_noise_team("Lille") and not is_noise_team("Real Betis")
    assert search_terms("FC Bayern München")[0] == ("FC Bayern München", False)
    assert ("Bayern München", False) in search_terms("FC Bayern München")
    assert ("bayern munchen", False) in search_terms("FC Bayern München")
    assert search_terms("1. FC Köln")[1] == ("Köln", True)
    assert search_terms("Bayer 04 Leverkusen")[1] == ("Bayer Leverkusen", False)
    # 同名但非足球：必须拒绝（TheSportsDB 里有 Bayern München Basketball / Schalke esports）
    basketball = [{"strTeam": "Bayern München Basketball", "strCountry": "Germany",
                   "strSport": "Basketball", "idTeam": "b1"}]
    assert pick_team(basketball, "Bayern München", "Germany") is None
    ice_hockey = [{"strTeam": "Hamburger SV", "strCountry": "Germany", "strSport": "Ice Hockey",
                   "idTeam": "h1"}]
    assert pick_team(ice_hockey, "Hamburger SV", "Germany") is None
    # TheSportsDB 用英文名：共用显著词 + 国家一致时接受
    english = [{"strTeam": "Bayern Munich", "strCountry": "Germany", "strSport": "Soccer",
                "idTeam": "b2"}]
    assert pick_team(english, "FC Bayern München", "Germany")["idTeam"] == "b2"
    # 共用关键词但国家不同：拒绝
    other_country = [{"strTeam": "Bayern Munich", "strCountry": "United States",
                      "strSport": "Soccer", "idTeam": "b3"}]
    assert pick_team(other_country, "FC Bayern München", "Germany") is None
    assert not is_noise_team("Arsenal FC")

    candidates = [
        {"strTeam": "Arsenal", "strCountry": "England", "strSport": "Soccer", "idTeam": "1"},
        {"strTeam": "Arsenal U21", "strCountry": "England", "strSport": "Soccer", "idTeam": "2"},
        {"strTeam": "Arsenal", "strCountry": "Ukraine", "strSport": "Soccer", "idTeam": "3"},
        {"strTeam": "Arsenal Tula", "strCountry": "Russia", "strSport": "Soccer", "idTeam": "4"},
    ]
    picked = pick_team(candidates, "Arsenal FC", "England")
    assert picked["idTeam"] == "1", picked
    assert pick_team(candidates, "不存在的球队", "England") is None
    assert "bayern" in significant_tokens("FC Bayern München")
    assert significant_tokens("1. FC Köln") == {"koln"}

    ours = [
        {"id": 1, "name": "Bukayo Saka", "dob": "2001-09-05"},
        {"id": 2, "name": "Declan Rice", "dob": "1999-01-14"},
        {"id": 3, "name": "Someone Else", "dob": "1990-01-01"},
        {"id": 4, "name": "No Dob", "dob": None},
    ]
    tsdb = [
        {"strPlayer": "Bukayo Saka", "dateBorn": "2001-09-05", "strCutout": "http://img/saka.png",
         "idPlayer": "9"},
        # 生日不符：必须拒绝
        {"strPlayer": "Declan Rice", "dateBorn": "1999-01-15", "strCutout": "http://img/wrong.png"},
        # 同名两人且我们没生日：必须拒绝
        {"strPlayer": "No Dob", "dateBorn": None, "strCutout": "http://img/a.png"},
        {"strPlayer": "No Dob", "dateBorn": "1995-05-05", "strCutout": "http://img/b.png"},
    ]
    matched = match_players(ours, tsdb)
    assert 1 in matched and photo_url(matched[1]) == "http://img/saka.png"
    assert 2 not in matched, "生日不一致必须拒绝"
    assert 4 not in matched, "同名多人且缺生日必须拒绝"
    assert 3 not in matched
    assert resized_url("https://r2.thesportsdb.com/images/media/player/cutout/a1.png", "small") ==         "https://r2.thesportsdb.com/images/media/player/cutout/small/a1.png"
    assert resized_url("https://r2.thesportsdb.com/images/media/player/thumb/a1.jpg", "small") ==         "https://r2.thesportsdb.com/images/media/player/thumb/small/a1.jpg"
    # 已经是 small 变体：不动
    assert resized_url("https://r2.thesportsdb.com/images/media/player/cutout/small/a1.png", "small") ==         "https://r2.thesportsdb.com/images/media/player/cutout/small/a1.png"
    assert photo_url({"strCutout": "https://r2.thesportsdb.com/images/media/player/cutout/a1.png"}) ==         "https://r2.thesportsdb.com/images/media/player/cutout/small/a1.png"
    assert photo_url({"strThumb": ""}) is None
    assert attribution({"idPlayer": "9", "strCreativeCommons": "CC BY-SA 4.0",
                        "strCreativeCommonsAttribution": "Some Author"}) == \
        "photo:TheSportsDB idPlayer=9 license=CC BY-SA 4.0 credit=Some Author"

    sql = render_sql({1: {"url": "http://img/saka.png", "remark": "photo:TheSportsDB idPlayer=9",
                          "name": "Bukayo Saka"}})
    assert "UPDATE football_player SET avatar_url = 'http://img/saka.png'" in sql
    assert sql.count("START TRANSACTION;") == 1 and sql.strip().endswith("COMMIT;")
    print("self-test OK")


def parse_args(argv=None):
    parser = argparse.ArgumentParser(description="给真实球员补头像（TheSportsDB）")
    parser.add_argument("--out", default=str(DEFAULT_OUT), help=f"输出 SQL（默认 {DEFAULT_OUT}）")
    parser.add_argument("--key", help=f"TheSportsDB API key，默认读环境变量 {TSDB_KEY_ENV} 或免费 key 3")
    parser.add_argument("--teams-limit", type=int, default=0, help="只处理前 N 支球队（试跑用）")
    parser.add_argument("--player-pass", dest="player_pass", action="store_true", default=True,
                        help="第二遍：按球员姓名搜索补头像（默认开启）")
    parser.add_argument("--no-player-pass", dest="player_pass", action="store_false",
                        help="只跑球队那一遍（快，约 15 分钟）")
    parser.add_argument("--players-limit", type=int, default=0,
                        help="第二遍最多查多少个球员（0=全部，4050 人约 2.4 小时，可分次跑）")
    parser.add_argument("--delay", type=float, default=MIN_INTERVAL_SECONDS, help="请求间隔秒")
    parser.add_argument("--refresh", action="store_true", help="忽略缓存")
    parser.add_argument("--no-validate", dest="no_validate", action="store_true",
                        help="跳过图片可用性校验（默认会校验并剔除坏图）")
    parser.add_argument("--self-test", action="store_true", help="离线自检，不联网")
    return parser.parse_args(argv)


def main(argv=None):
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(errors="replace")
    args = parse_args(argv)
    if args.self_test:
        run_self_test()
        return 0
    args.key = args.key or os.environ.get(TSDB_KEY_ENV) or "3"
    args.out = Path(args.out)
    return sync(args)


if __name__ == "__main__":
    sys.exit(main())
