# P1-M1 媒体资产清单

本目录下的 `p1-media/*.png` 是 P1-M1 开发环境专用栅格演示资源。

- `player-flagship.png`：虚构球员头像，绑定 DEMO 球员 `14000000000000061`、`14000000000000067`。
- `team-crest-crimson.png`：虚构队徽，绑定 DEMO 球队 `13000000000000011`（AC 米兰）。
- `team-crest-cobalt.png`：虚构队徽，绑定 DEMO 球队 `13000000000000012`（尤文图斯）。
- `user-demo.png`：虚构用户头像，绑定 DEMO 用户 `11000000000000001`—`11000000000000003`。
- `cover-stadium.png`、`cover-training.png`、`cover-tactics.png`：足球主题内容封面，绑定 DEMO 内容 `16000000000000001`—`16000000000000006`。

来源/许可：使用 OpenAI Image Generation 生成的原创开发演示位图；不含第三方照片、真实俱乐部徽标、可识别人物或品牌文字。仅用于本项目开发/验收数据，不代表真实球队或球员。

数据库入口：`P1_M1_MEDIA_SEED.sql`。

回滚：先执行 `P1_M1_MEDIA_ROLLBACK.sql`，再按需移除 `src/main/resources/static/demo/p1-media/` 资源。回滚 SQL 仅匹配 P1-M1 新 URL，不会覆盖之后被其他流程修改的值。
