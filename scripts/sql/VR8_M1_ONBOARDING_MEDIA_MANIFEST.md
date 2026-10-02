# VR8-M1 onboarding media manifest

目标来源：`OnboardingService` 按 `follower_count DESC, id ASC` 返回的 6 支球队和 6 名球员。

| 类型 | ID | 名称 | 原 URL | VR8 URL |
| --- | ---: | --- | --- | --- |
| TEAM | 30001 | Barcelona | `/uploads/team/barcelona.png` | `/demo/p1-media/team-crest-crimson.png` |
| TEAM | 30002 | Real Madrid | `/uploads/team/real-madrid.png` | `/demo/p1-media/team-crest-cobalt.png` |
| TEAM | 30003 | Bayern Munich | `/uploads/team/bayern.png` | `/demo/p1-media/team-crest-crimson.png` |
| TEAM | 30004 | Manchester City | `/uploads/team/man-city.png` | `/demo/p1-media/team-crest-cobalt.png` |
| TEAM | 30005 | Arsenal | `/uploads/team/arsenal.png` | `/demo/p1-media/team-crest-crimson.png` |
| TEAM | 30006 | Liverpool | `/uploads/team/liverpool.png` | `/demo/p1-media/team-crest-cobalt.png` |
| PLAYER | 40007 | Erling Haaland | `/uploads/player/haaland.png` | `/demo/p1-media/player-flagship.png` |
| PLAYER | 40001 | Robert Lewandowski | `/uploads/player/lewandowski.png` | `/demo/p1-media/player-flagship.png` |
| PLAYER | 40009 | Mohamed Salah | `/uploads/player/salah.png` | `/demo/p1-media/player-flagship.png` |
| PLAYER | 40002 | Pedri | `/uploads/player/pedri.png` | `/demo/p1-media/player-flagship.png` |
| PLAYER | 40003 | Vinicius Junior | `/uploads/player/vinicius.png` | `/demo/p1-media/player-flagship.png` |
| PLAYER | 40004 | Jude Bellingham | `/uploads/player/bellingham.png` | `/demo/p1-media/player-flagship.png` |

Seed 只更新上述 12 行，具备精确 ID/名称/旧 URL 冲突保护；Rollback 现在按每个球队 ID 的专属 VR8 URL 精确匹配（crimson 只回滚 crimson 目标，cobalt 只回滚 cobalt 目标），球员按专属目标 URL 匹配。VR8-R1 仅修正保护条件和 validator 命名，没有写入数据库，也没有改变媒体映射。执行正式证据后不运行 Rollback。
