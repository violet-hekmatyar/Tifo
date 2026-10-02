USE south_stand;
SET NAMES utf8mb4;
SET collation_connection='utf8mb4_unicode_ci';

DROP PROCEDURE IF EXISTS vr7_m1_assert_safe;
DELIMITER $$
CREATE PROCEDURE vr7_m1_assert_safe()
BEGIN
  IF EXISTS (SELECT 1 FROM content_publish_subject
             WHERE id>=16500000000000701 AND id<16500000000000717
               AND (remark IS NULL OR remark NOT LIKE 'VR7-M1-subject-%'))
     OR EXISTS (SELECT 1 FROM content_relation
             WHERE id>=16300000000000701 AND id<16300000000000713
               AND (remark IS NULL OR remark NOT LIKE 'VR7-M1-relation-%')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VR7-M1 conflict: reserved ID range is owned by non-VR7 data';
  END IF;
  IF EXISTS (SELECT 1 FROM content_publish_subject
             WHERE subject_type IN ('TOPIC','HOT_EVENT')
               AND name IN ('英超焦点','转会市场','国家队赛事','欧冠争冠讨论','球员状态观察','赛后战术复盘',
                            '利雅得胜利宣布中国行延期','欧冠八强抽签即将开始','新赛季转会窗口开启','金球奖候选名单公布','国家队新一期名单','赛季最佳进球票选')
               AND (id NOT BETWEEN 16500000000000701 AND 16500000000000716
                    OR remark NOT LIKE 'VR7-M1-subject-%')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VR7-M1 conflict: natural key is owned by non-VR7 data';
  END IF;
END$$
DELIMITER ;
CALL vr7_m1_assert_safe();
DROP PROCEDURE vr7_m1_assert_safe;

START TRANSACTION;
INSERT INTO content_publish_subject
  (id,subject_type,name,summary,cover_url,hot_score,sort_order,status,is_deleted,remark)
VALUES
  (16500000000000701,'TOPIC','英超焦点','英超比赛、球队和球员的持续讨论',NULL,98.00,1,'ACTIVE',0,'VR7-M1-subject-topic-701'),
  (16500000000000702,'TOPIC','转会市场','关注夏窗与冬窗的真实转会动态',NULL,96.00,2,'ACTIVE',0,'VR7-M1-subject-topic-702'),
  (16500000000000703,'TOPIC','国家队赛事','国家队比赛和阵容变化',NULL,94.00,3,'ACTIVE',0,'VR7-M1-subject-topic-703'),
  (16500000000000704,'TOPIC','欧冠争冠讨论','围绕欧冠淘汰赛的赛前与赛后分析',NULL,92.00,4,'ACTIVE',0,'VR7-M1-subject-topic-704'),
  (16500000000000705,'TOPIC','球员状态观察','从比赛表现追踪球员状态',NULL,90.00,5,'ACTIVE',0,'VR7-M1-subject-topic-705'),
  (16500000000000706,'TOPIC','赛后战术复盘','用比赛数据还原战术选择',NULL,88.00,6,'ACTIVE',0,'VR7-M1-subject-topic-706'),
  (16500000000000711,'HOT_EVENT','利雅得胜利宣布中国行延期','利雅得胜利召开新闻发布会宣布中国行延期，具体时间待定','/demo/p1-media/player-flagship.png',99.00,1,'ACTIVE',0,'VR7-M1-subject-hot-event-711'),
  (16500000000000712,'HOT_EVENT','欧冠八强抽签即将开始','欧冠八强抽签进入倒计时，赛程和对阵即将公布','/demo/p1-media/cover-stadium.png',97.00,2,'ACTIVE',0,'VR7-M1-subject-hot-event-712'),
  (16500000000000713,'HOT_EVENT','新赛季转会窗口开启','新赛季转会窗口开启，多支球队已经完成首笔签约','/demo/p1-media/cover-training.png',95.00,3,'ACTIVE',0,'VR7-M1-subject-hot-event-713'),
  (16500000000000714,'HOT_EVENT','金球奖候选名单公布','年度金球奖候选名单公布，球员竞争进入最后阶段','/demo/p1-media/player-flagship.png',93.00,4,'ACTIVE',0,'VR7-M1-subject-hot-event-714'),
  (16500000000000715,'HOT_EVENT','国家队新一期名单','国家队公布新一期集训名单，备战即将开始','/demo/p1-media/cover-tactics.png',91.00,5,'ACTIVE',0,'VR7-M1-subject-hot-event-715'),
  (16500000000000716,'HOT_EVENT','赛季最佳进球票选','赛季最佳进球开启票选，欢迎分享你的选择','/demo/p1-media/cover-stadium.png',89.00,6,'ACTIVE',0,'VR7-M1-subject-hot-event-716')
ON DUPLICATE KEY UPDATE id=id;

INSERT INTO content_relation
  (id,content_id,relation_type,relation_id,confidence,source_type,status,is_deleted,remark)
VALUES
  (16300000000000701,16000000000000201,'TOPIC',16500000000000701,1.0000,'VR7','ACTIVE',0,'VR7-M1-relation-701'),
  (16300000000000702,16000000000000202,'TOPIC',16500000000000702,1.0000,'VR7','ACTIVE',0,'VR7-M1-relation-702'),
  (16300000000000703,16000000000000203,'TOPIC',16500000000000703,1.0000,'VR7','ACTIVE',0,'VR7-M1-relation-703'),
  (16300000000000704,16000000000000204,'TOPIC',16500000000000704,1.0000,'VR7','ACTIVE',0,'VR7-M1-relation-704'),
  (16300000000000705,16000000000000205,'TOPIC',16500000000000705,1.0000,'VR7','ACTIVE',0,'VR7-M1-relation-705'),
  (16300000000000706,16000000000000206,'TOPIC',16500000000000706,1.0000,'VR7','ACTIVE',0,'VR7-M1-relation-706'),
  (16300000000000707,16000000000000207,'HOT_EVENT',16500000000000711,1.0000,'VR7','ACTIVE',0,'VR7-M1-relation-707'),
  (16300000000000708,16000000000000208,'HOT_EVENT',16500000000000712,1.0000,'VR7','ACTIVE',0,'VR7-M1-relation-708'),
  (16300000000000709,16000000000000209,'HOT_EVENT',16500000000000713,1.0000,'VR7','ACTIVE',0,'VR7-M1-relation-709'),
  (16300000000000710,16000000000000210,'HOT_EVENT',16500000000000714,1.0000,'VR7','ACTIVE',0,'VR7-M1-relation-710'),
  (16300000000000711,16000000000000201,'HOT_EVENT',16500000000000715,1.0000,'VR7','ACTIVE',0,'VR7-M1-relation-711'),
  (16300000000000712,16000000000000202,'HOT_EVENT',16500000000000716,1.0000,'VR7','ACTIVE',0,'VR7-M1-relation-712')
ON DUPLICATE KEY UPDATE id=id;
COMMIT;

SELECT 'VR7_M1_subjects' metric,COUNT(*) value FROM content_publish_subject WHERE id>=16500000000000701 AND id<16500000000000717 AND remark LIKE 'VR7-M1-subject-%';
SELECT 'VR7_M1_relations' metric,COUNT(*) value FROM content_relation WHERE id>=16300000000000701 AND id<16300000000000713 AND remark LIKE 'VR7-M1-relation-%';
