# 南看台真实数据源与字段能力说明

> 版本：2026-10-08
> 定位：说明 `football-data.org` 同步进来的真实数据范围、**每张表每个字段的来源与可得性**、以及更新/校验数据库的操作方式。
> 写查询、接口或新功能前先看本文，避免把免费套餐拿不到的字段当成"应该有数据"。

## 1. 数据源与范围

| 项 | 内容 |
|---|---|
| 数据源 | football-data.org v4 **免费套餐**（10 次/分钟，固定 13 个赛事） |
| 采集脚本 | `scripts/data-sync/sync_football_data.py`（仅标准库，见 [RUNBOOK](../scripts/data-sync/RUNBOOK_2026-01-01_to_2026-10-01.md)） |
| 球员照片 | `scripts/data-sync/player_photos.py`（TheSportsDB；football-data 不提供球员照片） |
| 本次导入范围 | 比赛时间 **2026-01-01 ~ 2026-10-01**（跨 2025/26、2026/27 两个赛季） |
| 时区 | 接口给 UTC，脚本统一 **+8 转北京时间**写入 `match_time`（与 `application.yml` 的 `serverTimezone=Asia/Shanghai` 一致） |

### 1.1 已接入的 8 个赛事

| 代码 | API ID | 中文名 | `league_type` |
|---|---:|---|---|
| `PL` | 2021 | 英超 | `LEAGUE` |
| `PD` | 2014 | 西甲 | `LEAGUE` |
| `BL1` | 2002 | 德甲 | `LEAGUE` |
| `SA` | 2019 | 意甲 | `LEAGUE` |
| `FL1` | 2015 | 法甲 | `LEAGUE` |
| `CL` | 2001 | 欧冠 | `CUP` |
| `WC` | 2000 | 世界杯 | `CUP` |
| `EC` | 2018 | 欧洲杯 | `CUP` |

> 赛事中文名在 `scripts/data-sync/name_map_zh.json` 的 `competitions` 里维护；球队/球员中文名同文件，
> 缺失时脚本自动追加空值、SQL 先用英文名兜底。

### 1.2 主键策略

- `football_league` / `football_season` / `football_team` / `football_player` / `match_info` 的 `id`
  **直接用 football-data 全局 ID**（英超 2021、阿森纳 57…），与演示数据 ID 段（1.1e16 起）不冲突。
- 派生表（`football_standing`、`football_team_competition_stat`、`football_player_competition_stat`、
  `football_competition_stage`、`football_team_season_player`、`team_player`、`match_event`）的 `id` 由
  「联赛 + 赛季 + 阶段 + 名次/序号」确定性生成，脚本可反复执行不产生重复行。

## 2. 导入量（截至 2026-10-01 数据）

| 表 | 行数 | 说明 |
|---|---:|---|
| `football_league` | **8** | 上表 8 个赛事 |
| `football_season` | **14** | 5 大联赛 + 欧冠各 2 个赛季，世界杯 / 欧洲杯各 1 个 |
| `football_competition_stage` | **25** | 常规赛 / 联赛阶段 / 小组赛 / 32强 / 附加赛 / 16强 / 8强 / 4强 / 三四名 / 决赛 |
| `football_team` | **192** | 俱乐部 + 国家队；含队徽、国家、主场、建队年 |
| `football_player` | **4050** | 其中 **2937 人（72.5%）有真实头像** |
| `team_player` | **4530** | 通用阵容表（按「队伍 + 赛季年份」） |
| `football_team_season_player` | **5428** | **App 球队/球员详情实际读的阵容表**（按「赛事 + 赛季 id + 队伍」） |
| `football_standing` | **312** | 只取 `type=TOTAL`（不含主/客场分表） |
| `football_team_competition_stat` | **312** | 球队榜（场次/进失球，由积分榜派生） |
| `football_player_competition_stat` | **1296** | 射手榜（每赛季前 100 名） |
| `match_info` | **1403** | 其中 **172 场 `important_level > 0`**（含 9 场半决赛及之后） |
| `match_event` | **0** | 需 `--with-events` 另跑（见 §5.3） |
| 其它足球表 | **0** | 见 §3.2：首发、出场统计、战报、荣誉、生涯、玩家评分均无真实数据 |

## 3. 字段能力矩阵

**图例**：`API` = 接口直接给；`派生` = 脚本计算；`固定` = 脚本常量；`空` = 真实数据下为空（默认值）；
`TheSportsDB` = 由照片脚本单独写入。

### 3.1 逐表字段来源

**`football_league`**（UPSERT）

| 字段 | 来源 |
|---|---|
| `id` | `API` competition.id |
| `league_name` | 中英对照表（缺失时用 `league_name_en`） |
| `league_name_en` | `API` name |
| `country` | `API` area.name |
| `logo_url` | `API` emblem |
| `season` | `API` currentSeason.startDate 年份 |
| `league_type` / `sort_order` | `固定`（按赛事代码） |

**`football_season`**（UPSERT）：`id`/`start_date`/`end_date` ← `API` season；`season_code` ← startDate 年；
`season_name` ← `2025/26` 或单年 `2026`；`current_flag` ← `派生`（今天是否落在赛季区间）；
`source='FOOTBALL_DATA'`、`source_record_id`、`source_updated_at`、`synced_at`。

**`football_competition_stage`**（先删后插）：`stage_type` ← `API` stage（经别名归一，见 §6.3）；
`stage_name` ← 中文映射；`group_code` = `空`（NULL）；`sort_order` ← 阶段顺序序号。

**`football_team`**（UPSERT，**字段级合并**）

| 字段 | 来源 |
|---|---|
| `id` / `team_name_en` / `short_name` / `logo_url` | `API` id / name / shortName‖tla / crest |
| `team_name` | 中英对照表（缺失时用 `team_name_en`） |
| `country` | `API` area.name（注意：`matches`/`standings` 里的球队对象没有该字段，脚本按字段合并写入，不会被覆盖成 NULL） |
| `home_stadium` | `API` venue |
| `founded_year` | `API` founded |
| `coach_name` | `API` coach.name——**免费套餐多数为 null**（实测 85/192 有值） |
| `city` / `market_value` / `follower_count` | `空`（接口不提供 / 用户行为字段，脚本不碰） |
| `extra_json` | `派生` `{source, lastUpdated}` |

**`football_player`**（UPSERT）

| 字段 | 来源 |
|---|---|
| `id` / `player_name_en` / `nationality` | `API` id / name / nationality |
| `player_name` | 中英对照表（缺失时用 `player_name_en`） |
| `position` | `派生`：Goalkeeper→`GK`、Defence→`DF`、Midfield→`MF`、Offence→`FW` |
| `birth_date` | `API` dateOfBirth |
| `avatar_url` | `TheSportsDB` 照片（§5.4）；无照片时为 NULL，App 用本地首字母头像兜底 |
| `remark` | 照片来源与授权：`photo:TheSportsDB idPlayer=… license=… credit=…` |
| `shirt_number` / `height_cm` / `weight_kg` / `market_value` | `空`（接口不提供） |

**`team_player` / `football_team_season_player`**（先删后插）：`position`/`shirt_number` ← `API` squad；
`shirt_number` 实际基本为 `空`（接口不返回球衣号）；`season` 取该队所在赛事**当前赛季年份**；
`football_team_season_player` 的 `position` 是 NOT NULL，位置缺失时兜底 `MF`。

**`match_info`**（UPSERT）

| 字段 | 来源 |
|---|---|
| `id` / `home_team_id` / `away_team_id` | `API` match.id / homeTeam.id / awayTeam.id |
| `season` | `派生` 赛季年份（与 `football_season.season_code` 对齐，供赛季跳转查询） |
| `round_name` | `派生` 中文轮次：联赛 `第N轮`、小组赛 `A组 第N轮`、淘汰赛用阶段中文名 |
| `home_score` / `away_score` | `API` score.fullTime |
| `match_time` | `API` utcDate **+8** |
| `match_status` | `派生`：`SCHEDULED`/`LIVE`/`FINISHED`（`TIMED`→SCHEDULED、`IN_PLAY`/`PAUSED`→LIVE、`AWARDED`→FINISHED、延期/取消→SCHEDULED） |
| `important_level` | `派生` 启发式：`3` 半决赛及之后、`2` 其他淘汰赛、`1` 联赛前 6 对阵、`0` 其他 |
| `extra_json` | `派生` `{source, apiStatus, stage, stageId, matchday, group, lastUpdated}` |
| `venue` / `has_report` | `空`（接口不提供场地；战报需自研内容） |

**`football_standing`**（先删后插）：`rank_no`/=position、`played`/=playedGames、`won`/`drawn`/`lost`、
`goals_for`/`goals_against`/`goal_difference`/`points`/=对应字段、`form_text`/=form；
`deduction_points` = `空`（0）；`group_code` ← 小组阶段的分组。

**`football_player_competition_stat`**（先删后插）：`appearances`/=playedMatches、`goals`、`assists`；
`starts`/`minutes`/`yellow_cards`/`red_cards`/`shots`/`shots_on_target`/`saves`/`rating` = `空`。

**`football_team_competition_stat`**（**UPSERT，只更新三列**）：`played`/`goals_for`/`goals_against` 由积分榜
`派生`；`assists`/牌面/射门/角球/犯规/零封/评分等列**保留库内已有值**（接口不提供，避免被清零）。

**`match_event`**（仅 `--with-events`，按场先删后插）：`event_type` ← 进球 `GOAL`/`OWN_GOAL`/`PENALTY_GOAL`、
`YELLOW_CARD`/`YELLOW_RED_CARD`/`RED_CARD`、`SUBSTITUTION`；`minute`/`extra_minute`(=injuryTime)/`score_after`；
`description` ← 换人 `out → in`。

### 3.2 无真实数据的表（当前为空，勿假设有值）

| 表 | 现状 | 补齐方式 |
|---|---|---|
| `match_event` | 0 行 | `--with-events`（每场 1 次请求） |
| `football_match_lineup` / `football_match_player_appearance` | 0 行 | 免费套餐无首发/出场接口；需付费源或自研 |
| `football_match_team_stat` / `football_match_player_stat` | 0 行 | 同上（可由事件推导部分字段） |
| `match_report` | 仅演示行 | 战报需自研 / 第三方内容 |
| `football_team_honor` / `football_player_team_history` | 仅演示行 | 接口不提供；TheSportsDB 可补荣誉、生涯需另找源 |
| `football_user_player_rating` | 仅演示行 | 用户行为数据，非同步范围 |

## 4. App 页面 ↔ 接口 ↔ 表

| App 位置 | 接口 | 主要表 |
|---|---|---|
| 数据页「重要」 | `GET /api/app/football/matches/important` | `match_info`（`important_level > 0` 倒序） |
| 数据页「联赛 / 欧冠 / 杯赛」 | `/leagues` → `/leagues/{id}/seasons` → `/seasons/{id}/stages` | `football_league` / `football_season` / `football_competition_stage` |
| 赛程 | `GET /api/app/football/matches?leagueId&teamId&date&status` | `match_info` |
| 积分榜 | `GET /api/app/football/standings?leagueId&seasonId&stageId&groupCode` | `football_standing` + `football_team` |
| 球员榜 | `GET /api/app/football/player-ranks?rankType=…` | `football_player_competition_stat` + `football_player` |
| 球队榜 | `GET /api/app/football/team-ranks?rankType=…` | `football_team_competition_stat` |
| 球队详情 → 球员 | `GET /api/app/football/teams/{id}/players` | **`football_team_season_player`**（不是 `team_player`） |
| 球员 / 比赛详情 | `/players/{id}/overview`、`/matches/{id}` | `football_player` / `match_info` / 各榜单表 |

> 关键约束：积分榜/球员榜/球队榜都经过 `requireSeason()` + `resolveStage()` 按
> `league_id + season_id + stage_id` **等值查询**，所以 `football_season` 与 `football_competition_stage`
> 必须与榜单一起写入（脚本已保证），阶段的 `stage_name` 也已中文化。

## 5. 更新数据库

### 5.1 全量 / 冷启动

```powershell
cd D:\Football-APP\scripts\data-sync
$env:FOOTBALL_DATA_API_KEY = "<token>"
py -3 sync_football_data.py --date-from 2026-01-01 --date-to 2026-10-01 --out ..\sql\seed_football_data_2026.sql
```

### 5.2 日常增量（近 2 天 + 未来 7 天，约 40 次请求）

```powershell
py -3 sync_football_data.py --date-from 2026-10-05 --date-to 2026-10-13 --out ..\sql\update_20261005.sql
```

- 请求间隔 ≥ 6.5 秒（`request_log.json` 记录时间戳，中断重跑会补足等待）；429 退避 60 秒、连续 3 次失败中止。
- 缓存 `scripts/data-sync/cache/`：赛事/球队 7 天、积分榜/射手榜/比赛 10 分钟；复跑不重复请求。
- 阵容优先取 `/competitions/{code}/teams` payload 自带的全队名单（**含国家队**），只有 payload 没带的
  （如欧冠部分球队）才回退 `/teams/{id}`；后者对国家队返回 403，按「受限资源」跳过并在 SQL 头记录。
- 生成的 SQL 幂等：可重复执行；只覆盖 API 来源字段，`follower_count` 等用户行为字段不触碰。

### 5.3 比赛事件（按需小范围跑）

```powershell
py -3 sync_football_data.py --date-from 2026-09-01 --date-to 2026-09-30 --competitions PL `
    --with-events --events-limit 60 --out ..\sql\events_pl_202609.sql
```

### 5.4 球员头像

```powershell
py -3 player_photos.py                     # 两遍：按球队 + 按球员搜索；缓存可续跑
py -3 player_photos.py --no-player-pass    # 只跑球队那遍（约 15 分钟）
```

- 匹配规则：球队名归一化（支持 `FC Bayern München` → `Bayern Munich`）、**硬性只要足球**（避免命中同名篮球队/冰球队）、
  球员姓名归一化 + **生日必须一致**；生成前逐个校验图片可用性（HTTP 200 且 `image/*`）。
- 图片取 200×200 的 `small` 变体（原图 228 KB → 39 KB）；`remark` 写入来源与授权信息。

### 5.5 演示数据退役与校验

```sql
source scripts/sql/retire-demo-data.sql;             -- 软删演示赛事与演示账号（可回滚）
source scripts/sql/validate-retired-demo-data.sql;   -- 校验：演示行应全为 0
source scripts/sql/retire-demo-data-rollback.sql;    -- 需要时回滚
```

### 5.6 离线自检（不联网、不耗额度）

```powershell
py -3 sync_football_data.py --self-test      # 映射 / ID / 重要等级 / SQL 渲染
py -3 selftest_football_sync.py              # 本地假 API 跑完整同步并校验生成的 SQL
py -3 player_photos.py --self-test           # 队名/球员匹配规则（含错配防护）
```

## 6. 已知数据特性与坑

1. **跨赛季**：同一赛事在一个时间区间内可能命中两个赛季 → 两行 `football_season`、两套阶段与榜单，属预期。
2. **欧洲杯没有积分榜**：接口对 `EC standings` 返回 404（只有射手榜），赛季为 2024。
3. **阶段键有多种写法**：`LEAGUE_STAGE`、`GROUP_STAGE`、`LAST_16`、`LAST_32`、`PLAYOFFS`、`THIRD_PLACE`…
   脚本按别名表归一（`LAST_16`→`ROUND_OF_16`、`LAST_32`→`ROUND_OF_32`、`PLAYOFFS`→`PLAYOFF`、
   `THIRD_PLACE`→`THIRD_PLACE_PLAY_OFF`），**新增赛事时先看真实响应里有没有新写法**，否则阶段名会退化成英文、
   淘汰赛也不会进「重要」tab。
4. **55 支国家队的队徽是 SVG**（football-data 没有 PNG 版本），App 端必须走 `SvgPicture`
   （`AppEntityAvatar` 已按 `.svg` 自动分流），否则会一直显示兜底徽标。
5. **国家队阵容**：`/competitions/{code}/teams` 的 payload 自带名单，所以世界杯/欧洲杯球队**是有阵容的**；
   被 403 限制的只是独立端点 `/teams/{id}`。
6. **射手榜只有每赛季前 100 名**：阵容页里多数球员的出场/进球会是 0，属数据源限制，不是 bug。
7. **`assists` 等计数列接口可能返回 null**，而表里是 NOT NULL → 脚本统一写 0（否则整批导入失败）。
8. **导出中文注意字符集**：用容器里的 mysql 客户端时务必带 `--default-character-set=utf8mb4`，
   否则 `Köln` 这类名字会按 latin1 输出、UTF-8 解码失败（`player_photos.py` 已处理）。
9. **外链依赖**：队徽走 football-data CDN、球员头像走 TheSportsDB CDN，App 需联网；如需离线要另做本地镜像。
10. **重要等级是启发式**（淘汰赛深浅 + 联赛前 6 对阵），不反映真实热度；人工调整后重跑脚本会被重算覆盖。
11. **TheSportsDB 免费接口每队只返回 10 名球员**，所以头像必须依赖第二遍「按球员搜索」才能到 ~72% 覆盖。

## 7. 文件清单

| 文件 | 作用 |
|---|---|
| `scripts/data-sync/sync_football_data.py` | 主同步脚本（唯一入口，标准库实现） |
| `scripts/data-sync/selftest_football_sync.py` | 本地假 API 的端到端回归 |
| `scripts/data-sync/player_photos.py` | 球员头像抓取与匹配 |
| `scripts/data-sync/name_map_zh.json` | 中英对照表（赛事已填，球队/球员自动追加待补译） |
| `scripts/data-sync/cache/` | API 响应缓存（`thesportsdb/` 含图片校验结果），可随时删 |
| `scripts/data-sync/RUNBOOK_2026-01-01_to_2026-10-01.md` | 一次完整采集的操作手册与校验 SQL |
| `scripts/data-sync/request_log.json` | 限速依据（时间戳） |
| `scripts/sql/seed_football_data_2026.sql` | 本次全量产物（幂等，可重复导入） |
| `scripts/sql/update_player_avatars.sql` | 球员头像 UPDATE 产物 |
| `scripts/sql/retire-demo-data.sql` / `-rollback` / `validate-retired-demo-data.sql` | 演示数据退役三件套 |
| `scripts/sql/migrations/V020__standings_drop_rank_unique.sql` | 删除 `uk_standing_scope_rank`（重复名次不再导致整批失败） |
