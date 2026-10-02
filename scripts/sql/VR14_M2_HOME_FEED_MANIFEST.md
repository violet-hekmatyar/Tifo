# VR14-M2 Home feed seed manifest

This additive demo-only adjustment tunes `hot_score` for fixed DEMO
VR14/P1 content rows listed in the seed. It lets the existing recommendation
composition expose meaningful fixed content alongside real match, ranking,
player-rating and discussion cards, without changing user-generated rows. It
does not modify the feed algorithm, API contract, page size, or non-demo rows.

- Seed: `VR14_M2_HOME_FEED_SEED.sql`（固定 DEMO 目标共 144 条有效记录：116 + 10 + 12 + 6；另有 4 条已删除记录受保护不写入）
- Validator: `VR14_M2_HOME_FEED_VALIDATOR.sql`
- Rollback: `VR14_M2_HOME_FEED_ROLLBACK.sql`
- Idempotence: rerunning the seed writes the same fixed target scores.
- Rollback scope: exact fixed content IDs plus author/source/remark ownership predicates; each row is restored only while its hot score still equals the VR14 seed value. No insert/delete/truncate and no non-DEMO rows.
