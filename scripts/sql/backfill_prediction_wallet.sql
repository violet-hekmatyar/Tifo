-- 给所有存量正常用户补发预测初始本金 1,000 看台豆。
-- 幂等：已有钱包的用户不会重复发；已有 INITIAL_GRANT 流水的用户不会重复记流水。
-- 执行：
--   docker exec -i apihub-mysql mysql -uroot -p123456 --default-character-set=utf8mb4 -D south_stand < scripts/sql/backfill_prediction_wallet.sql

START TRANSACTION;

INSERT INTO prediction_wallet (id, user_id, balance, create_time, update_time)
SELECT UUID_SHORT(), u.id, 1000, NOW(), NOW()
FROM sys_user u
WHERE u.is_deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM prediction_wallet w WHERE w.user_id = u.id
  );

INSERT INTO prediction_bean_log
    (id, user_id, prediction_id, change_amount, balance_after, log_type, remark, create_time)
SELECT UUID_SHORT(), w.user_id, NULL, 1000, 1000, 'INITIAL_GRANT',
       '存量用户补发初始本金 1,000 豆', NOW()
FROM prediction_wallet w
WHERE w.balance = 1000
  AND NOT EXISTS (
      SELECT 1 FROM prediction_bean_log l
      WHERE l.user_id = w.user_id AND l.log_type = 'INITIAL_GRANT'
  );

COMMIT;
