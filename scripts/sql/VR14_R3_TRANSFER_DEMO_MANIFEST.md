# VR14-R3 转会快讯 DEMO 数据清单

- Seed: `VR14_R3_TRANSFER_DEMO_SEED.sql`
- Validator: `VR14_R3_TRANSFER_DEMO_VALIDATOR.sql`
- Rollback: `VR14_R3_TRANSFER_DEMO_ROLLBACK.sql`
- 所有权标记：`VR14_R3_TRANSFER_DEMO_CONTENT` 与 `VR14-R3-transfer-relation-*`
- 固定数据：一条 POST 和三条足球对象关系；未创建球员、球队、媒体或用户记录。
- 唯一新增演示内容明确声明“非真实交易数据”；球员/球队图片复用 P1 媒体，关联已有球员 `14000000000000067` 和球队 `13000000000000012`、`13000000000000011`。
- Seed 使用保留 ID、前置冲突与依赖检查、`INSERT ... ON DUPLICATE KEY UPDATE id=id`，不会更新既有内容或 VR14-M2 的 feed score。
- Validator 核对内容身份、显式展示类型、名称及三条关系；每次 seed 后应执行两次并记录输出。
- Rollback 仅删除这些标记的保留行；如果内容已被评论、点赞或收藏，或保留行所有权/结构发生变化则中止，不清除用户互动。
- 推荐 `hot_score=180` 只用于使这条 R3 演示卡进入候选集；不改全局页大小、R2 排序/组合规则或既有内容热度。

在目标 MySQL 数据库中按顺序执行 Seed 两次、Validator 两次。Rollback 仅在需要撤销本演示数据且无用户互动时执行；Rollback 后再次执行 Validator 应失败且剩余行数为 0。
