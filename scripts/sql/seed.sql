USE south_stand;

SET NAMES utf8mb4;

-- Local development smoke accounts use password: password
-- These BCrypt hashes are only for reset-dev local seed data.
INSERT INTO sys_user (id, username, phone, password_hash, role_type, onboarding_completed, status)
VALUES
  (10001, 'admin', NULL, '$2a$10$5A.dJ/Qi0uBgG.o5Vp2mFubNFFjRd6U59788aQriHIRYWI5uZn7za', 'ADMIN', 1, 'ACTIVE'),
  (10002, 'test_user', '13900000001', '$2a$10$5A.dJ/Qi0uBgG.o5Vp2mFubNFFjRd6U59788aQriHIRYWI5uZn7za', 'USER', 1, 'ACTIVE');

INSERT INTO user_profile (id, user_id, nickname, main_team_id, team_follow_count, player_follow_count, status)
VALUES
  (11001, 10001, '南看台编辑部', NULL, 0, 0, 'ACTIVE'),
  (11002, 10002, '南看台老球迷', 30001, 1, 1, 'ACTIVE');

INSERT INTO user_onboarding (id, user_id, main_team_id, selected_team_ids, selected_player_ids, completed, completed_time)
VALUES
  (12002, 10002, 30001, JSON_ARRAY(30001), JSON_ARRAY(40001), 1, NOW());

INSERT INTO football_league (id, league_name, league_name_en, country, season, league_type, sort_order)
VALUES
  (20001, '英超', 'Premier League', 'England', '2026', 'LEAGUE', 1),
  (20002, '欧冠', 'UEFA Champions League', 'Europe', '2026', 'CUP', 2),
  (20003, '西甲', 'La Liga', 'Spain', '2026', 'LEAGUE', 3);

INSERT INTO football_team (id, team_name, team_name_en, short_name, logo_url, country, city, home_stadium, founded_year, coach_name, market_value, follower_count)
VALUES
  (30001, '巴塞罗那', 'FC Barcelona', '巴萨', '/uploads/team/barcelona.png', 'Spain', 'Barcelona', 'Camp Nou', 1899, '示例教练A', 'demo', 128),
  (30002, '皇家马德里', 'Real Madrid', '皇马', '/uploads/team/real-madrid.png', 'Spain', 'Madrid', 'Santiago Bernabeu', 1902, '示例教练B', 'demo', 126),
  (30003, '拜仁慕尼黑', 'FC Bayern Munich', '拜仁', '/uploads/team/bayern.png', 'Germany', 'Munich', 'Allianz Arena', 1900, '示例教练C', 'demo', 122),
  (30004, '曼城', 'Manchester City', '曼城', '/uploads/team/man-city.png', 'England', 'Manchester', 'Etihad Stadium', 1880, '示例教练D', 'demo', 118),
  (30005, '阿森纳', 'Arsenal', '阿森纳', '/uploads/team/arsenal.png', 'England', 'London', 'Emirates Stadium', 1886, '示例教练E', 'demo', 112),
  (30006, '利物浦', 'Liverpool', '利物浦', '/uploads/team/liverpool.png', 'England', 'Liverpool', 'Anfield', 1892, '示例教练F', 'demo', 109);

INSERT INTO football_player (id, player_name, player_name_en, avatar_url, nationality, shirt_number, position, birth_date, height_cm, market_value, follower_count)
VALUES
  (40001, '莱万多夫斯基', 'Robert Lewandowski', '/uploads/player/lewandowski.png', 'Poland', 9, 'FW', '1988-08-21', 185, 'demo', 88),
  (40002, '佩德里', 'Pedri', '/uploads/player/pedri.png', 'Spain', 8, 'MF', '2002-11-25', 174, 'demo', 86),
  (40003, '维尼修斯', 'Vinicius Junior', '/uploads/player/vinicius.png', 'Brazil', 7, 'FW', '2000-07-12', 176, 'demo', 84),
  (40004, '贝林厄姆', 'Jude Bellingham', '/uploads/player/bellingham.png', 'England', 5, 'MF', '2003-06-29', 186, 'demo', 83),
  (40005, '凯恩', 'Harry Kane', '/uploads/player/kane.png', 'England', 9, 'FW', '1993-07-28', 188, 'demo', 82),
  (40006, '穆西亚拉', 'Jamal Musiala', '/uploads/player/musiala.png', 'Germany', 10, 'MF', '2003-02-26', 184, 'demo', 80),
  (40007, '哈兰德', 'Erling Haaland', '/uploads/player/haaland.png', 'Norway', 9, 'FW', '2000-07-21', 194, 'demo', 90),
  (40008, '萨卡', 'Bukayo Saka', '/uploads/player/saka.png', 'England', 7, 'FW', '2001-09-05', 178, 'demo', 78),
  (40009, '萨拉赫', 'Mohamed Salah', '/uploads/player/salah.png', 'Egypt', 11, 'FW', '1992-06-15', 175, 'demo', 87),
  (40010, '阿利森', 'Alisson Becker', '/uploads/player/alisson.png', 'Brazil', 1, 'GK', '1992-10-02', 193, 'demo', 71);

INSERT INTO team_player (id, team_id, player_id, team_type, season, shirt_number, position)
VALUES
  (41001, 30001, 40001, 'CLUB', '2026', 9, 'FW'),
  (41002, 30001, 40002, 'CLUB', '2026', 8, 'MF'),
  (41003, 30002, 40003, 'CLUB', '2026', 7, 'FW'),
  (41004, 30002, 40004, 'CLUB', '2026', 5, 'MF'),
  (41005, 30003, 40005, 'CLUB', '2026', 9, 'FW'),
  (41006, 30003, 40006, 'CLUB', '2026', 10, 'MF'),
  (41007, 30004, 40007, 'CLUB', '2026', 9, 'FW'),
  (41008, 30005, 40008, 'CLUB', '2026', 7, 'FW'),
  (41009, 30006, 40009, 'CLUB', '2026', 11, 'FW'),
  (41010, 30006, 40010, 'CLUB', '2026', 1, 'GK');

INSERT INTO match_info (id, league_id, season, round_name, home_team_id, away_team_id, home_score, away_score, match_time, venue, match_status, important_level, has_report)
VALUES
  (50001, 20002, '2026', '小组赛', 30001, 30003, 2, 1, '2026-07-10 20:00:00', '示例球场一', 'FINISHED', 5, 1),
  (50002, 20001, '2026', '第 1 轮', 30004, 30005, NULL, NULL, '2026-07-12 21:00:00', '示例球场二', 'SCHEDULED', 3, 0);

INSERT INTO match_event (id, match_id, team_id, player_id, event_type, minute, score_after, description)
VALUES
  (51001, 50001, 30001, 40001, 'GOAL', 63, '2-1', '示例进球事件');

INSERT INTO content (id, content_type, content_format, card_type, title, summary, body, author_id, source_type, source_name, is_official, like_count, comment_count, publish_time)
VALUES
  (60001, 'REPORT', 'ARTICLE_FORMAT', 'CONTENT_CARD', '巴萨 2-1 拜仁示例战报', '示例战报摘要', '示例战报正文', 10001, 'ADMIN', '南看台编辑部', 1, 8, 1, '2026-07-10 23:00:00'),
  (60002, 'POST', 'POST_FORMAT', 'CONTENT_CARD', '这场比赛值得聊聊', '示例帖子摘要', '示例帖子正文', 10002, 'USER', NULL, 0, 3, 0, '2026-07-11 10:00:00');

INSERT INTO content_relation (id, content_id, relation_type, relation_id, confidence, source_type)
VALUES
  (61001, 60001, 'MATCH', 50001, 1.0000, 'MANUAL'),
  (61002, 60001, 'TEAM', 30001, 1.0000, 'MANUAL');

INSERT INTO content_media (id, content_id, media_type, media_url, sort_order)
VALUES
  (62001, 60001, 'IMAGE', '/uploads/demo/report-cover.jpg', 1);

INSERT INTO match_report (id, match_id, content_id, report_type)
VALUES
  (63001, 50001, 60001, 'REPORT');

INSERT INTO comment (id, target_type, target_id, parent_id, user_id, content_text, like_count, reply_count)
VALUES
  (70001, 'CONTENT', 60001, 0, 10002, '示例评论：这场比赛节奏很快。', 1, 0);

INSERT INTO follow_record (id, user_id, follow_type, target_id, is_main, status)
VALUES
  (80001, 10002, 'TEAM', 30001, 1, 'ACTIVE');

INSERT INTO like_record (id, user_id, target_type, target_id, status)
VALUES
  (90001, 10002, 'CONTENT', 60001, 'ACTIVE');

INSERT INTO favorite_record (id, user_id, target_type, target_id, status)
VALUES
  (91001, 10002, 'CONTENT', 60001, 'ACTIVE');
