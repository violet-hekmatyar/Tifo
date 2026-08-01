#!/usr/bin/env python3
"""Generate the deterministic, offline T14 demo dataset and SVG assets."""

from __future__ import annotations

import json
import argparse
import random
from collections import Counter, defaultdict
from datetime import datetime, timedelta
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
DATA_DIR = ROOT / "scripts" / "data"
SQL_PATH = ROOT / "scripts" / "sql" / "seed-demo.sql"
STATIC_DIR = ROOT / "src" / "main" / "resources" / "static" / "demo"


def load_json(name: str):
    with (DATA_DIR / name).open(encoding="utf-8") as handle:
        return json.load(handle)


def sql_value(value):
    if value is None:
        return "NULL"
    if isinstance(value, bool):
        return "1" if value else "0"
    if isinstance(value, (int, float)):
        return str(value)
    return "'" + str(value).replace("\\", "\\\\").replace("'", "''") + "'"


def insert(lines, table, columns, rows, batch_size=250):
    for start in range(0, len(rows), batch_size):
        batch = rows[start:start + batch_size]
        lines.append(f"INSERT INTO {table} ({', '.join(columns)}) VALUES")
        lines.append(",\n".join("  (" + ", ".join(sql_value(v) for v in row) + ")" for row in batch) + ";")
        lines.append("")


def svg(label, background, foreground="#ffffff", subtitle="南看台 Demo"):
    safe = (label.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"))
    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="800" height="450" viewBox="0 0 800 450">
<rect width="800" height="450" fill="{background}"/><path d="M0 340 Q200 250 400 340 T800 340 V450 H0Z" fill="#16803b"/>
<circle cx="400" cy="330" r="78" fill="none" stroke="#ffffff" stroke-width="5" opacity=".65"/>
<text x="400" y="170" text-anchor="middle" font-family="Arial,Microsoft YaHei,sans-serif" font-size="54" font-weight="700" fill="{foreground}">{safe}</text>
<text x="400" y="220" text-anchor="middle" font-family="Arial,Microsoft YaHei,sans-serif" font-size="24" fill="{foreground}" opacity=".88">{subtitle}</text></svg>'''


def write_assets(names):
    palettes = ["#c62828", "#1455a0", "#232323", "#00695c", "#6a1b9a", "#ad6b00", "#37474f", "#8e2430"]
    folders = ["teams", "players", "users", "contents", "matches", "common"]
    for folder in folders:
        (STATIC_DIR / folder).mkdir(parents=True, exist_ok=True)
    for i in range(12):
        (STATIC_DIR / "teams" / f"team-{i + 1:02}.svg").write_text(svg(f"球队 {i + 1:02}", palettes[i % len(palettes)]), encoding="utf-8")
    for i in range(12):
        (STATIC_DIR / "players" / f"player-{i + 1:02}.svg").write_text(svg(f"球员 {i + 1:02}", palettes[(i + 2) % len(palettes)]), encoding="utf-8")
    for i in range(12):
        label = names["nicknames"][i][:6]
        (STATIC_DIR / "users" / f"user-{i + 1:02}.svg").write_text(svg(label, palettes[(i + 4) % len(palettes)]), encoding="utf-8")
    for i in range(20):
        (STATIC_DIR / "contents" / f"cover-{i + 1:02}.svg").write_text(svg(f"绿茵现场 {i + 1:02}", palettes[(i + 1) % len(palettes)]), encoding="utf-8")
    for i in range(10):
        (STATIC_DIR / "matches" / f"match-{i + 1:02}.svg").write_text(svg(f"比赛日 {i + 1:02}", palettes[(i + 3) % len(palettes)]), encoding="utf-8")
    (STATIC_DIR / "common" / "placeholder.svg").write_text(svg("南看台", "#263238"), encoding="utf-8")


def main():
    config = load_json("demo-config.json")
    names = load_json("demo-names.json")
    rng = random.Random(config["seed"])
    counts = config["counts"]
    bases = config["id_bases"]
    baseline = datetime.strptime(config["baseline_time"], "%Y-%m-%d %H:%M:%S")
    write_assets(names)

    user_ids = [bases["user"] + i for i in range(counts["users"])]
    league_ids = [bases["league"] + i for i in range(counts["leagues"])]
    team_ids = [bases["team"] + i for i in range(counts["teams"])]
    player_ids = [bases["player"] + i for i in range(counts["teams"] * counts["players_per_team"])]
    match_ids = [bases["match"] + i for i in range(counts["matches"])]
    content_ids = [bases["content"] + i for i in range(counts["contents"])]

    users, profiles, onboardings = [], [], []
    for i, user_id in enumerate(user_ids):
        disabled = i == len(user_ids) - 1
        main_team = team_ids[i % len(team_ids)]
        users.append((user_id, f"demo_user_{i + 1:02}", config["password_hash"], "USER", 1, "DISABLED" if disabled else "ACTIVE", config["baseline_time"]))
        profiles.append((user_id + 1000, user_id, names["nicknames"][i], f"/demo/users/user-{i % 12 + 1:02}.svg", "记录比赛，也记录看台上的声音。", main_team, "ACTIVE"))
        onboardings.append((user_id + 2000, user_id, main_team, json.dumps([main_team], ensure_ascii=False), json.dumps([], ensure_ascii=False), 1, config["baseline_time"]))

    leagues = []
    for i, (cn, country, league_type) in enumerate(names["leagues"]):
        leagues.append((league_ids[i], cn, f"Demo League {i + 1}", country, f"/demo/teams/team-{i % 12 + 1:02}.svg", "2026", league_type, i + 1))

    teams = []
    for i, (cn, city, stadium) in enumerate(names["teams"]):
        country = names["leagues"][i // 3][1]
        teams.append((team_ids[i], cn, f"Demo Team {i + 1}", cn[:4], f"/demo/teams/team-{i % 12 + 1:02}.svg", country, city, stadium, 1880 + i * 3, f"教练{names['surnames'][i % 12]}指导", "演示数据", 0))

    players, team_players = [], []
    positions = ["GK", "DF", "DF", "MF", "MF", "FW"]
    for team_index, team_id in enumerate(team_ids):
        for slot in range(counts["players_per_team"]):
            index = team_index * counts["players_per_team"] + slot
            player_id = player_ids[index]
            player_name = names["surnames"][(team_index + slot) % 12] + names["given_names"][(team_index * 2 + slot) % 12]
            number = [1, 2, 5, 8, 10, 9][slot]
            position = positions[slot]
            players.append((player_id, player_name, f"Demo Player {index + 1}", f"/demo/players/player-{index % 12 + 1:02}.svg", "演示国籍", number, position, f"{1988 + index % 16}-{index % 12 + 1:02}-{index % 27 + 1:02}", 174 + index % 18, 68 + index % 16, "演示数据", 0))
            team_players.append((player_id + 500000, team_id, player_id, "CLUB", "2026", number, position, "ACTIVE"))

    matches, events = [], []
    match_goals = {}
    event_seq = 0
    for i, match_id in enumerate(match_ids):
        league_index = i % len(league_ids)
        league_teams = team_ids[league_index * 3:league_index * 3 + 3]
        home = league_teams[i % 3]
        away = league_teams[(i + 1 + i // 3) % 3]
        if home == away:
            away = league_teams[(league_teams.index(home) + 1) % 3]
        if i < 10:
            match_status, home_score, away_score = "SCHEDULED", None, None
            match_time = baseline + timedelta(days=i + 1)
        elif i < 16:
            match_status = "LIVE"
            home_score, away_score = (i % 3) + 1, (i + 1) % 2
            match_time = baseline + timedelta(minutes=(i - 13) * 10)
        else:
            match_status = "FINISHED"
            home_score, away_score = (i * 2) % 4, (i + 1) % 3
            match_time = baseline - timedelta(days=(i - 15), hours=i % 5)
        has_report = 1 if match_status == "FINISHED" and i < 40 else 0
        matches.append((match_id, league_ids[league_index], "2026", f"第{i + 1}轮", home, away, home_score, away_score, match_time.strftime("%Y-%m-%d %H:%M:%S"), teams[team_ids.index(home)][7], match_status, i % 4, has_report))
        goal_count = 0
        if match_status != "SCHEDULED":
            running_home = running_away = 0
            goal_sides = [home] * home_score + [away] * away_score
            rng.shuffle(goal_sides)
            for goal_index, side in enumerate(goal_sides):
                team_index = team_ids.index(side)
                scorer = player_ids[team_index * 6 + 5 - goal_index % 2]
                assist = player_ids[team_index * 6 + 3 + goal_index % 2]
                if side == home:
                    running_home += 1
                else:
                    running_away += 1
                minute = min(88, 8 + goal_index * 14 + i % 7)
                events.append((bases["match_event"] + event_seq, match_id, side, scorer, assist, "GOAL", minute, None, f"{running_home}:{running_away}", f"第{minute}分钟进球", 0))
                event_seq += 1
                goal_count += 1
            for event_type, minute, slot in [("YELLOW_CARD", 37 + i % 8, 2), ("SUBSTITUTION", 62 + i % 9, 3)]:
                side = home if (i + slot) % 2 == 0 else away
                team_index = team_ids.index(side)
                player = player_ids[team_index * 6 + slot]
                events.append((bases["match_event"] + event_seq, match_id, side, player, None, event_type, minute, None, None, "演示比赛事件", 0))
                event_seq += 1
        match_goals[match_id] = goal_count

    seasons, stages, standings, player_stats, team_stats = [], [], [], [], []
    current_season_by_league = {}
    current_stage_by_league = {}
    season_seq = stage_seq = standing_seq = player_stat_seq = team_stat_seq = 0
    for league_index, league_id in enumerate(league_ids):
        for previous, code, name, start, end in [
            (True, "2024-2025", "2024/25赛季", "2024-08-01", "2025-06-30"),
            (False, "2025-2026", "2025/26赛季", "2025-08-01", "2026-06-30")
        ]:
            season_id = bases["season"] + season_seq
            stage_id = bases["stage"] + stage_seq
            current = 0 if previous else 1
            seasons.append((season_id, league_id, code, name, start, end, current, "ACTIVE", "DEMO", f"demo-{league_index + 1}-{code}", config["baseline_time"], config["baseline_time"], 0))
            stage_type = "GROUP" if names["leagues"][league_index][2] == "CUP" else "LEAGUE"
            stage_name = "小组赛" if stage_type == "GROUP" else "联赛阶段"
            stages.append((stage_id, league_id, season_id, stage_type, stage_name, None, 1, "ACTIVE", 0))
            if current:
                current_season_by_league[league_id] = season_id
                current_stage_by_league[league_id] = stage_id
            season_seq += 1
            stage_seq += 1

    for league_index in range(counts["rank_leagues"]):
        league_id = league_ids[league_index]
        season_id = current_season_by_league[league_id]
        stage_id = current_stage_by_league[league_id]
        scope_teams = [team_ids[(league_index * 4 + offset) % len(team_ids)] for offset in range(counts["standing_teams_per_league"])]
        standing_rows = []
        for offset, team_id in enumerate(scope_teams):
            played = 20
            won = 14 - offset
            drawn = offset % 3
            lost = played - won - drawn
            goals_for = 44 - offset * 3
            goals_against = 15 + offset * 2
            deduction = 1 if offset == 6 else 0
            points = won * 3 + drawn - deduction
            standing_rows.append((team_id, played, won, drawn, lost, goals_for, goals_against,
                                  goals_for - goals_against, points, deduction, "胜胜平负胜"))
        standing_rows.sort(key=lambda row: (-row[8], -row[7], -row[5], row[0]))
        for rank, row in enumerate(standing_rows, 1):
            team_id, played, won, drawn, lost, goals_for, goals_against, goal_difference, points, deduction, form = row
            standings.append((bases["standing"] + standing_seq, league_id, season_id, stage_id, "", team_id, rank,
                              played, won, drawn, lost, goals_for, goals_against, goal_difference, points, deduction,
                              form, "DEMO", config["baseline_time"], 0))
            assists = max(0, goals_for - 3)
            shots = goals_for * 5 + rank * 3
            shots_on_target = goals_for * 2 + rank
            team_stats.append((bases["team_stat"] + team_stat_seq, league_id, season_id, stage_id, team_id,
                               played, goals_for, goals_against, assists, 22 + rank, rank % 3, shots,
                               shots_on_target, 70 + rank * 3, 180 + rank * 4, max(0, 8 - rank),
                               round(7.45 - rank * 0.06, 2), "DEMO", config["baseline_time"], 0))
            standing_seq += 1
            team_stat_seq += 1
            team_index = team_ids.index(team_id)
            for slot in range(counts["players_per_team"]):
                player_id = player_ids[team_index * counts["players_per_team"] + slot]
                appearances = 14 + (rank + slot) % 7
                starts = max(0, appearances - slot % 4)
                minutes = starts * 82 + (appearances - starts) * 24
                goals = max(0, (7 - slot) * 2 + (8 - rank)) if slot >= 3 else slot % 2
                assists_count = max(0, 8 - rank + (slot % 4)) if slot >= 2 else slot
                shots_on_target = goals + 4 + slot
                shots = shots_on_target + 8 + rank
                saves = 36 + rank * 3 if slot == 0 else 0
                rating = round(6.15 + goals * 0.05 + assists_count * 0.03 + appearances * 0.01, 2)
                player_stats.append((bases["player_stat"] + player_stat_seq, league_id, season_id, stage_id,
                                     player_id, team_id, appearances, starts, minutes, goals, assists_count,
                                     (rank + slot) % 6, 1 if (rank + slot) % 17 == 0 else 0, shots,
                                     shots_on_target, saves, min(rating, 9.50), "DEMO", config["baseline_time"], 0))
                player_stat_seq += 1

    file_rows = []
    for i in range(60):
        file_id = bases["file"] + i
        owner = user_ids[i % 35]
        n = i % 20 + 1
        file_rows.append((file_id, owner, "CONTENT_IMAGE", f"演示封面{n:02}.svg", f"demo-cover-{i + 1:02}.svg", f"demo/content-file-{i + 1:02}.svg", f"demo/contents/cover-{n:02}.svg", f"/demo/contents/cover-{n:02}.svg", "image/svg+xml", "svg", 512, "LOCAL", "ACTIVE"))

    contents, media, blocks, relations, reports = [], [], [], [], []
    content_types = ["POST"] * 30 + ["ARTICLE"] * 30 + ["NEWS"] * 30 + ["REPORT"] * 30
    block_seq = media_seq = relation_seq = report_seq = 0
    title_templates = [
        "{team}赛前训练完成，年轻球员进入大名单", "{team}主场迎来关键比赛，双方公布首发安排",
        "本轮进攻效率明显提升，{player}成为焦点", "比赛最后十分钟的节奏变化值得复盘",
        "你认为本场最佳球员是谁？", "看台观察：{team}的边路配合渐入佳境"
    ]
    for i, content_id in enumerate(content_ids):
        ctype = content_types[i]
        author = user_ids[i % 35]
        team = teams[i % len(teams)][1]
        player = players[(i * 3) % len(players)][1]
        match_id = match_ids[i % len(match_ids)]
        title = title_templates[i % len(title_templates)].format(team=team, player=player)
        summary = f"围绕{team}近期表现整理的演示内容，包含比赛背景、关键节点与看台观点。"
        body = f"{team}在本轮展现出清晰的比赛计划。中场通过连续传递控制节奏，边路球员积极拉开宽度，给禁区内创造了更多空间。{player}在攻防转换中的选择尤其值得关注。本文为离线生成的中文演示数据，只用于开发环境验证分页、详情和关联关系。"
        deleted = i >= 116
        status = "DELETED" if deleted else "PUBLISHED"
        cover = f"/demo/contents/cover-{i % 20 + 1:02}.svg"
        content_format = "ARTICLE_BLOCKS" if ctype == "ARTICLE" else "POST_FORMAT"
        publish = baseline - timedelta(hours=i * 3)
        contents.append((content_id, ctype, content_format, "CONTENT_CARD", title, summary, body, cover, author, "USER", 0, 80 + i * 7, 0, 0, 0, 300 - i, publish.strftime("%Y-%m-%d %H:%M:%S"), status, 1 if deleted else 0))
        media.append((bases["content_block"] + 5000 + media_seq, content_id, "IMAGE", cover, cover, 800, 450, 1, "ACTIVE", 0))
        media_seq += 1
        if ctype == "ARTICLE" and i not in (30, 31):
            file_index = i % 60
            file_id = bases["file"] + file_index
            # Ensure block media ownership matches the article author.
            file_rows[file_index] = (file_id, author, *file_rows[file_index][2:])
            block_data = [("TEXT", body[:110], None, None), ("IMAGE", None, file_id, cover), ("TEXT", body[110:] + " 赛后讨论仍在继续。", None, None)]
            for order, (block_type, text_content, media_file_id, media_url) in enumerate(block_data, 1):
                blocks.append((bases["content_block"] + block_seq, content_id, block_type, text_content, media_file_id, media_url, order, "ACTIVE", 0))
                block_seq += 1
        relation_items = [("TEAM", team_ids[i % 24]), ("PLAYER", player_ids[(i * 3) % 144]), ("MATCH", match_id)]
        for relation_type, relation_id in relation_items:
            relations.append((bases["content_block"] + 10000 + relation_seq, content_id, relation_type, relation_id, 1.0, "MANUAL", "ACTIVE", 0))
            relation_seq += 1
        if ctype == "REPORT":
            finished_match = match_ids[16 + report_seq]
            reports.append((bases["match_event"] + 10000 + report_seq, finished_match, content_id, "REPORT", "ACTIVE", 0))
            report_seq += 1

    comments = []
    comment_seq = 0
    roots_by_content = defaultdict(list)
    comment_texts = ["这场比赛的中场控制很有层次。", "边路推进是今天最明显的变化。", "年轻球员获得机会值得肯定。", "最后十分钟的调整改变了走势。", "这次判罚可以从另一个角度讨论。", "期待下一轮继续保持这样的节奏。"]
    for i in range(counts["root_comments"]):
        content_id = content_ids[i % 5] if i < 100 else content_ids[5 + (i - 100) % 111]
        comment_id = bases["comment"] + comment_seq
        deleted = i >= counts["root_comments"] - 8
        comments.append([comment_id, "CONTENT", content_id, 0, None, None, user_ids[(i * 7) % 35], comment_texts[i % len(comment_texts)], 0, 0, 0, 1 if i < 8 else 0, "DELETED" if deleted else "ACTIVE", 1 if deleted else 0, (baseline - timedelta(minutes=i)).strftime("%Y-%m-%d %H:%M:%S")])
        if not deleted:
            roots_by_content[content_id].append(comment_id)
        comment_seq += 1
    active_roots = [row for row in comments if row[12] == "ACTIVE"]
    for i in range(counts["replies"]):
        if i < 120:
            content_id = content_ids[i % 5]
            root_id = roots_by_content[content_id][i % len(roots_by_content[content_id])]
        else:
            root = active_roots[(i * 5) % len(active_roots)]
            content_id, root_id = root[2], root[0]
        root_row = next(row for row in comments if row[0] == root_id)
        comment_id = bases["comment"] + comment_seq
        user_id = user_ids[(i * 11 + 3) % 35]
        comments.append([comment_id, "CONTENT", content_id, root_id, root_id, root_row[6], user_id, "同意这个观察，我也注意到了相同的比赛细节。", 0, 0, 0, 0, "ACTIVE", 0, (baseline - timedelta(minutes=400 + i)).strftime("%Y-%m-%d %H:%M:%S")])
        root_row[9] += 1
        comment_seq += 1

    likes, favorites, follows = [], [], []
    interaction_seq = 0
    for u, user_id in enumerate(user_ids[:35]):
        for j in range(25):
            likes.append((bases["interaction"] + interaction_seq, user_id, "CONTENT", content_ids[(u * 7 + j) % 116], "ACTIVE")); interaction_seq += 1
        for j in range(14):
            target = active_roots[(u * 13 + j) % len(active_roots)][0]
            likes.append((bases["interaction"] + interaction_seq, user_id, "COMMENT", target, "ACTIVE")); interaction_seq += 1
        for j in range(20):
            favorites.append((bases["interaction"] + 10000 + interaction_seq, user_id, "CONTENT", content_ids[(u * 5 + j) % 116], "ACTIVE")); interaction_seq += 1
        for j in range(6):
            target = team_ids[(u + j * 3) % 24]
            follows.append((bases["interaction"] + 20000 + interaction_seq, user_id, "TEAM", target, 1 if j == 0 else 0, "ACTIVE", 0)); interaction_seq += 1
        for j in range(4):
            follows.append((bases["interaction"] + 20000 + interaction_seq, user_id, "PLAYER", player_ids[(u * 4 + j * 9) % 144], 0, "ACTIVE", 0)); interaction_seq += 1
        for j in range(4):
            target = user_ids[(u + j + 1) % 35]
            follows.append((bases["interaction"] + 20000 + interaction_seq, user_id, "USER", target, 0, "ACTIVE", 0)); interaction_seq += 1
    for i in range(20):
        user_id = user_ids[i]
        active_like_targets = {row[3] for row in likes if row[1] == user_id and row[2] == "CONTENT"}
        cancelled_like_target = next(target for target in reversed(content_ids[:116]) if target not in active_like_targets)
        likes.append((bases["interaction"] + interaction_seq, user_id, "CONTENT", cancelled_like_target, "CANCELLED")); interaction_seq += 1
        active_player_targets = {row[3] for row in follows if row[1] == user_id and row[2] == "PLAYER"}
        cancelled_player_target = next(target for target in reversed(player_ids) if target not in active_player_targets)
        follows.append((bases["interaction"] + 20000 + interaction_seq, user_id, "PLAYER", cancelled_player_target, 0, "CANCELLED", 0)); interaction_seq += 1

    content_like_count = Counter(row[2] for row in likes if row[1] in user_ids and row[4] == "ACTIVE" and row[2] == "CONTENT")
    # Counter above used the target type by mistake if left generic; build explicit target counters.
    content_like_count = Counter(row[3] for row in likes if row[2] == "CONTENT" and row[4] == "ACTIVE")
    comment_like_count = Counter(row[3] for row in likes if row[2] == "COMMENT" and row[4] == "ACTIVE")
    favorite_count = Counter(row[3] for row in favorites if row[4] == "ACTIVE")
    content_comment_count = Counter(row[2] for row in comments if row[12] == "ACTIVE" and row[13] == 0)
    for row in contents:
        row_index = contents.index(row)
        mutable = list(row)
        mutable[12] = content_like_count[row[0]]
        mutable[13] = content_comment_count[row[0]]
        mutable[14] = favorite_count[row[0]]
        contents[row_index] = tuple(mutable)
    for row in comments:
        row[8] = comment_like_count[row[0]]
        row[10] = row[8] * 2 + row[9] * 3 + (50 if row[11] else 0)

    team_follow_count = Counter(row[3] for row in follows if row[2] == "TEAM" and row[5] == "ACTIVE")
    player_follow_count = Counter(row[3] for row in follows if row[2] == "PLAYER" and row[5] == "ACTIVE")
    for i, row in enumerate(teams):
        mutable = list(row); mutable[-1] = team_follow_count[row[0]]; teams[i] = tuple(mutable)
    for i, row in enumerate(players):
        mutable = list(row); mutable[-1] = player_follow_count[row[0]]; players[i] = tuple(mutable)

    following_count = Counter(row[1] for row in follows if row[2] == "USER" and row[5] == "ACTIVE")
    follower_count = Counter(row[3] for row in follows if row[2] == "USER" and row[5] == "ACTIVE")
    team_count = Counter(row[1] for row in follows if row[2] == "TEAM" and row[5] == "ACTIVE")
    player_count = Counter(row[1] for row in follows if row[2] == "PLAYER" and row[5] == "ACTIVE")
    post_count = Counter(row[8] for row in contents if row[17] == "PUBLISHED" and row[18] == 0)
    profiles = [row + (post_count[row[1]], follower_count[row[1]], following_count[row[1]], team_count[row[1]], player_count[row[1]]) for row in profiles]

    lines = ["USE south_stand;", "", "SET NAMES utf8mb4;", "SET FOREIGN_KEY_CHECKS = 0;", "", "-- Deterministic T14/T15 demo dataset. Development use only."]
    cleanup = [
        ("football_team_competition_stat", "id", bases["team_stat"]), ("football_player_competition_stat", "id", bases["player_stat"]),
        ("football_standing", "id", bases["standing"]), ("football_competition_stage", "id", bases["stage"]), ("football_season", "id", bases["season"]),
        ("match_report", "id", bases["match_event"]), ("match_event", "id", bases["match_event"]), ("match_info", "id", bases["match"]),
        ("team_player", "player_id", bases["player"]), ("football_player", "id", bases["player"]), ("football_team", "id", bases["team"]), ("football_league", "id", bases["league"]),
        ("follow_record", "id", bases["interaction"]), ("favorite_record", "id", bases["interaction"]), ("like_record", "id", bases["interaction"]),
        ("comment", "id", bases["comment"]), ("content_relation", "content_id", bases["content"]), ("content_block", "content_id", bases["content"]), ("content_media", "content_id", bases["content"]), ("content", "id", bases["content"]),
        ("file_resource", "id", bases["file"]), ("user_onboarding", "user_id", bases["user"]), ("user_profile", "user_id", bases["user"]), ("sys_user", "id", bases["user"])
    ]
    for table, column, base in cleanup:
        lines.append(f"DELETE FROM {table} WHERE {column} >= {base};")
    lines.extend(["", "SET FOREIGN_KEY_CHECKS = 1;", ""])

    insert(lines, "sys_user", ["id", "username", "password_hash", "role_type", "onboarding_completed", "status", "create_time"], users)
    insert(lines, "football_league", ["id", "league_name", "league_name_en", "country", "logo_url", "season", "league_type", "sort_order"], leagues)
    insert(lines, "football_season", ["id", "league_id", "season_code", "season_name", "start_date", "end_date", "current_flag", "status", "source", "source_record_id", "source_updated_at", "synced_at", "is_deleted"], seasons)
    insert(lines, "football_competition_stage", ["id", "league_id", "season_id", "stage_type", "stage_name", "group_code", "sort_order", "status", "is_deleted"], stages)
    insert(lines, "football_team", ["id", "team_name", "team_name_en", "short_name", "logo_url", "country", "city", "home_stadium", "founded_year", "coach_name", "market_value", "follower_count"], teams)
    insert(lines, "football_player", ["id", "player_name", "player_name_en", "avatar_url", "nationality", "shirt_number", "position", "birth_date", "height_cm", "weight_kg", "market_value", "follower_count"], players)
    insert(lines, "team_player", ["id", "team_id", "player_id", "team_type", "season", "shirt_number", "position", "status"], team_players)
    insert(lines, "user_profile", ["id", "user_id", "nickname", "avatar_url", "bio", "main_team_id", "status", "post_count", "follower_count", "following_count", "team_follow_count", "player_follow_count"], profiles)
    insert(lines, "user_onboarding", ["id", "user_id", "main_team_id", "selected_team_ids", "selected_player_ids", "completed", "completed_time"], onboardings)
    insert(lines, "file_resource", ["id", "user_id", "biz_type", "original_name", "storage_name", "object_key", "relative_path", "url", "content_type", "extension", "size_bytes", "storage_type", "status"], file_rows)
    insert(lines, "match_info", ["id", "league_id", "season", "round_name", "home_team_id", "away_team_id", "home_score", "away_score", "match_time", "venue", "match_status", "important_level", "has_report"], matches)
    insert(lines, "match_event", ["id", "match_id", "team_id", "player_id", "assist_player_id", "event_type", "minute", "extra_minute", "score_after", "description", "has_debate"], events)
    insert(lines, "football_standing", ["id", "league_id", "season_id", "stage_id", "group_code", "team_id", "rank_no", "played", "won", "drawn", "lost", "goals_for", "goals_against", "goal_difference", "points", "deduction_points", "form_text", "source", "source_updated_at", "is_deleted"], standings)
    insert(lines, "football_player_competition_stat", ["id", "league_id", "season_id", "stage_id", "player_id", "team_id", "appearances", "starts", "minutes", "goals", "assists", "yellow_cards", "red_cards", "shots", "shots_on_target", "saves", "rating", "source", "source_updated_at", "is_deleted"], player_stats)
    insert(lines, "football_team_competition_stat", ["id", "league_id", "season_id", "stage_id", "team_id", "played", "goals_for", "goals_against", "assists", "yellow_cards", "red_cards", "shots", "shots_on_target", "corners", "fouls", "clean_sheets", "avg_rating", "source", "source_updated_at", "is_deleted"], team_stats)
    insert(lines, "content", ["id", "content_type", "content_format", "card_type", "title", "summary", "body", "cover_url", "author_id", "source_type", "is_official", "view_count", "like_count", "comment_count", "favorite_count", "hot_score", "publish_time", "status", "is_deleted"], contents)
    insert(lines, "content_media", ["id", "content_id", "media_type", "media_url", "thumbnail_url", "width", "height", "sort_order", "status", "is_deleted"], media)
    insert(lines, "content_block", ["id", "content_id", "block_type", "text_content", "media_file_id", "media_url", "sort_order", "status", "is_deleted"], blocks)
    insert(lines, "content_relation", ["id", "content_id", "relation_type", "relation_id", "confidence", "source_type", "status", "is_deleted"], relations)
    insert(lines, "match_report", ["id", "match_id", "content_id", "report_type", "status", "is_deleted"], reports)
    insert(lines, "comment", ["id", "target_type", "target_id", "parent_id", "root_id", "reply_to_user_id", "user_id", "content_text", "like_count", "reply_count", "hot_score", "is_top", "status", "is_deleted", "create_time"], [tuple(row) for row in comments])
    insert(lines, "like_record", ["id", "user_id", "target_type", "target_id", "status"], likes)
    insert(lines, "favorite_record", ["id", "user_id", "target_type", "target_id", "status"], favorites)
    insert(lines, "follow_record", ["id", "user_id", "follow_type", "target_id", "is_main", "status", "is_deleted"], follows)
    lines.append("-- End of deterministic T14/T15 demo dataset.")
    SQL_PATH.write_text("\n".join(lines) + "\n", encoding="utf-8", newline="\n")

    summary = {
        "users": len(users), "leagues": len(leagues), "teams": len(teams), "players": len(players),
        "matches": len(matches), "events": len(events), "seasons": len(seasons), "stages": len(stages),
        "standings": len(standings), "player_stats": len(player_stats), "team_stats": len(team_stats),
        "contents": len(contents), "blocks": len(blocks),
        "comments": len(comments), "content_likes_active": sum(1 for r in likes if r[2] == "CONTENT" and r[4] == "ACTIVE"),
        "comment_likes_active": sum(1 for r in likes if r[2] == "COMMENT" and r[4] == "ACTIVE"),
        "favorites_active": len(favorites), "follows": len(follows), "files": len(file_rows)
    }
    print(json.dumps(summary, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--scope", choices=("full", "t15", "t16"), default="full")
    parser.add_argument("--mode", choices=("reset", "incremental"), default="reset")
    args = parser.parse_args()
    if args.scope in ("t15", "t16") or args.mode == "incremental":
        if args.scope not in ("t15", "t16") or args.mode != "incremental":
            parser.error("incremental mode requires --scope t15 or --scope t16")
        incremental_path = ROOT / "scripts" / "sql" / f"seed-{args.scope}-incremental.sql"
        if not incremental_path.is_file():
            parser.error(f"missing incremental seed: {incremental_path}")
        print(json.dumps({"scope": args.scope, "mode": "incremental", "sql": str(incremental_path)}, indent=2))
    else:
        main()
