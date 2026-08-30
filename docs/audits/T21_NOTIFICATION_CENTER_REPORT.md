# T21 互动消息通知中心报告

## 1. 原始需求与现状审计

原始《tifo需求文档》第 42 页仅给出底部“消息”入口和页面视觉占位，没有通知分类、已读或跳转契约。T21 因此以该入口为产品边界，并按 T21 prompt 收口第一版数据库持久化 + HTTP 拉取通知中心，不实现私信、WebSocket、Push、邮件、短信、比赛提醒、推荐推送或 MQ。

开工前真实调用链：

| 行为 | API | Service | T21 触发 |
| --- | --- | --- | --- |
| 内容点赞 | `POST /api/app/likes/toggle` | `InteractionService.toggleLike` | 激活时 `CONTENT_LIKED` |
| 根评论 | `POST /api/app/comments` | `CommentService.create` | `CONTENT_COMMENTED` |
| 评论回复 | 同上，带 `parentId` | `CommentService.create` | 仅 `COMMENT_REPLIED` |
| 评论点赞 | `POST /api/app/comments/{id}/likes/toggle` | `CommentService.toggleLike` | 激活时 `COMMENT_LIKED` |
| 用户关注 | 用户主页 follow / 通用 follow toggle | `UserSocialService` / `FollowService` | 仅 USER 激活时 `USER_FOLLOWED` |

开发前没有 notification/message 表、Controller、Service 或未读数接口。统一响应和分页继续使用 `Result` / `PageResult`，鉴权及错误码沿用现有 40101、40301、40401、40001 风格。

## 2. 新增表与字段设计

V019 增量创建 `notification`：

- 身份：`id`、`recipient_user_id`、可空 `actor_user_id`。
- 类型：`notification_type`，只允许代码枚举中的六种类型。
- 跳转：主 `target_type/target_id` 与次 `secondary_target_type/secondary_target_id`。
- 展示：`title`、`content`。
- 状态：`read_flag`、`read_time`、简单 `ACTIVE`、`is_deleted`。
- 幂等：非空 `dedup_key`。
- 审计时间：`create_time`、`update_time`。

索引：唯一 `uk_notification_dedup`；接收人时间、接收人未读时间和目标三组查询索引。迁移使用 `CREATE TABLE IF NOT EXISTS`，不含 DROP/TRUNCATE/DELETE/旧表 UPDATE。

## 3. 通知类型、目标与去重

| notificationType | target / secondary | dedupKey | 默认文案 |
| --- | --- | --- | --- |
| `CONTENT_LIKED` | CONTENT / null | `CONTENT_LIKED:{actorId}:{contentId}` | `{actorName} 点赞了你的内容` |
| `CONTENT_COMMENTED` | CONTENT / COMMENT | `CONTENT_COMMENTED:{commentId}` | `{actorName} 评论了你的内容` |
| `COMMENT_REPLIED` | COMMENT / CONTENT | `COMMENT_REPLIED:{commentId}` | `{actorName} 回复了你的评论` |
| `COMMENT_LIKED` | COMMENT / CONTENT | `COMMENT_LIKED:{actorId}:{commentId}` | `{actorName} 点赞了你的评论` |
| `USER_FOLLOWED` | USER / null | `USER_FOLLOWED:{actorId}:{recipientId}` | `{actorName} 关注了你` |
| `SYSTEM` | SYSTEM / null | `SYSTEM:{stableBusinessKey}` | Service 调用方提供 |

类型与 targetType 分别由 `NotificationType`、`NotificationTargetType` 安全枚举控制。recipient == actor 时直接跳过。唯一键承担并发/重试的最终去重；重复键异常被安全忽略。

## 4. 触发、取消与失败隔离

- 只有 LIKE/FOLLOW 从无记录或 CANCELLED 变为 ACTIVE 时调用通知；取消不触发、不删除历史通知。
- 点赞 → 取消 → 再点赞及关注/评论点赞的同类流程不会生成第二条通知，避免反复骚扰。
- 根评论只产生 `CONTENT_COMMENTED`；回复只产生 `COMMENT_REPLIED`，不会重复通知内容作者。
- 用户关注仅 `follow_type=USER` 触发，球队和球员关注不触发。
- 主业务事务活跃时，通知任务登记为 `afterCommit`；提交后由 `NotificationWriter` 的 `REQUIRES_NEW` 独立事务写入。所有通知异常均被捕获并记录不含敏感数据的摘要，不能回滚点赞、评论、回复或关注。
- SYSTEM 只提供 `createSystemNotification` Service 能力，校验接收用户和稳定业务 key，没有 App 创建接口。

## 5. HTTP 契约

| 接口 | 行为 |
| --- | --- |
| `GET /api/app/notifications` | 登录用户列表；支持数据库分页、type、READ/UNREAD 过滤，时间/id 倒序 |
| `GET /api/app/notifications/unread-count` | SQL COUNT 返回 `{total, byType}`；第一版 `byType` 为空映射 |
| `POST /api/app/notifications/{id}/read` | 仅按当前 recipient 更新；重复调用成功；他人 ID 同样幂等成功，不泄露存在性 |
| `POST /api/app/notifications/read-all` | 当前用户所有 ACTIVE 未读改为已读并返回 `updatedCount` |

列表返回 VO，不返回 Entity。每条含 notificationType、结构化 actor、主/次 target、文案、已读时间、创建时间、`targetAvailable` 和可选 preview。

## 6. Preview、删除目标与性能

数据库先用显式 `COUNT + LIMIT/OFFSET` 分页 notification，再收集本页 ID：

1. actor profile 单次 IN 查询；
2. CONTENT 单次 batch 查询；
3. COMMENT 单次 batch 查询；
4. USER target 单次 batch 查询。

40 条通知仍是固定批量查询，不逐通知查 actor/target。CONTENT preview 返回标题和封面；COMMENT preview 返回最多 80 字摘要。目标缺失、软删除或不可访问时保留通知并返回 `targetAvailable=false`，列表不报 500。

## 7. Demo 数据与数据校验

本轮 0 seed。Smoke 每次通过两个新用户的真实点赞、评论、回复、评论点赞和关注自然生成少量通知。

`validate-t21-notification.sql` 检查：接收人缺失、actor 缺失、自通知、非法 notificationType、非法 targetType、重复 dedupKey、未读却有 readTime、已读却无 readTime。两轮检查全部为 0；正常软删除 target 不作为异常。

## 8. 自动化测试与回归结果

- `NotificationServiceTests`：六类型、自通知、预览和删除目标。
- `NotificationControllerTests`：列表过滤委托、未读、单条/全部已读和未登录拒绝。
- `NotificationTriggerIntegrationTests`：内容点赞、根评论、回复、评论点赞、USER 关注及通知写失败不破坏主业务。
- `NotificationPerformanceStructureTests`：40 条通知固定 actor/content/comment 批量查询。
- `T21IncrementalMigrationTests`：V019 非破坏、幂等和关键索引。
- Java：142 tests，0 failures，0 errors，0 skipped。
- Python：8 tests passed；只有既有 Starlette/httpx2 弃用提示。
- T03-T20 全量 Smoke 两轮均通过，包括推荐 RULE/CF/fallback 和 T20 详情契约。
- T21 Smoke 两轮均通过：五类触发、自通知、去重、取消策略、preview、分页、未读、单条/全部已读、所有权隐藏。
- `check-t21.ps1` 连续两次通过，V019 重跑成功，旧数据逐 ID/关键字段指纹保持，8080/8100 无残留。

## 9. Flutter 后续工作

Flutter 可直接开发第一版消息页面：使用列表分页和 type/readStatus 筛选，角标读取 unread-count，点击后先调用 single read，再按 notificationType + targetType + targetId 跳转。`targetAvailable=false` 时显示“内容已不可用”且禁用跳转。SYSTEM 没有 actor 是正常情况。实时红点需轮询或进入页面刷新；本轮按范围不提供 WebSocket/Push。

不存在阻塞 Flutter 第一版通知页面的后端缺口。可选增强包括 byType 未读统计、通知删除、通知聚合和实时推送，均不在 T21 范围。

## 10. 修改范围与 Git 状态

新增 V019、校验 SQL、notification 包、五组测试、两个 Windows 脚本和本报告；更新互动/关注触发 Service 以及 README 和 5 份工程文档。未新增通知 seed，未重置数据库。

最终 `git status --short` 显示 T21 文档与四个既有 Service 为修改状态，V019、校验、脚本、notification 生产/测试包和本报告为未跟踪状态。未执行 `git add`、`git commit` 或 `git push`。
