# T14 Demo Dataset

This directory defines a deterministic, offline dataset for local development and UI demonstrations. It is synthetic data, not production data, and does not use third-party sports APIs or scraped logos/photos.

## Generate and import

```powershell
py -3 scripts/data/generate-demo-data.py
.\scripts\windows\init-demo-data.ps1

# T15 default: preserve existing data and add only missing rank data
py -3 scripts/data/generate-demo-data.py --scope t15 --mode incremental
.\scripts\windows\init-demo-data.ps1 -Mode Incremental

# Destructive full demo rebuild: both flags are mandatory
.\scripts\windows\init-demo-data.ps1 -Mode ResetDemo -ConfirmReset
```

The generator uses only the Python standard library, reads `demo-config.json` and `demo-names.json`, writes UTF-8 `scripts/sql/seed-demo.sql`, and creates reusable SVG assets under `src/main/resources/static/demo/`. The fixed random seed is `20260722`; the baseline time is `2026-07-20 12:00:00 +08:00`. Repeated generation with the same files produces the same SQL bytes.

The dataset uses Chinese-first league, team, player, user, content, and comment display text. Generic images are generated locally and intentionally reused. `file_resource` records point to committed `/demo/contents/*.svg` resources and use `LOCAL` storage metadata.

## Development accounts

Accounts `demo_user_01` through `demo_user_36` are development-only. Their shared password is `Demo123456!`, stored in MySQL as a BCrypt hash. Never auto-import `seed-demo.sql` in production.

## ID ranges

| Entity | Start ID |
| --- | ---: |
| User | 11000000000000001 |
| League | 12000000000000001 |
| Team | 13000000000000001 |
| Player | 14000000000000001 |
| Match | 15000000000000001 |
| Match event | 15100000000000001 |
| Content | 16000000000000001 |
| Content block | 16100000000000001 |
| Comment | 17000000000000001 |
| Interaction | 18000000000000001 |
| File | 19000000000000001 |

## Validation

```powershell
py -3 scripts/data/validate-demo-data.py
.\scripts\windows\smoke-demo-data.ps1 -Port 8080
.\scripts\windows\check-t14.ps1
```

Database validation checks foreign references, active uniqueness, score/goal agreement, report status, ARTICLE block rules, comment trees, counter/detail agreement, and minimum pagination volumes. User main teams always have a matching active TEAM follow. Active players have exactly one current team. There is no USER, TEAM, or PLAYER follow-count cap.
# T15 榜单数据

生成器同时创建 16 个赛季、16 个阶段、5 份各 8 队的当前赛季积分榜、240 条球员赛事统计和 40 条球队赛事统计。所有来源均为 `DEMO`，更新时间固定为演示基准时间；积分、净胜球、排名和球队统计交叉一致。数据仅用于开发与展示，不代表官方体育数据。
