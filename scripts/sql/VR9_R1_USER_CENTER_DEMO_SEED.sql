USE south_stand;
SET NAMES utf8mb4;
SET collation_connection='utf8mb4_unicode_ci';

-- VR9-R1 only: additive content, media, blocks and missing entity follows.
-- M1 user relations (18100000000090001..90020) are never modified.
DROP PROCEDURE IF EXISTS vr9_r1_user_center_assert_safe;
DELIMITER $$
CREATE PROCEDURE vr9_r1_user_center_assert_safe()
BEGIN
  IF (SELECT COUNT(*) FROM sys_user WHERE id=10002 AND username='test_user' AND status='ACTIVE' AND is_deleted=0) <> 1
     OR (SELECT COUNT(*) FROM sys_user WHERE id=11000000000000016 AND username='demo_user_16' AND status='ACTIVE' AND is_deleted=0) <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VR9-R1 conflict: target DEMO users are missing or changed';
  END IF;
  IF EXISTS (SELECT 1 FROM content WHERE id BETWEEN 16000000000000901 AND 16000000000000912
             AND (remark IS NULL OR remark <> 'VR9_R1_DEMO_CONTENT'))
     OR EXISTS (SELECT 1 FROM content_media WHERE id BETWEEN 16100000000000901 AND 16100000000000912
             AND (remark IS NULL OR remark NOT LIKE 'VR9-R1-media-%'))
     OR EXISTS (SELECT 1 FROM content_block WHERE id BETWEEN 16200000000000901 AND 16200000000000912
             AND (content_id NOT BETWEEN 16000000000000901 AND 16000000000000912))
     OR EXISTS (SELECT 1 FROM follow_record WHERE id BETWEEN 18100000000090101 AND 18100000000090104
             AND (remark IS NULL OR remark NOT LIKE 'VR9-R1-entity-%')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VR9-R1 conflict: reserved ID range contains non-R1 data';
  END IF;
  IF EXISTS (SELECT 1 FROM follow_record WHERE id BETWEEN 18100000000090001 AND 18100000000090020
             AND remark NOT LIKE 'VR9-M1-relation-%') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='VR9-R1 safety check failed: VR9-M1 relation ownership changed';
  END IF;
END$$
DELIMITER ;
CALL vr9_r1_user_center_assert_safe();
DROP PROCEDURE vr9_r1_user_center_assert_safe;

START TRANSACTION;

INSERT INTO content(
  id,content_type,content_format,card_type,title,summary,body,cover_url,author_id,
  source_type,source_name,is_official,view_count,like_count,comment_count,favorite_count,
  hot_score,publish_time,status,is_deleted,remark
)
VALUES
  (16000000000000901,'POST','POST_FORMAT','CONTENT_CARD','今晚的边路推进值得再看一遍','把比赛拆成几个阶段，边路推进会更清楚。','这是一条 VR9-R1 DEMO 正常态内容，用于验证用户主页连续内容流。','/demo/p1-media/cover-stadium.png',10002,'USER','VR9-R1 DEMO',0,36,8,1,2,218.00,'2026-09-23 09:20:00','PUBLISHED',0,'VR9_R1_DEMO_CONTENT'),
  (16000000000000902,'POST','POST_FORMAT','CONTENT_CARD','中场回收速度如何影响比赛节奏','从第二落点和回收站位看一场比赛。','真实 DEMO 内容，复用 P1 图片资源，不改变首页推荐排序。','/demo/p1-media/cover-training.png',10002,'USER','VR9-R1 DEMO',0,31,6,1,2,217.00,'2026-09-23 09:10:00','PUBLISHED',0,'VR9_R1_DEMO_CONTENT'),
  (16000000000000903,'ARTICLE','ARTICLE_FORMAT','CONTENT_CARD','一次提前移动带来的防守收益','门将站位和后防线高度之间的关系。','真实 DEMO 文章内容，用于验证本人发布页的双列卡片。','/demo/p1-media/cover-tactics.png',10002,'USER','VR9-R1 DEMO',0,29,5,0,1,216.00,'2026-09-23 09:00:00','PUBLISHED',0,'VR9_R1_DEMO_CONTENT'),
  (16000000000000904,'POST','POST_FORMAT','CONTENT_CARD','比赛最后十五分钟的节奏变化','比分之外，最后阶段的选择同样重要。','真实 DEMO 内容，保留可访问封面和作者关系。','/demo/p1-media/cover-stadium.png',10002,'USER','VR9-R1 DEMO',0,27,4,1,1,215.00,'2026-09-23 08:50:00','PUBLISHED',0,'VR9_R1_DEMO_CONTENT'),
  (16000000000000905,'POST','POST_FORMAT','CONTENT_CARD','如果你来排阵会保留哪组首发','从球员特点和比赛状态出发讨论。','真实 DEMO 内容，确保内容页继续向下延伸。','/demo/p1-media/cover-training.png',10002,'USER','VR9-R1 DEMO',0,24,3,0,1,214.00,'2026-09-23 08:40:00','PUBLISHED',0,'VR9_R1_DEMO_CONTENT'),
  (16000000000000906,'ARTICLE','ARTICLE_FORMAT','CONTENT_CARD','赛后笔记：把现场感留在数据之外','数据与现场感可以互相补充。','真实 DEMO 文章内容，封面复用既有 P1 媒体。','/demo/p1-media/cover-tactics.png',10002,'USER','VR9-R1 DEMO',0,22,3,0,1,213.00,'2026-09-23 08:30:00','PUBLISHED',0,'VR9_R1_DEMO_CONTENT'),
  (16000000000000907,'POST','POST_FORMAT','CONTENT_CARD','公开主页的第一条复盘','关注推进路线，也关注回防距离。','真实 DEMO 内容，用于公开用户主页视觉密度。','/demo/p1-media/cover-stadium.png',11000000000000016,'USER','VR9-R1 DEMO',0,42,9,1,2,212.00,'2026-09-23 08:20:00','PUBLISHED',0,'VR9_R1_DEMO_CONTENT'),
  (16000000000000908,'POST','POST_FORMAT','CONTENT_CARD','边路空间打开之后发生了什么','从球员站位看进攻宽度。','真实 DEMO 内容，用于公开主页双列流。','/demo/p1-media/cover-training.png',11000000000000016,'USER','VR9-R1 DEMO',0,39,8,1,2,211.00,'2026-09-23 08:10:00','PUBLISHED',0,'VR9_R1_DEMO_CONTENT'),
  (16000000000000909,'ARTICLE','ARTICLE_FORMAT','CONTENT_CARD','一场比赛里的三个转折点','把事件、阵容和统计放回比赛过程。','真实 DEMO 文章，保持现有内容接口契约。','/demo/p1-media/cover-tactics.png',11000000000000016,'USER','VR9-R1 DEMO',0,37,7,0,1,210.00,'2026-09-23 08:00:00','PUBLISHED',0,'VR9_R1_DEMO_CONTENT'),
  (16000000000000910,'POST','POST_FORMAT','CONTENT_CARD','门将站位与后卫线的距离','提前半步也可能改变防守选择。','真实 DEMO 内容，封面来自 P1 媒体。','/demo/p1-media/cover-stadium.png',11000000000000016,'USER','VR9-R1 DEMO',0,35,6,0,1,209.00,'2026-09-23 07:50:00','PUBLISHED',0,'VR9_R1_DEMO_CONTENT'),
  (16000000000000911,'POST','POST_FORMAT','CONTENT_CARD','赛季数据应该怎样放回现场','单场表现需要放在赛季背景里理解。','真实 DEMO 内容，避免公开主页首屏提前结束。','/demo/p1-media/cover-training.png',11000000000000016,'USER','VR9-R1 DEMO',0,32,5,0,1,208.00,'2026-09-23 07:40:00','PUBLISHED',0,'VR9_R1_DEMO_CONTENT'),
  (16000000000000912,'ARTICLE','ARTICLE_FORMAT','CONTENT_CARD','复盘一场比赛的正确顺序','先看过程，再回到数据和结果。','真实 DEMO 文章内容，使用已验收图片。','/demo/p1-media/cover-tactics.png',11000000000000016,'USER','VR9-R1 DEMO',0,30,4,0,1,207.00,'2026-09-23 07:30:00','PUBLISHED',0,'VR9_R1_DEMO_CONTENT')
ON DUPLICATE KEY UPDATE id=id;

INSERT INTO content_media(id,content_id,media_type,media_url,thumbnail_url,width,height,sort_order,status,is_deleted,remark)
VALUES
  (16100000000000901,16000000000000901,'IMAGE','/demo/p1-media/cover-stadium.png','/demo/p1-media/cover-stadium.png',1600,900,0,'ACTIVE',0,'VR9-R1-media-901'),
  (16100000000000902,16000000000000902,'IMAGE','/demo/p1-media/cover-training.png','/demo/p1-media/cover-training.png',1600,900,0,'ACTIVE',0,'VR9-R1-media-902'),
  (16100000000000903,16000000000000903,'IMAGE','/demo/p1-media/cover-tactics.png','/demo/p1-media/cover-tactics.png',1600,900,0,'ACTIVE',0,'VR9-R1-media-903'),
  (16100000000000904,16000000000000904,'IMAGE','/demo/p1-media/cover-stadium.png','/demo/p1-media/cover-stadium.png',1600,900,0,'ACTIVE',0,'VR9-R1-media-904'),
  (16100000000000905,16000000000000905,'IMAGE','/demo/p1-media/cover-training.png','/demo/p1-media/cover-training.png',1600,900,0,'ACTIVE',0,'VR9-R1-media-905'),
  (16100000000000906,16000000000000906,'IMAGE','/demo/p1-media/cover-tactics.png','/demo/p1-media/cover-tactics.png',1600,900,0,'ACTIVE',0,'VR9-R1-media-906'),
  (16100000000000907,16000000000000907,'IMAGE','/demo/p1-media/cover-stadium.png','/demo/p1-media/cover-stadium.png',1600,900,0,'ACTIVE',0,'VR9-R1-media-907'),
  (16100000000000908,16000000000000908,'IMAGE','/demo/p1-media/cover-training.png','/demo/p1-media/cover-training.png',1600,900,0,'ACTIVE',0,'VR9-R1-media-908'),
  (16100000000000909,16000000000000909,'IMAGE','/demo/p1-media/cover-tactics.png','/demo/p1-media/cover-tactics.png',1600,900,0,'ACTIVE',0,'VR9-R1-media-909'),
  (16100000000000910,16000000000000910,'IMAGE','/demo/p1-media/cover-stadium.png','/demo/p1-media/cover-stadium.png',1600,900,0,'ACTIVE',0,'VR9-R1-media-910'),
  (16100000000000911,16000000000000911,'IMAGE','/demo/p1-media/cover-training.png','/demo/p1-media/cover-training.png',1600,900,0,'ACTIVE',0,'VR9-R1-media-911'),
  (16100000000000912,16000000000000912,'IMAGE','/demo/p1-media/cover-tactics.png','/demo/p1-media/cover-tactics.png',1600,900,0,'ACTIVE',0,'VR9-R1-media-912')
ON DUPLICATE KEY UPDATE id=id;

INSERT INTO content_block(id,content_id,block_type,text_content,media_file_id,media_url,embed_url,sort_order,status,is_deleted)
VALUES
  (16200000000000901,16000000000000901,'TEXT','VR9-R1 DEMO 内容块：边路推进和回收速度。',NULL,NULL,NULL,0,'ACTIVE',0),
  (16200000000000902,16000000000000902,'TEXT','VR9-R1 DEMO 内容块：中场站位和比赛节奏。',NULL,NULL,NULL,0,'ACTIVE',0),
  (16200000000000903,16000000000000903,'TEXT','VR9-R1 DEMO 内容块：门将提前移动的防守收益。',NULL,NULL,NULL,0,'ACTIVE',0),
  (16200000000000904,16000000000000904,'TEXT','VR9-R1 DEMO 内容块：最后十五分钟的比赛选择。',NULL,NULL,NULL,0,'ACTIVE',0),
  (16200000000000905,16000000000000905,'TEXT','VR9-R1 DEMO 内容块：阵容与球员状态。',NULL,NULL,NULL,0,'ACTIVE',0),
  (16200000000000906,16000000000000906,'TEXT','VR9-R1 DEMO 内容块：现场感和数据复盘。',NULL,NULL,NULL,0,'ACTIVE',0),
  (16200000000000907,16000000000000907,'TEXT','VR9-R1 DEMO 内容块：公开主页比赛复盘。',NULL,NULL,NULL,0,'ACTIVE',0),
  (16200000000000908,16000000000000908,'TEXT','VR9-R1 DEMO 内容块：边路空间和进攻宽度。',NULL,NULL,NULL,0,'ACTIVE',0),
  (16200000000000909,16000000000000909,'TEXT','VR9-R1 DEMO 内容块：一场比赛的三个转折点。',NULL,NULL,NULL,0,'ACTIVE',0),
  (16200000000000910,16000000000000910,'TEXT','VR9-R1 DEMO 内容块：门将和后防线距离。',NULL,NULL,NULL,0,'ACTIVE',0),
  (16200000000000911,16000000000000911,'TEXT','VR9-R1 DEMO 内容块：赛季数据与现场。',NULL,NULL,NULL,0,'ACTIVE',0),
  (16200000000000912,16000000000000912,'TEXT','VR9-R1 DEMO 内容块：比赛复盘顺序。',NULL,NULL,NULL,0,'ACTIVE',0)
ON DUPLICATE KEY UPDATE id=id;

INSERT INTO follow_record(id,user_id,follow_type,target_id,is_main,status,is_deleted,remark)
SELECT 18100000000090101,10002,'TEAM',30002,0,'ACTIVE',0,'VR9-R1-entity-team-30002'
WHERE NOT EXISTS (SELECT 1 FROM follow_record WHERE user_id=10002 AND follow_type='TEAM' AND target_id=30002);
INSERT INTO follow_record(id,user_id,follow_type,target_id,is_main,status,is_deleted,remark)
SELECT 18100000000090102,10002,'TEAM',30003,0,'ACTIVE',0,'VR9-R1-entity-team-30003'
WHERE NOT EXISTS (SELECT 1 FROM follow_record WHERE user_id=10002 AND follow_type='TEAM' AND target_id=30003);
INSERT INTO follow_record(id,user_id,follow_type,target_id,is_main,status,is_deleted,remark)
SELECT 18100000000090103,10002,'PLAYER',40002,0,'ACTIVE',0,'VR9-R1-entity-player-40002'
WHERE NOT EXISTS (SELECT 1 FROM follow_record WHERE user_id=10002 AND follow_type='PLAYER' AND target_id=40002);
INSERT INTO follow_record(id,user_id,follow_type,target_id,is_main,status,is_deleted,remark)
SELECT 18100000000090104,10002,'PLAYER',40003,0,'ACTIVE',0,'VR9-R1-entity-player-40003'
WHERE NOT EXISTS (SELECT 1 FROM follow_record WHERE user_id=10002 AND follow_type='PLAYER' AND target_id=40003);

UPDATE user_profile p
SET p.post_count=(SELECT COUNT(*) FROM content c WHERE c.author_id=p.user_id AND c.status='PUBLISHED' AND c.is_deleted=0),
    p.team_follow_count=(SELECT COUNT(*) FROM follow_record f WHERE f.user_id=p.user_id AND f.follow_type='TEAM' AND f.status='ACTIVE' AND f.is_deleted=0),
    p.player_follow_count=(SELECT COUNT(*) FROM follow_record f WHERE f.user_id=p.user_id AND f.follow_type='PLAYER' AND f.status='ACTIVE' AND f.is_deleted=0)
WHERE p.user_id IN (10002,11000000000000016) AND p.status='ACTIVE' AND p.is_deleted=0;

COMMIT;

SELECT 'VR9_R1_contents_me' metric,COUNT(*) value FROM content WHERE author_id=10002 AND status='PUBLISHED' AND is_deleted=0;
SELECT 'VR9_R1_contents_public' metric,COUNT(*) value FROM content WHERE author_id=11000000000000016 AND status='PUBLISHED' AND is_deleted=0;
SELECT 'VR9_R1_media' metric,COUNT(*) value FROM content_media WHERE id BETWEEN 16100000000000901 AND 16100000000000912 AND remark LIKE 'VR9-R1-media-%';
SELECT 'VR9_R1_teams' metric,COUNT(*) value FROM follow_record WHERE user_id=10002 AND follow_type='TEAM' AND status='ACTIVE' AND is_deleted=0;
SELECT 'VR9_R1_players' metric,COUNT(*) value FROM follow_record WHERE user_id=10002 AND follow_type='PLAYER' AND status='ACTIVE' AND is_deleted=0;
