# VR7-M1 发布目录清单

- 目录表：`content_publish_subject`，由 `VR7_M1_PUBLISH_SUBJECT_MIGRATION.sql` 幂等创建。
- 话题目录 ID：`16500000000000701`—`16500000000000706`。
- 热点目录 ID：`16500000000000711`—`16500000000000716`。
- VR7 关系 ID：`16300000000000701`—`16300000000000712`，仅连接现有 P1-M3 内容，不修改正文、封面、热度和既有关系。
- 热点图片复用 `/demo/p1-media/player-flagship.png`、`cover-stadium.png`、`cover-training.png`、`cover-tactics.png`。
- Seed 使用 `VR7-M1-*` remark、独立 ID 段、冲突保护和 `ON DUPLICATE KEY UPDATE id=id`；连续执行不得增加行。
- Validator 校验 12 条目录、两种类型、热点图片、关系目标、重复关系和所有权泄漏。
- Rollback 仅删除 VR7 所有权关系和目录行；若发现非 VR7 关系引用，先拒绝删除；不 DROP 目录表。
