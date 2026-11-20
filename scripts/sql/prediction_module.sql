-- ============================================================
-- 比赛详情「预测」模块
-- 依赖：match_info / football_team / football_league / sys_user / user_profile
-- 幂等执行：mysql --default-character-set=utf8mb4 south_stand < prediction_module.sql
-- ============================================================

CREATE TABLE IF NOT EXISTS match_prediction (
    id BIGINT NOT NULL,
    match_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    home_probability INT NOT NULL,
    draw_probability INT NOT NULL,
    away_probability INT NOT NULL,
    predicted_outcome VARCHAR(16) NOT NULL,
    insight_score INT DEFAULT NULL,
    stake INT NOT NULL DEFAULT 0,
    challenge_result VARCHAR(16) DEFAULT NULL,
    reward_beans INT DEFAULT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'SUBMITTED',
    submitted_at DATETIME NOT NULL,
    settled_at DATETIME DEFAULT NULL,
    extra_json JSON DEFAULT NULL,
    remark VARCHAR(512) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_match_user (match_id, user_id),
    KEY idx_user_submitted (user_id, submitted_at),
    KEY idx_match_status (match_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS match_prediction_consensus (
    match_id BIGINT NOT NULL,
    home_percent DECIMAL(5,2) NOT NULL DEFAULT 0,
    draw_percent DECIMAL(5,2) NOT NULL DEFAULT 0,
    away_percent DECIMAL(5,2) NOT NULL DEFAULT 0,
    participant_count INT NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (match_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS prediction_wallet (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    balance INT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_wallet_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS prediction_bean_log (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    prediction_id BIGINT DEFAULT NULL,
    change_amount INT NOT NULL,
    balance_after INT NOT NULL,
    log_type VARCHAR(32) NOT NULL,
    remark VARCHAR(255) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_bean_user_time (user_id, create_time),
    KEY idx_bean_prediction (prediction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 用户自己的赛前判断轨迹（用于赛后复盘）
CREATE TABLE IF NOT EXISTS prediction_trajectory (
    id BIGINT NOT NULL,
    match_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    stage_label VARCHAR(32) NOT NULL,
    home_percent INT NOT NULL,
    draw_percent INT NOT NULL,
    away_percent INT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    captured_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    KEY idx_trajectory_match_user (match_id, user_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 「更多预测」运营位（非核心概率玩法）
CREATE TABLE IF NOT EXISTS prediction_market (
    id BIGINT NOT NULL,
    match_id BIGINT NOT NULL,
    label VARCHAR(64) NOT NULL,
    participant_count INT NOT NULL DEFAULT 0,
    percent INT NOT NULL DEFAULT 0,
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    PRIMARY KEY (id),
    KEY idx_market_match (match_id, status, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- 演示账号：prediction_demo / Prediction123!
-- 该账号用于本机验证预测、看台挑战、赛后复盘三态。
-- ------------------------------------------------------------
INSERT INTO sys_user (id, username, phone, password_hash, role_type, onboarding_completed, status)
VALUES (900000000000000001, 'prediction_demo', '19900000001',
        '$2b$12$XRM/UjaAV5KsmwOrmCfMkundvPNePM8VD1bCkIPA/.aGXro5rsKw2', 'USER', 1, 'ACTIVE')
ON DUPLICATE KEY UPDATE password_hash = VALUES(password_hash), status = 'ACTIVE';

INSERT INTO user_profile (id, user_id, nickname, bio, main_team_id)
VALUES (900000000000000002, 900000000000000001, '预测体验官', '本机预测模块演示账号', 86)
ON DUPLICATE KEY UPDATE nickname = VALUES(nickname), main_team_id = VALUES(main_team_id);

-- ------------------------------------------------------------
-- 真实比赛种子：2026-10-22 欧冠联赛阶段
-- 575370 Real Madrid 4-1 RB Leipzig（真实赛果，可核对）
-- 575372 Bayern 2-2 Arsenal
-- 575367 PSG 4-2 Barcelona
-- 575370 的 64/22/14 -> 洞察分 90，与产品算例一致。
-- ------------------------------------------------------------
INSERT INTO match_prediction_consensus (match_id, home_percent, draw_percent, away_percent, participant_count)
VALUES (575370, 46.00, 28.00, 26.00, 27846),
       (575372, 41.00, 31.00, 28.00, 19024),
       (575367, 52.00, 24.00, 24.00, 23510)
ON DUPLICATE KEY UPDATE home_percent = VALUES(home_percent),
                        draw_percent = VALUES(draw_percent),
                        away_percent = VALUES(away_percent),
                        participant_count = VALUES(participant_count);

INSERT INTO prediction_trajectory
    (id, match_id, user_id, stage_label, home_percent, draw_percent, away_percent, sort_order, captured_at)
VALUES (900000000000000201, 575370, 900000000000000001, '赛前24小时', 52, 26, 22, 1, '2026-10-21 03:00:00'),
       (900000000000000202, 575370, 900000000000000001, '首发公布', 61, 22, 17, 2, '2026-10-22 02:00:00'),
       (900000000000000203, 575370, 900000000000000001, '阵容锁定', 64, 22, 14, 3, '2026-10-22 02:30:00')
ON DUPLICATE KEY UPDATE home_percent = VALUES(home_percent),
                        draw_percent = VALUES(draw_percent),
                        away_percent = VALUES(away_percent),
                        stage_label = VALUES(stage_label),
                        sort_order = VALUES(sort_order);

INSERT INTO match_prediction
    (id, match_id, user_id, home_probability, draw_probability, away_probability, predicted_outcome,
     insight_score, stake, challenge_result, reward_beans, status, submitted_at, settled_at, extra_json, remark)
VALUES (900000000000000101, 575370, 900000000000000001, 64, 22, 14, 'HOME',
        90, 50, 'HIT', 30, 'SETTLED', '2026-10-22 02:30:00', '2026-10-22 05:00:00',
        JSON_OBJECT(
            'headline', '你的最高概率结果与赛果一致',
            'highlights', JSON_ARRAY(
                '你在首发公布后 8 分钟修正了判断',
                '比 72% 的球迷更早看好皇马',
                '连续 5 场保持概率校准良好'
            )
        ),
        '真实赛果：皇马 4-1 莱比锡')
ON DUPLICATE KEY UPDATE insight_score = VALUES(insight_score),
                        stake = VALUES(stake),
                        challenge_result = VALUES(challenge_result),
                        reward_beans = VALUES(reward_beans),
                        status = VALUES(status),
                        extra_json = VALUES(extra_json),
                        remark = VALUES(remark);

INSERT INTO prediction_wallet (id, user_id, balance)
VALUES (900000000000000301, 900000000000000001, 1030)
ON DUPLICATE KEY UPDATE balance = VALUES(balance);

INSERT INTO prediction_bean_log (id, user_id, prediction_id, change_amount, balance_after, log_type, remark, create_time)
VALUES (900000000000000400, 900000000000000001, NULL, 1000, 1000, 'INITIAL_GRANT', '演示账号初始本金 1,000 豆', '2026-10-22 00:00:00'),
       (900000000000000401, 900000000000000001, 900000000000000101, -50, 950, 'CHALLENGE_STAKE', '看台挑战投入 50 豆', '2026-10-22 02:30:00'),
       (900000000000000402, 900000000000000001, 900000000000000101, 80, 1030, 'CHALLENGE_REWARD', '方向命中，返还 50 豆并奖励 30 豆', '2026-10-22 05:00:00')
ON DUPLICATE KEY UPDATE change_amount = VALUES(change_amount),
                        balance_after = VALUES(balance_after);

INSERT INTO prediction_market (id, match_id, label, participant_count, percent, sort_order)
VALUES (900000000000000501, 575370, '两队都进球', 8322, 71, 1),
       (900000000000000502, 575370, '下一球：皇马', 8322, 71, 2)
ON DUPLICATE KEY UPDATE label = VALUES(label),
                        participant_count = VALUES(participant_count),
                        percent = VALUES(percent);

-- 未开赛预览场：真实球队 + 真实赛事，赛程日期用于本机演示预测面板。
-- 若不需要演示赛程，可删除 700001 及其预测数据。
INSERT INTO match_info
    (id, league_id, season, round_name, home_team_id, away_team_id, home_score, away_score,
     match_time, venue, match_status, important_level, has_report, status, remark)
VALUES (700001, 2001, '2026', '联赛阶段 第5轮', 5, 86, NULL, NULL,
        '2026-11-25 04:00:00', '安联球场', 'SCHEDULED', 1, 0, 'ACTIVE', '预测面板演示场：真实球队与赛事')
ON DUPLICATE KEY UPDATE match_time = VALUES(match_time),
                        match_status = VALUES(match_status),
                        home_team_id = VALUES(home_team_id),
                        away_team_id = VALUES(away_team_id),
                        venue = VALUES(venue),
                        remark = VALUES(remark);

INSERT INTO match_prediction_consensus (match_id, home_percent, draw_percent, away_percent, participant_count)
VALUES (700001, 44.00, 30.00, 26.00, 12860)
ON DUPLICATE KEY UPDATE home_percent = VALUES(home_percent),
                        draw_percent = VALUES(draw_percent),
                        away_percent = VALUES(away_percent),
                        participant_count = VALUES(participant_count);

INSERT INTO prediction_market (id, match_id, label, participant_count, percent, sort_order)
VALUES (900000000000000503, 700001, '两队都进球', 4210, 63, 1),
       (900000000000000504, 700001, '下一球：拜仁', 4210, 68, 2)
ON DUPLICATE KEY UPDATE label = VALUES(label),
                        participant_count = VALUES(participant_count),
                        percent = VALUES(percent);

-- ------------------------------------------------------------
-- 2026-10-22 真实比赛扩展样本
-- 全部取自 match_info 的真实赛果，用于演示不同方向命中/未命中场景。
-- 免费预测：stake = 0，不产生看台豆流水；575370 仍保留挑战流水示例。
-- ------------------------------------------------------------
INSERT INTO match_prediction_consensus
    (match_id, home_percent, draw_percent, away_percent, participant_count)
VALUES (575374, 62.00, 22.00, 16.00, 15320),
       (575368, 28.00, 26.00, 46.00, 21480),
       (575375, 58.00, 24.00, 18.00, 11760),
       (575363, 61.00, 23.00, 16.00, 24930)
ON DUPLICATE KEY UPDATE home_percent = VALUES(home_percent),
                        draw_percent = VALUES(draw_percent),
                        away_percent = VALUES(away_percent),
                        participant_count = VALUES(participant_count);

INSERT INTO match_prediction
    (id, match_id, user_id, home_probability, draw_probability, away_probability, predicted_outcome,
     insight_score, stake, challenge_result, reward_beans, status, submitted_at, settled_at, extra_json, remark)
VALUES
    (900000000000000102, 575374, 900000000000000001, 58, 24, 18, 'HOME',
     87, 0, 'HIT', 0, 'SETTLED', '2026-10-22 01:00:00', '2026-10-22 05:00:00',
     JSON_OBJECT('headline', '你的最高概率结果与赛果一致', 'highlights', JSON_ARRAY(
         '赛前你更看好维拉主场取胜', '维拉 3-1 获胜，方向命中', '本场概率校准得分 87')),
     '真实赛果：维拉 3-1 维京'),
    (900000000000000103, 575372, 900000000000000001, 55, 25, 20, 'HOME',
     55, 0, 'MISS', 0, 'SETTLED', '2026-10-22 01:00:00', '2026-10-22 05:00:00',
     JSON_OBJECT('headline', '方向未命中，但概率仍接近赛果', 'highlights', JSON_ARRAY(
         '你判断拜仁更可能获胜', '实际 2-2，平局概率为 25%', '这场提醒：足球结果存在波动')),
     '真实赛果：拜仁 2-2 阿森纳'),
    (900000000000000104, 575368, 900000000000000001, 20, 25, 55, 'AWAY',
     50, 0, 'MISS', 0, 'SETTLED', '2026-10-21 23:00:00', '2026-10-22 03:00:00',
     JSON_OBJECT('headline', '冷门场次：方向未命中', 'highlights', JSON_ARRAY(
         '你判断曼联客场取胜', '科莫 4-1 完成冷门', '概率校准得分 50，仍高于随机猜测')),
     '真实赛果：科莫 4-1 曼联'),
    (900000000000000105, 575367, 900000000000000001, 52, 24, 24, 'HOME',
     83, 0, 'HIT', 0, 'SETTLED', '2026-10-21 01:30:00', '2026-10-21 05:00:00',
     JSON_OBJECT('headline', '你的最高概率结果与赛果一致', 'highlights', JSON_ARRAY(
         '你赛前更看好巴黎', '巴黎 4-2 获胜，方向命中', '本场概率校准得分 83')),
     '真实赛果：巴黎 4-2 巴萨'),
    (900000000000000106, 575375, 900000000000000001, 55, 25, 20, 'HOME',
     50, 0, 'MISS', 0, 'SETTLED', '2026-10-22 01:00:00', '2026-10-22 05:00:00',
     JSON_OBJECT('headline', '主场热门翻车：方向未命中', 'highlights', JSON_ARRAY(
         '你判断里斯本主场取胜', 'LASK 3-2 客场取胜', '概率校准得分 50，说明仍需保留冷门空间')),
     '真实赛果：葡萄牙体育 2-3 LASK'),
    (900000000000000107, 575363, 900000000000000001, 60, 22, 18, 'HOME',
     88, 0, 'HIT', 0, 'SETTLED', '2026-10-21 01:30:00', '2026-10-21 05:00:00',
     JSON_OBJECT('headline', '你的最高概率结果与赛果一致', 'highlights', JSON_ARRAY(
         '你赛前更看好利物浦', '利物浦 4-3 获胜，方向命中', '本场概率校准得分 88')),
     '真实赛果：利物浦 4-3 比利亚雷亚尔')
ON DUPLICATE KEY UPDATE home_probability = VALUES(home_probability),
                        draw_probability = VALUES(draw_probability),
                        away_probability = VALUES(away_probability),
                        predicted_outcome = VALUES(predicted_outcome),
                        insight_score = VALUES(insight_score),
                        stake = VALUES(stake),
                        challenge_result = VALUES(challenge_result),
                        reward_beans = VALUES(reward_beans),
                        status = VALUES(status),
                        settled_at = VALUES(settled_at),
                        extra_json = VALUES(extra_json),
                        remark = VALUES(remark);

INSERT INTO prediction_trajectory
    (id, match_id, user_id, stage_label, home_percent, draw_percent, away_percent, sort_order, captured_at)
VALUES
    (900000000000000210, 575374, 900000000000000001, '赛前24小时', 52, 28, 20, 1, '2026-10-21 03:00:00'),
    (900000000000000211, 575374, 900000000000000001, '首发公布', 56, 26, 18, 2, '2026-10-22 02:00:00'),
    (900000000000000212, 575374, 900000000000000001, '阵容锁定', 58, 24, 18, 3, '2026-10-22 02:30:00'),
    (900000000000000213, 575372, 900000000000000001, '赛前24小时', 48, 28, 24, 1, '2026-10-21 03:00:00'),
    (900000000000000214, 575372, 900000000000000001, '首发公布', 52, 26, 22, 2, '2026-10-22 02:00:00'),
    (900000000000000215, 575372, 900000000000000001, '阵容锁定', 55, 25, 20, 3, '2026-10-22 02:30:00'),
    (900000000000000216, 575368, 900000000000000001, '赛前24小时', 24, 26, 50, 1, '2026-10-21 00:30:00'),
    (900000000000000217, 575368, 900000000000000001, '首发公布', 22, 25, 53, 2, '2026-10-21 23:30:00'),
    (900000000000000218, 575368, 900000000000000001, '阵容锁定', 20, 25, 55, 3, '2026-10-22 00:00:00'),
    (900000000000000219, 575367, 900000000000000001, '赛前24小时', 46, 28, 26, 1, '2026-10-20 03:00:00'),
    (900000000000000220, 575367, 900000000000000001, '首发公布', 50, 26, 24, 2, '2026-10-21 02:00:00'),
    (900000000000000221, 575367, 900000000000000001, '阵容锁定', 52, 24, 24, 3, '2026-10-21 02:30:00'),
    (900000000000000222, 575375, 900000000000000001, '赛前24小时', 50, 28, 22, 1, '2026-10-21 03:00:00'),
    (900000000000000223, 575375, 900000000000000001, '首发公布', 53, 26, 21, 2, '2026-10-22 02:00:00'),
    (900000000000000224, 575375, 900000000000000001, '阵容锁定', 55, 25, 20, 3, '2026-10-22 02:30:00'),
    (900000000000000225, 575363, 900000000000000001, '赛前24小时', 54, 26, 20, 1, '2026-10-20 03:00:00'),
    (900000000000000226, 575363, 900000000000000001, '首发公布', 58, 24, 18, 2, '2026-10-21 02:00:00'),
    (900000000000000227, 575363, 900000000000000001, '阵容锁定', 60, 22, 18, 3, '2026-10-21 02:30:00')
ON DUPLICATE KEY UPDATE home_percent = VALUES(home_percent),
                        draw_percent = VALUES(draw_percent),
                        away_percent = VALUES(away_percent),
                        stage_label = VALUES(stage_label),
                        sort_order = VALUES(sort_order),
                        captured_at = VALUES(captured_at);

INSERT INTO prediction_market (id, match_id, label, participant_count, percent, sort_order)
VALUES
    (900000000000000505, 575374, '两队都进球', 6120, 66, 1),
    (900000000000000506, 575374, '下一球：维拉', 6120, 58, 2),
    (900000000000000507, 575372, '两队都进球', 8860, 71, 1),
    (900000000000000508, 575372, '下一球：拜仁', 8860, 54, 2),
    (900000000000000509, 575368, '两队都进球', 7340, 64, 1),
    (900000000000000510, 575368, '下一球：科莫', 7340, 57, 2),
    (900000000000000511, 575367, '两队都进球', 10240, 78, 1),
    (900000000000000512, 575367, '下一球：巴黎', 10240, 62, 2),
    (900000000000000513, 575375, '两队都进球', 4980, 69, 1),
    (900000000000000514, 575375, '下一球：里斯本', 4980, 55, 2),
    (900000000000000515, 575363, '两队都进球', 11860, 74, 1),
    (900000000000000516, 575363, '下一球：利物浦', 11860, 61, 2)
ON DUPLICATE KEY UPDATE label = VALUES(label),
                        participant_count = VALUES(participant_count),
                        percent = VALUES(percent),
                        sort_order = VALUES(sort_order);
