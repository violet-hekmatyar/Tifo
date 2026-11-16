#!/usr/bin/env python3
"""查询预测模块关联的真实比赛数据。

默认查询最近 30 天，允许任意起止日期，但区间最长 183 天（约 6 个月）。
优先读取环境变量，以便与后端 application.yml 保持一致：
  MYSQL_HOST / MYSQL_PORT / MYSQL_DATABASE / MYSQL_USERNAME / MYSQL_PASSWORD

示例：
  py -3 query_prediction_data.py --from 2026-10-01 --to 2026-10-31
  py -3 query_prediction_data.py --from 2026-10-15 --to 2026-10-22 --match-id 575370
  py -3 query_prediction_data.py --from 2026-09-01 --to 2026-10-31 --format json
"""
from __future__ import annotations

import argparse
import json
import os
import sys
from datetime import date, datetime, timedelta
from decimal import Decimal

try:
    import pymysql
except ImportError as exc:  # pragma: no cover
    raise SystemExit("缺少 pymysql，请先执行：py -3 -m pip install pymysql") from exc

MAX_RANGE_DAYS = 183


def parse_date(value: str) -> date:
    try:
        return datetime.strptime(value, "%Y-%m-%d").date()
    except ValueError as exc:
        raise argparse.ArgumentTypeError(f"日期格式应为 YYYY-MM-DD：{value}") from exc


def decimal_to_number(value):
    if isinstance(value, Decimal):
        return float(value)
    if isinstance(value, datetime):
        return value.strftime("%Y-%m-%d %H:%M:%S")
    return value


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="按时间段查询预测模块与真实比赛数据")
    parser.add_argument("--from", dest="date_from", type=parse_date,
                        default=date.today() - timedelta(days=30), help="开始日期 YYYY-MM-DD")
    parser.add_argument("--to", dest="date_to", type=parse_date, default=date.today(),
                        help="结束日期 YYYY-MM-DD")
    parser.add_argument("--match-id", type=int, help="只查询某场比赛")
    parser.add_argument("--format", choices=("table", "json"), default="table")
    parser.add_argument("--limit", type=int, default=200, help="最多返回比赛数，默认 200")
    return parser


def fetch_rows(args):
    span = (args.date_to - args.date_from).days
    if span < 0:
        raise SystemExit("结束日期不能早于开始日期")
    if span > MAX_RANGE_DAYS:
        raise SystemExit(f"查询区间最长 {MAX_RANGE_DAYS} 天（约 6 个月），当前 {span} 天")

    conn = pymysql.connect(
        host=os.getenv("MYSQL_HOST", "127.0.0.1"),
        port=int(os.getenv("MYSQL_PORT", "3306")),
        user=os.getenv("MYSQL_USERNAME", "root"),
        password=os.getenv("MYSQL_PASSWORD", "123456"),
        database=os.getenv("MYSQL_DATABASE", "south_stand"),
        charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor,
    )
    sql = """
        SELECT m.id AS match_id,
               m.match_time,
               m.match_status,
               m.round_name,
               l.league_name,
               ht.team_name AS home_team,
               at.team_name AS away_team,
               m.home_score,
               m.away_score,
               c.home_percent,
               c.draw_percent,
               c.away_percent,
               c.participant_count,
               COUNT(p.id) AS prediction_count,
               SUM(CASE WHEN p.status = 'SETTLED' THEN 1 ELSE 0 END) AS settled_count,
               ROUND(AVG(p.insight_score), 1) AS avg_insight_score,
               SUM(CASE WHEN p.challenge_result = 'HIT' THEN 1 ELSE 0 END) AS hit_count,
               SUM(CASE WHEN p.stake > 0 THEN p.stake ELSE 0 END) AS total_stake,
               SUM(CASE WHEN p.reward_beans > 0 THEN p.reward_beans ELSE 0 END) AS total_reward
        FROM match_info m
        JOIN football_league l ON l.id = m.league_id
        JOIN football_team ht ON ht.id = m.home_team_id
        JOIN football_team at ON at.id = m.away_team_id
        LEFT JOIN match_prediction_consensus c ON c.match_id = m.id
        LEFT JOIN match_prediction p ON p.match_id = m.id
        WHERE m.is_deleted = 0
          AND m.match_time >= %s
          AND m.match_time < DATE_ADD(%s, INTERVAL 1 DAY)
    """
    params = [args.date_from, args.date_to]
    if args.match_id:
        sql += " AND m.id = %s"
        params.append(args.match_id)
    sql += """
        GROUP BY m.id, m.match_time, m.match_status, m.round_name, l.league_name,
                 ht.team_name, at.team_name, m.home_score, m.away_score,
                 c.home_percent, c.draw_percent, c.away_percent, c.participant_count
        ORDER BY m.match_time DESC
        LIMIT %s
    """
    params.append(args.limit)
    with conn:
        with conn.cursor() as cursor:
            cursor.execute(sql, params)
            rows = cursor.fetchall()
    return [{key: decimal_to_number(value) for key, value in row.items()} for row in rows]


def print_table(rows):
    if not rows:
        print("没有符合条件的比赛。")
        return
    headers = ["比赛时间", "赛事", "对阵", "比分", "球迷共识", "参与人数",
               "预测数", "已结算", "均分", "命中", "投入豆", "奖励豆"]
    body = []
    for row in rows:
        score = "-" if row["home_score"] is None else f'{row["home_score"]}-{row["away_score"]}'
        consensus = "-"
        if row["home_percent"] is not None:
            consensus = f'{row["home_percent"]:.0f}/{row["draw_percent"]:.0f}/{row["away_percent"]:.0f}'
        body.append([
            str(row["match_time"]),
            str(row["league_name"]),
            f'{row["home_team"]} vs {row["away_team"]}',
            score,
            consensus,
            str(row["participant_count"] or 0),
            str(row["prediction_count"] or 0),
            str(row["settled_count"] or 0),
            str(row["avg_insight_score"] or "-"),
            str(row["hit_count"] or 0),
            str(row["total_stake"] or 0),
            str(row["total_reward"] or 0),
        ])
    widths = [max(len(str(cell)) for cell in column) for column in zip(headers, *body)]
    line = "  ".join(header.ljust(width) for header, width in zip(headers, widths))
    print(line)
    print("-" * len(line))
    for cells in body:
        print("  ".join(str(cell).ljust(width) for cell, width in zip(cells, widths)))
    print(f"\n共 {len(rows)} 场；区间 {rows[-1]['match_time'][:10]} ~ {rows[0]['match_time'][:10]}")


def main() -> int:
    # Windows 控制台默认 GBK，遇到 München 等字符会抛 UnicodeEncodeError。
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    args = build_parser().parse_args()
    rows = fetch_rows(args)
    if args.format == "json":
        print(json.dumps({
            "from": args.date_from.isoformat(),
            "to": args.date_to.isoformat(),
            "count": len(rows),
            "matches": rows,
        }, ensure_ascii=False, indent=2))
    else:
        print_table(rows)
    return 0


if __name__ == "__main__":
    sys.exit(main())
