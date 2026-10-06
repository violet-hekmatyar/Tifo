#!/usr/bin/env python3
"""sync_football_data.py 的离线回归检查：本地假 API + 真实跑完整同步。

不访问 football-data.org，不需要 API token，不连接数据库。

    py -3 selftest_football_sync.py

覆盖范围：跨赛季取数（同一赛事两个赛季）、只取 TOTAL 积分榜、状态/时间映射、
按范围先删后插、ID 生成、事件抓取、缓存命中、中英对照表自动补空值、
重要等级启发式（淘汰赛 / 联赛前 6 对阵）、球队榜由积分榜派生。
"""

from __future__ import annotations

import importlib.util
import json
import re
import sys
import tempfile
import threading
import urllib.parse
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

HERE = Path(__file__).resolve().parent

TEAM_57 = {"id": 57, "name": "Arsenal FC", "shortName": "Arsenal", "tla": "ARS",
           "crest": "https://crests.football-data.org/57.png", "venue": "Emirates Stadium",
           "founded": 1886, "area": {"id": 2072, "name": "England"},
           "coach": {"id": 115, "name": "Mikel Arteta"}, "lastUpdated": "2026-09-01T00:00:00Z"}
TEAM_65 = {"id": 65, "name": "Manchester City FC", "shortName": "Man City", "tla": "MCI",
           "crest": "https://crests.football-data.org/65.png", "venue": "Etihad Stadium",
           "founded": 1880, "area": {"id": 2072, "name": "England"},
           "coach": {"id": 116, "name": "Pep Guardiola"}, "lastUpdated": "2026-09-01T00:00:00Z"}
TEAM_64 = {"id": 64, "name": "Liverpool FC", "shortName": "Liverpool", "tla": "LIV",
           "crest": "https://crests.football-data.org/64.png", "venue": "Anfield",
           "founded": 1892, "area": {"id": 2072, "name": "England"},
           "coach": {"id": 117, "name": "Arne Slot"}, "lastUpdated": "2026-09-01T00:00:00Z"}
# 榜外球队：不在积分榜前 6，用来验证「联赛前 6 对阵」不会误判
TEAM_66 = {"id": 66, "name": "Aston Villa FC", "shortName": "Aston Villa", "tla": "AVL",
           "crest": "https://crests.football-data.org/66.png", "venue": "Villa Park",
           "founded": 1874, "area": {"id": 2072, "name": "England"},
           "coach": {"id": 118, "name": "Unai Emery"}, "lastUpdated": "2026-09-01T00:00:00Z"}
TEAM_67 = {"id": 67, "name": "Everton FC", "shortName": "Everton", "tla": "EVE",
           "crest": "https://crests.football-data.org/67.png", "venue": "Goodison Park",
           "founded": 1878, "area": {"id": 2072, "name": "England"},
           "coach": {"id": 119, "name": "Demo"}, "lastUpdated": "2026-09-01T00:00:00Z"}
TEAM_68 = {"id": 68, "name": "Norwich City FC", "shortName": "Norwich", "tla": "NOR",
           "crest": "https://crests.football-data.org/68.png", "venue": "Carrow Road",
           "founded": 1902, "area": {"id": 2072, "name": "England"},
           "coach": {"id": 120, "name": "Demo"}, "lastUpdated": "2026-09-01T00:00:00Z"}

SEASON_2025 = {"id": 2001, "startDate": "2025-08-15", "endDate": "2026-05-24", "currentMatchday": 38}
SEASON_2026 = {"id": 2002, "startDate": "2026-08-14", "endDate": "2027-05-23", "currentMatchday": 8}
SEASON_CL = {"id": 3002, "startDate": "2026-09-01", "endDate": "2027-05-30", "currentMatchday": 1}

PLAYER_SAKA = {"id": 38101, "name": "Bukayo Saka", "position": "Offence",
               "dateOfBirth": "2001-09-05", "nationality": "England"}
PLAYER_HAALAND = {"id": 99813, "name": "Erling Haaland", "position": "Offence",
                  "dateOfBirth": "2000-07-21", "nationality": "Norway"}
PLAYER_RICE = {"id": 34022, "name": "Declan Rice", "position": "Midfield",
               "dateOfBirth": "1999-01-14", "nationality": "England"}
PLAYER_RAYA = {"id": 33101, "name": "David Raya", "position": "Goalkeeper",
               "dateOfBirth": "1995-09-15", "nationality": "Spain"}
PLAYER_SALIBA = {"id": 33102, "name": "William Saliba", "position": "Defence",
                 "dateOfBirth": "2001-03-24", "nationality": "France"}

def thin(team):
    """matches / standings 里的球队对象只有这 5 个字段（真实接口如此），
    用来验证同步时不会把它们当成完整球队信息覆盖掉 country/venue/coach。"""
    return {key: team[key] for key in ("id", "name", "shortName", "tla", "crest")}


MATCH_1001 = {
    "id": 1001, "utcDate": "2026-05-02T14:00:00Z", "status": "FINISHED", "matchday": 38,
    "stage": "REGULAR_SEASON", "group": None, "homeTeam": thin(TEAM_57), "awayTeam": thin(TEAM_65),
    "score": {"winner": "HOME_TEAM", "duration": "REGULAR", "fullTime": {"home": 2, "away": 1},
              "halfTime": {"home": 1, "away": 0}},
    "season": SEASON_2025, "lastUpdated": "2026-05-02T17:00:00Z",
}
MATCH_1002 = {
    "id": 1002, "utcDate": "2026-08-15T11:30:00Z", "status": "TIMED", "matchday": 1,
    "stage": "REGULAR_SEASON", "group": None, "homeTeam": thin(TEAM_64), "awayTeam": thin(TEAM_57),
    "score": {"winner": None, "duration": "REGULAR", "fullTime": {"home": None, "away": None},
              "halfTime": {"home": None, "away": None}},
    "season": SEASON_2026, "lastUpdated": "2026-08-01T10:00:00Z",
}
# 一方不在积分榜前 6：重要等级必须为 0
MATCH_1003 = {
    "id": 1003, "utcDate": "2026-08-22T14:00:00Z", "status": "TIMED", "matchday": 2,
    "stage": "REGULAR_SEASON", "group": None, "homeTeam": thin(TEAM_66), "awayTeam": thin(TEAM_57),
    "score": {"winner": None, "duration": "REGULAR", "fullTime": {"home": None, "away": None},
              "halfTime": {"home": None, "away": None}},
    "season": SEASON_2026, "lastUpdated": "2026-08-01T10:00:00Z",
}

# 杯赛夹具不求赛历真实，只覆盖「阶段 → 重要等级」映射
def cup_match(match_id, stage, utc_date):
    return {
        "id": match_id, "utcDate": utc_date, "status": "TIMED", "matchday": None, "stage": stage,
        "group": None, "homeTeam": thin(TEAM_57), "awayTeam": thin(TEAM_65),
        "score": {"winner": None, "duration": "REGULAR", "fullTime": {"home": None, "away": None},
                  "halfTime": {"home": None, "away": None}},
        "season": SEASON_CL, "lastUpdated": "2026-09-01T00:00:00Z",
    }


MATCH_3001 = cup_match(3001, "LEAGUE_STAGE", "2026-09-15T19:00:00Z")
# 真实接口的写法（欧冠 2025/26 用 PLAYOFFS、LAST_16；世界杯用 LAST_32）
MATCH_3002 = cup_match(3002, "PLAYOFFS", "2026-09-22T19:00:00Z")
MATCH_3003 = cup_match(3003, "LAST_16", "2026-09-25T19:00:00Z")
MATCH_3004 = cup_match(3004, "LAST_32", "2026-09-27T19:00:00Z")
MATCH_3005 = cup_match(3005, "FINAL", "2026-09-29T19:00:00Z")

TABLE_2025_TOTAL = [
    {"position": 1, "team": thin(TEAM_57), "playedGames": 38, "form": "W,W,D,W,L", "won": 27, "draw": 7,
     "lost": 4, "points": 88, "goalsFor": 85, "goalsAgainst": 30, "goalDifference": 55},
    {"position": 2, "team": thin(TEAM_65), "playedGames": 38, "form": "W,W,W,L,D", "won": 26, "draw": 6,
     "lost": 6, "points": 84, "goalsFor": 90, "goalsAgainst": 38, "goalDifference": 52},
    {"position": 3, "team": thin(TEAM_64), "playedGames": 38, "form": "D,W,L,W,W", "won": 24, "draw": 8,
     "lost": 6, "points": 80, "goalsFor": 78, "goalsAgainst": 41, "goalDifference": 37},
]
TABLE_2026_TOTAL = [
    {"position": 1, "team": thin(TEAM_64), "playedGames": 8, "form": "W,W,W,W,D", "won": 6, "draw": 2,
     "lost": 0, "points": 20, "goalsFor": 18, "goalsAgainst": 5, "goalDifference": 13},
    {"position": 2, "team": thin(TEAM_57), "playedGames": 8, "form": "W,D,W,L,W", "won": 5, "draw": 1,
     "lost": 2, "points": 16, "goalsFor": 15, "goalsAgainst": 9, "goalDifference": 6},
    {"position": 3, "team": thin(TEAM_65), "playedGames": 8, "form": "D,L,W,W,W", "won": 4, "draw": 2,
     "lost": 2, "points": 14, "goalsFor": 14, "goalsAgainst": 10, "goalDifference": 4},
]

STANDINGS = {
    "2025": {"competition": {"id": 2021}, "season": SEASON_2025, "standings": [
        {"stage": "REGULAR_SEASON", "type": "TOTAL", "group": None, "table": TABLE_2025_TOTAL},
        {"stage": "REGULAR_SEASON", "type": "HOME", "group": None, "table": [
            {"position": 1, "team": thin(TEAM_57), "playedGames": 19, "form": "W,W,W,W,W", "won": 16,
             "draw": 2, "lost": 1, "points": 50, "goalsFor": 48, "goalsAgainst": 12,
             "goalDifference": 36}]},
    ]},
    "2026": {"competition": {"id": 2021}, "season": SEASON_2026, "standings": [
        {"stage": "REGULAR_SEASON", "type": "TOTAL", "group": None, "table": TABLE_2026_TOTAL},
    ]},
}
SCORERS = {
    "2025": {"competition": {"id": 2021}, "season": SEASON_2025, "scorers": [
        {"player": PLAYER_HAALAND, "team": TEAM_65, "playedMatches": 36, "goals": 31,
         "assists": 6, "penalties": 4},
        {"player": PLAYER_SAKA, "team": TEAM_57, "playedMatches": 35, "goals": 19,
         "assists": 13, "penalties": 5},
    ]},
    "2026": {"competition": {"id": 2021}, "season": SEASON_2026, "scorers": [
        {"player": PLAYER_SAKA, "team": TEAM_57, "playedMatches": 7, "goals": 6,
         "assists": 4, "penalties": 1},
        # 接口偶尔省略计数字段：库里 assists 是 NOT NULL，缺值必须写 0 而不是 NULL
        {"player": PLAYER_SALIBA, "team": TEAM_57, "playedMatches": 7, "goals": 2,
         "assists": None, "penalties": 0},
    ]},
}

FIXTURES = {
    "/v4/competitions/PL": {
        "id": 2021, "code": "PL", "name": "Premier League", "type": "LEAGUE",
        "emblem": "https://crests.football-data.org/PL.png", "area": {"id": 2072, "name": "England"},
        "currentSeason": SEASON_2026, "numberOfAvailableSeasons": 34,
    },
    "/v4/competitions/PL/teams": {"competition": {"id": 2021}, "season": SEASON_2026,
                                  "teams": [TEAM_57, TEAM_65, {**TEAM_64, "squad": [PLAYER_RAYA]}, TEAM_66, TEAM_67, TEAM_68]},
    "/v4/competitions/PL/matches": {"competition": {"id": 2021},
                                    "matches": [MATCH_1001, MATCH_1002, MATCH_1003]},
    "/v4/competitions/PL/standings": lambda query: STANDINGS[query.get("season", ["2025"])[0]],
    "/v4/competitions/PL/scorers": lambda query: SCORERS[query.get("season", ["2025"])[0]],
    "/v4/teams/57": {"id": 57, "name": "Arsenal FC",
                     "squad": [PLAYER_RAYA, PLAYER_SALIBA, PLAYER_RICE, PLAYER_SAKA]},
    "/v4/teams/65": {"id": 65, "name": "Manchester City FC", "squad": [PLAYER_HAALAND]},
    "/v4/teams/64": {"id": 64, "name": "Liverpool FC", "squad": []},
    # 国家队/受限球队：免费套餐不给阵容，必须跳过而不是整轮中止；
    # 66、67 相邻且连续受限，68 排在它们后面，用来验证「连续受限后停止浪费请求」
    "/v4/teams/66": "FORBIDDEN",
    "/v4/teams/67": "FORBIDDEN",
    "/v4/teams/68": {"id": 68, "name": "Norwich City FC", "squad": []},
    "/v4/matches/1001": {
        "id": 1001, "status": "FINISHED", "homeTeam": TEAM_57, "awayTeam": TEAM_65,
        "goals": [
            {"minute": 23, "injuryTime": None, "type": "REGULAR", "team": TEAM_57,
             "scorer": PLAYER_SAKA, "assist": PLAYER_RICE, "score": {"home": 1, "away": 0}},
            {"minute": 58, "injuryTime": None, "type": "PENALTY", "team": TEAM_65,
             "scorer": PLAYER_HAALAND, "assist": None, "score": {"home": 1, "away": 1}},
            {"minute": 90, "injuryTime": 4, "type": "REGULAR", "team": TEAM_57,
             "scorer": PLAYER_SALIBA, "assist": PLAYER_SAKA, "score": {"home": 2, "away": 1}},
        ],
        "bookings": [{"minute": 70, "team": TEAM_65, "player": PLAYER_HAALAND, "card": "YELLOW"}],
        "substitutions": [{"minute": 80, "team": TEAM_57, "playerOut": PLAYER_SAKA,
                           "playerIn": PLAYER_RICE}],
    },
    # 杯赛：只给赛事信息、球队与赛程，积分榜/射手榜返回 404（脚本应跳过而不报错）
    "/v4/competitions/CL": {
        "id": 2001, "code": "CL", "name": "UEFA Champions League", "type": "CUP",
        "emblem": "https://crests.football-data.org/CL.png", "area": {"id": 2077, "name": "Europe"},
        "currentSeason": SEASON_CL, "numberOfAvailableSeasons": 70,
    },
    "/v4/competitions/CL/teams": {"competition": {"id": 2001}, "season": SEASON_CL,
                                  "teams": [TEAM_57, TEAM_65]},
    "/v4/competitions/CL/matches": {"competition": {"id": 2001},
                                    "matches": [MATCH_3001, MATCH_3002, MATCH_3003, MATCH_3004,
                                                MATCH_3005]},
}


class FixtureHandler(BaseHTTPRequestHandler):
    paths: list = []

    def do_GET(self):  # noqa: N802
        parsed = urllib.parse.urlparse(self.path)
        FixtureHandler.paths.append(self.path)
        payload = FIXTURES.get(parsed.path)
        if callable(payload):
            payload = payload(urllib.parse.parse_qs(parsed.query))
        if payload == "FORBIDDEN":
            body = json.dumps({"message": "The resource you are looking for is restricted and apparently "
                                          "not within your permissions. Please check your subscription.",
                               "errorCode": 403}).encode("utf-8")
            self.send_response(403)
        elif payload is None:
            body = b'{"message": "not found"}'
            self.send_response(404)
        else:
            body = json.dumps(payload).encode("utf-8")
            self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, *args):
        pass


def load_module():
    sys.dont_write_bytecode = True
    spec = importlib.util.spec_from_file_location("sync_football_data", HERE / "sync_football_data.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def table_rows(sql, table):
    blocks = re.findall(rf"INSERT INTO {table} \([^;]*?;", sql, re.S)
    return [row for block in blocks for row in re.findall(r"^  \((.+)\),?$", block, re.M)]


def cells(row, index):
    return row.lstrip("(").split(", ")[index]


def last_cell(row):
    """match_info 的 extra_json 里含 ", "，最后一列不能用 split(", ") 取。"""
    return row.rsplit(", ", 1)[-1]


def row_of(rows, row_id):
    return next(row for row in rows if cells(row, 0) == row_id)


def main():
    module = load_module()
    server = ThreadingHTTPServer(("127.0.0.1", 0), FixtureHandler)
    threading.Thread(target=server.serve_forever, daemon=True).start()

    with tempfile.TemporaryDirectory() as tmp:
        tmp_path = Path(tmp)
        module.API_BASE = f"http://127.0.0.1:{server.server_port}/v4"
        module.MIN_INTERVAL_SECONDS = 0
        module.RATE_LIMIT_SLEEP_SECONDS = 0  # 夹具下不留退避等待，失败立即暴露
        module.CACHE_DIR = tmp_path / "cache"
        module.REQUEST_LOG_PATH = tmp_path / "request_log.json"
        module.NAME_MAP_PATH = tmp_path / "name_map_zh.json"
        module.NAME_MAP_PATH.write_text(
            json.dumps({"competitions": {"PL": "英超", "CL": "欧冠"}, "teams": {"57": "阿森纳"},
                        "players": {}}, ensure_ascii=False), encoding="utf-8")
        out = tmp_path / "sync.sql"

        argv = ["--date-from", "2026-01-01", "--date-to", "2026-10-01", "--out", str(out),
                "--competitions", "PL,CL", "--api-key", "test-token", "--with-events"]
        module.main(argv)
        first_requests = len(FixtureHandler.paths)
        FixtureHandler.paths = []
        module.main(argv)
        second_paths = FixtureHandler.paths

        # PL: 赛事/赛程/球队/2 积分榜/2 射手榜 + 阵容 57、65、66(403)、67(403) 后停止 = 11
        #     （64 的阵容来自 teams payload，不额外请求）
        # CL: 赛事/赛程/球队 + 404 的积分榜/射手榜 = 5；事件 1 场 = 1
        assert first_requests == 17, f"首次应发 17 次请求，实际 {first_requests}"
        # 第二次运行只允许：两个 404 端点 + 两支受限球队（66、67，403 不缓存）+ 一次 token 探测
        not_found_endpoints = ("/v4/competitions/CL/standings", "/v4/competitions/CL/scorers")
        allowed = not_found_endpoints + ("/v4/teams/66", "/v4/teams/67", "/v4/competitions/PL")
        assert len(second_paths) == 5, f"第二次只应有 5 次请求（2 个 404 + 2 支受限球队 + 探测），实际 {second_paths}"
        assert all(path.split("?")[0] in allowed for path in second_paths), second_paths
        # 68 排在连续两次受限之后，不应被请求
        assert not any(path.startswith("/v4/teams/68") for path in second_paths)
        sql = out.read_text(encoding="utf-8")

        # 结构
        assert sql.startswith("-- 由 scripts/data-sync/sync_football_data.py 生成")
        assert f"USE {module.DB_NAME};" in sql
        assert sql.count("START TRANSACTION;") == 1 and sql.strip().endswith("COMMIT;")

        # 用户数据安全：不出现用户/内容域表，也不覆盖 follower_count（跳过注释头）
        body = sql.split("START TRANSACTION;", 1)[1]
        for forbidden in ("sys_user", "user_profile", "content", "follow_record", "like_record",
                          "favorite_record", "comment", "follower_count"):
            assert f"INTO ({forbidden}" not in body
            assert f"INSERT INTO {forbidden} " not in body
            assert f"UPDATE {forbidden}" not in body
        assert "follower_count" not in body

        # 联赛：联赛型与杯赛型并存
        leagues = table_rows(sql, "football_league")
        assert len(leagues) == 2, leagues
        assert cells(row_of(leagues, "2021"), 1) == "'英超'" and cells(row_of(leagues, "2021"), 6) == "'LEAGUE'"
        assert cells(row_of(leagues, "2001"), 1) == "'欧冠'" and cells(row_of(leagues, "2001"), 6) == "'CUP'"

        # 赛季：年份取自赛季对象本身（2025/26 与 2026/27 不能串号）
        seasons = table_rows(sql, "football_season")
        assert len(seasons) == 3, seasons
        assert cells(row_of(seasons, "2001"), 2) == "'2025'"
        assert cells(row_of(seasons, "2001"), 3) == "'2025/26'"
        assert cells(row_of(seasons, "2001"), 6) == "0"
        assert cells(row_of(seasons, "2002"), 2) == "'2026'"
        assert cells(row_of(seasons, "2002"), 6) == "1"
        assert cells(row_of(seasons, "3002"), 3) == "'2026/27'"
        assert all(cells(row, 7) == "'FOOTBALL_DATA'" for row in seasons)

        # 阶段：PL 每赛季一个常规赛阶段；CL 按阶段类型各一个，与赛季 id 绑定
        stages = table_rows(sql, "football_competition_stage")
        stage_2025 = str(module.stage_id_of(2021, 2025, "REGULAR_SEASON"))
        stage_2026 = str(module.stage_id_of(2021, 2026, "REGULAR_SEASON"))
        assert len(stages) == 7, stages  # PL 两赛季各一 + CL 五个阶段
        assert {cells(row_of(stages, stage_2025), 2), cells(row_of(stages, stage_2026), 2)} == {"2001", "2002"}
        assert cells(row_of(stages, stage_2026), 3) == "'REGULAR_SEASON'"
        assert cells(row_of(stages, stage_2026), 4) == "'常规赛'"
        cup_stages = {cells(row, 3): row for row in stages if cells(row, 1) == "2001"}
        assert set(cup_stages) == {"'LEAGUE_STAGE'", "'PLAYOFF'", "'ROUND_OF_16'", "'ROUND_OF_32'",
                                   "'FINAL'"}, cup_stages
        assert cells(cup_stages["'FINAL'"], 4) == "'决赛'" and cells(cup_stages["'FINAL'"], 2) == "3002"
        # 真实接口的 LAST_16 / LAST_32 / PLAYOFFS 必须落成中文阶段名
        assert cells(cup_stages["'ROUND_OF_16'"], 4) == "'16强'"
        assert cells(cup_stages["'ROUND_OF_32'"], 4) == "'32强'"
        assert cells(cup_stages["'PLAYOFF'"], 4) == "'附加赛'"

        # 球队 / 球员：中文名走对照表，缺项英文兜底；
        # matches/standings 的简化球队对象不能把 country/venue/founded 覆盖成 NULL
        teams = table_rows(sql, "football_team")
        assert len(teams) == 6, teams
        arsenal = row_of(teams, "57")
        assert arsenal.startswith("57, '阿森纳', 'Arsenal FC', 'Arsenal'")
        assert "'Emirates Stadium'" in arsenal and ", 1886, " in arsenal and "'England'" in arsenal, arsenal
        assert row_of(teams, "65").startswith("65, 'Manchester City FC', 'Manchester City FC', 'Man City'")
        assert "'Mikel Arteta'" in arsenal
        assert len(table_rows(sql, "football_player")) == 5

        # 阵容：team_player 按 (season, team) 删除；64 的阵容来自 /competitions/PL/teams 的 payload
        roster = table_rows(sql, "team_player")
        assert len(roster) == 6, roster
        assert all(cells(row, 4) == "'2026'" for row in roster)
        assert any(cells(row, 1) == "64" and cells(row, 2) == "33101" for row in roster)
        # 66/67 阵容接口 403、68 因连续受限未请求：三者都不写阵容，
        # 也绝不能出现在删除范围里（否则会把库里已有的阵容删空）
        assert all(cells(row, 1) not in ("66", "67", "68") for row in roster)
        assert "DELETE FROM team_player WHERE season = '2026' AND team_id IN (57, 64, 65);" in sql
        assert "-- 本次跳过" in sql and "403 /teams/66" in sql and "403 /teams/67" in sql

        # App 的球队/球员详情读 football_team_season_player，必须有行，且按 (赛事,赛季,球队) 先删后插
        season_players = table_rows(sql, "football_team_season_player")
        assert season_players, "缺少 football_team_season_player 行"
        assert "DELETE FROM football_team_season_player WHERE league_id = 2021 AND season_id = 2002 " \
               "AND team_id IN (57, 64, 65);" in sql
        assert "DELETE FROM football_team_season_player WHERE league_id = 2001 AND season_id = 3002 " \
               "AND team_id IN (57, 65);" in sql
        for row in season_players:
            assert cells(row, 1) in ("2021", "2001")          # league_id
            assert cells(row, 5) in ("'GK'", "'DF'", "'MF'", "'FW'")  # position NOT NULL
            assert cells(row, 7) == "'FOOTBALL_DATA'"         # source
        # 英超 2026/27 的 57 号球队阵容：4 人；欧冠 2026/27 复用同一份阵容
        pl_57 = [row for row in season_players
                 if cells(row, 1) == "2021" and cells(row, 3) == "57" and cells(row, 2) == "2002"]
        cl_57 = [row for row in season_players
                 if cells(row, 1) == "2001" and cells(row, 3) == "57" and cells(row, 2) == "3002"]
        assert len(pl_57) == 4 and len(cl_57) == 4, (pl_57, cl_57)

        # 比赛：状态映射 + UTC 转北京时间 + 轮次名 + 重要等级
        matches = table_rows(sql, "match_info")
        assert len(matches) == 8, matches
        finished = row_of(matches, "1001")
        assert cells(finished, 2) == "'2025'" and cells(finished, 3) == "'第38轮'"
        assert cells(finished, 6) == "2" and cells(finished, 7) == "1"
        assert cells(finished, 8) == "'2026-05-02 22:00:00'" and cells(finished, 10) == "'FINISHED'"
        scheduled = row_of(matches, "1002")
        assert cells(scheduled, 2) == "'2026'" and cells(scheduled, 6) == "NULL"
        assert cells(scheduled, 8) == "'2026-08-15 19:30:00'" and cells(scheduled, 10) == "'SCHEDULED'"
        assert cells(row_of(matches, "3005"), 3) == "'决赛'"
        assert cells(row_of(matches, "3003"), 3) == "'16强'"
        assert cells(row_of(matches, "3004"), 3) == "'32强'"
        assert cells(row_of(matches, "3002"), 3) == "'附加赛'"
        # 重要等级：联赛前 6 对阵 = 1；一方榜外 = 0；淘汰赛 = 2；决赛 = 3
        assert [last_cell(row_of(matches, mid)) for mid in ("1001", "1002", "1003")] == ["1", "1", "0"]
        assert [last_cell(row_of(matches, mid)) for mid in ("3001", "3002", "3003", "3004", "3005")] == \
               ["0", "2", "2", "2", "3"]

        # 榜单：只取 TOTAL（HOME 表被过滤），先按 (league, season) 删
        for table in ("football_competition_stage", "football_standing",
                      "football_player_competition_stat"):
            assert f"DELETE FROM {table} WHERE league_id = 2021 AND season_id IN (2001, 2002);" in sql
        assert "DELETE FROM football_competition_stage WHERE league_id = 2001 AND season_id IN (3002);" in sql
        standings = table_rows(sql, "football_standing")
        assert len(standings) == 6, len(standings)
        for row in standings:
            assert cells(row, 1) == "2021"
            assert cells(row, 3) == (stage_2025 if cells(row, 2) == "2001" else stage_2026)
            assert cells(row, 4) == "''" and cells(row, 16) == "'FOOTBALL_DATA'"
        assert cells(row_of(standings, str(module.standing_id_of(2021, 2025, "REGULAR_SEASON", "", 1))), 6) == "1"
        assert cells(row_of(standings, str(module.standing_id_of(2021, 2025, "REGULAR_SEASON", "", 1))), 15) == "'W,W,D,W,L'"

        # 球队榜：由积分榜派生，UPSERT（不删），只更新 API 提供的三列
        team_stats = table_rows(sql, "football_team_competition_stat")
        assert len(team_stats) == 6, team_stats
        assert "DELETE FROM football_team_competition_stat" not in sql
        assert "played=VALUES(played), goals_for=VALUES(goals_for), goals_against=VALUES(goals_against)" in sql
        assert "corners" not in sql and "clean_sheets" not in sql
        derived = row_of(team_stats, str(module.team_stat_id_of(2021, 2025, "REGULAR_SEASON", "", 1)))
        assert cells(derived, 1) == "2021" and cells(derived, 3) == stage_2025 and cells(derived, 4) == "57"
        assert cells(derived, 5) == "38" and cells(derived, 6) == "85" and cells(derived, 7) == "30"

        # 射手榜：挂在该赛季首个阶段上；缺 assists 的接口返回必须写成 0 而不是 NULL
        stats = table_rows(sql, "football_player_competition_stat")
        assert len(stats) == 4, stats
        assert {(cells(row, 2), cells(row, 3)) for row in stats} == {
            ("2001", stage_2025), ("2002", stage_2026)}
        assert any(cells(row, 4) == "99813" and cells(row, 7) == "31" for row in stats)
        null_assists = row_of(stats, str(module.player_stat_id_of(2021, 2026, "REGULAR_SEASON", 2)))
        assert cells(null_assists, 4) == "33102" and cells(null_assists, 8) == "0", null_assists

        # 事件：按场删除后重插，分钟/类型/比分/换人说明齐全
        assert "DELETE FROM match_event WHERE match_id IN (1001);" in sql
        events = table_rows(sql, "match_event")
        assert len(events) == 5, events
        assert cells(events[0], 5) == "'GOAL'" and cells(events[0], 6) == "23"
        assert any("'PENALTY_GOAL'" in row and "'1-1'" in row for row in events)
        assert any("'YELLOW_CARD'" in row and cells(row, 6) == "70" for row in events)
        assert any("'SUBSTITUTION'" in row and "Bukayo Saka → Declan Rice" in row for row in events)
        assert any(cells(row, 6) == "90" and cells(row, 7) == "4" for row in events)

        # 对照表自动补空值，人工后续补译
        name_map = json.loads(module.NAME_MAP_PATH.read_text(encoding="utf-8"))
        assert name_map["teams"]["57"] == "阿森纳" and name_map["teams"]["65"] is None
        assert name_map["players"]["99813"] is None

        # token 不可用（所有请求都 403，连探测也不通）必须中止，不能悄悄产出一份空 SQL。
        # 夹具服务器此刻仍在运行，探测会真实发请求。
        original = FIXTURES["/v4/competitions/PL"]
        FIXTURES["/v4/competitions/PL"] = "FORBIDDEN"
        FixtureHandler.paths = []
        try:
            module.main(argv + ["--refresh"])
            raise AssertionError("token 不可用时应中止，而不是继续跑完")
        except SystemExit as error:
            assert "token" in str(error), error
            assert len(FixtureHandler.paths) == 2, FixtureHandler.paths  # 原请求 + 探测
        finally:
            FIXTURES["/v4/competitions/PL"] = original

    server.shutdown()
    print(f"fixture sync OK（首次 {first_requests} 次请求，第二次只有 {len(second_paths)} 次不可缓存请求）")
    return 0


if __name__ == "__main__":
    sys.exit(main())
