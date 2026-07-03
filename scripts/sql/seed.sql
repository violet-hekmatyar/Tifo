USE south_stand;

SET NAMES utf8mb4;

-- Local development smoke accounts use password: password
-- These BCrypt hashes are only for reset-dev local seed data.
INSERT INTO sys_user (id, username, phone, password_hash, role_type, onboarding_completed, status)
VALUES
  (10001, 'admin', NULL, '$2a$10$5A.dJ/Qi0uBgG.o5Vp2mFubNFFjRd6U59788aQriHIRYWI5uZn7za', 'ADMIN', 1, 'ACTIVE'),
  (10002, 'test_user', '13900000001', '$2a$10$5A.dJ/Qi0uBgG.o5Vp2mFubNFFjRd6U59788aQriHIRYWI5uZn7za', 'USER', 1, 'ACTIVE');

INSERT INTO user_profile (id, user_id, nickname, avatar_url, main_team_id, team_follow_count, player_follow_count, post_count, status)
VALUES
  (11001, 10001, 'South Stand Editorial', '/uploads/avatar/admin.png', NULL, 0, 0, 0, 'ACTIVE'),
  (11002, 10002, 'Demo Fan', '/uploads/avatar/demo-fan.png', 30001, 1, 1, 1, 'ACTIVE');

INSERT INTO user_onboarding (id, user_id, main_team_id, selected_team_ids, selected_player_ids, completed, completed_time)
VALUES
  (12002, 10002, 30001, JSON_ARRAY(30001), JSON_ARRAY(40001), 1, NOW());

INSERT INTO football_league (id, league_name, league_name_en, country, season, league_type, sort_order)
VALUES
  (20001, 'Premier League', 'Premier League', 'England', '2026', 'LEAGUE', 1),
  (20002, 'UEFA Champions League', 'UEFA Champions League', 'Europe', '2026', 'CUP', 2),
  (20003, 'La Liga', 'La Liga', 'Spain', '2026', 'LEAGUE', 3);

INSERT INTO football_team (id, team_name, team_name_en, short_name, logo_url, country, city, home_stadium, founded_year, coach_name, market_value, follower_count)
VALUES
  (30001, 'Barcelona', 'FC Barcelona', 'Barca', '/uploads/team/barcelona.png', 'Spain', 'Barcelona', 'Camp Nou', 1899, 'Demo Coach A', 'demo', 128),
  (30002, 'Real Madrid', 'Real Madrid', 'Real', '/uploads/team/real-madrid.png', 'Spain', 'Madrid', 'Santiago Bernabeu', 1902, 'Demo Coach B', 'demo', 126),
  (30003, 'Bayern Munich', 'FC Bayern Munich', 'Bayern', '/uploads/team/bayern.png', 'Germany', 'Munich', 'Allianz Arena', 1900, 'Demo Coach C', 'demo', 122),
  (30004, 'Manchester City', 'Manchester City', 'Man City', '/uploads/team/man-city.png', 'England', 'Manchester', 'Etihad Stadium', 1880, 'Demo Coach D', 'demo', 118),
  (30005, 'Arsenal', 'Arsenal', 'Arsenal', '/uploads/team/arsenal.png', 'England', 'London', 'Emirates Stadium', 1886, 'Demo Coach E', 'demo', 112),
  (30006, 'Liverpool', 'Liverpool', 'Liverpool', '/uploads/team/liverpool.png', 'England', 'Liverpool', 'Anfield', 1892, 'Demo Coach F', 'demo', 109);

INSERT INTO football_player (id, player_name, player_name_en, avatar_url, nationality, shirt_number, position, birth_date, height_cm, market_value, follower_count)
VALUES
  (40001, 'Robert Lewandowski', 'Robert Lewandowski', '/uploads/player/lewandowski.png', 'Poland', 9, 'FW', '1988-08-21', 185, 'demo', 88),
  (40002, 'Pedri', 'Pedri', '/uploads/player/pedri.png', 'Spain', 8, 'MF', '2002-11-25', 174, 'demo', 86),
  (40003, 'Vinicius Junior', 'Vinicius Junior', '/uploads/player/vinicius.png', 'Brazil', 7, 'FW', '2000-07-12', 176, 'demo', 84),
  (40004, 'Jude Bellingham', 'Jude Bellingham', '/uploads/player/bellingham.png', 'England', 5, 'MF', '2003-06-29', 186, 'demo', 83),
  (40005, 'Harry Kane', 'Harry Kane', '/uploads/player/kane.png', 'England', 9, 'FW', '1993-07-28', 188, 'demo', 82),
  (40006, 'Jamal Musiala', 'Jamal Musiala', '/uploads/player/musiala.png', 'Germany', 10, 'MF', '2003-02-26', 184, 'demo', 80),
  (40007, 'Erling Haaland', 'Erling Haaland', '/uploads/player/haaland.png', 'Norway', 9, 'FW', '2000-07-21', 194, 'demo', 90),
  (40008, 'Bukayo Saka', 'Bukayo Saka', '/uploads/player/saka.png', 'England', 7, 'FW', '2001-09-05', 178, 'demo', 78),
  (40009, 'Mohamed Salah', 'Mohamed Salah', '/uploads/player/salah.png', 'Egypt', 11, 'FW', '1992-06-15', 175, 'demo', 87),
  (40010, 'Alisson Becker', 'Alisson Becker', '/uploads/player/alisson.png', 'Brazil', 1, 'GK', '1992-10-02', 193, 'demo', 71);

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
  (50001, 20002, '2026', 'Group A', 30001, 30003, 2, 1, '2026-07-10 20:00:00', 'Demo Stadium One', 'FINISHED', 5, 1),
  (50002, 20001, '2026', 'Round 1', 30004, 30005, NULL, NULL, '2026-07-12 21:00:00', 'Demo Stadium Two', 'SCHEDULED', 3, 0);

INSERT INTO match_event (id, match_id, team_id, player_id, event_type, minute, score_after, description)
VALUES
  (51001, 50001, 30001, 40001, 'GOAL', 63, '2-1', 'Demo goal event');

INSERT INTO content (id, content_type, content_format, card_type, title, summary, body, cover_url, author_id, source_type, source_name, is_official, view_count, like_count, comment_count, favorite_count, hot_score, publish_time, status)
VALUES
  (20001, 'NEWS', 'ARTICLE_FORMAT', 'CONTENT_CARD', 'Barcelona training update', 'Barcelona completed an open training session before the next match.', 'Barcelona focused on wing progression and transition defense in the open session.', '/uploads/content/demo-news-cover.jpg', 10001, 'ADMIN', 'South Stand Editorial', 1, 120, 10, 3, 2, 98.50, '2026-07-03 09:00:00', 'PUBLISHED'),
  (20002, 'POST', 'POST_FORMAT', 'CONTENT_CARD', 'The right side looked sharp today', 'A demo fan post about Barcelona wing play.', 'I think the key was creating space on the right side and attacking the half-space early.', '/uploads/content/demo-post-1.jpg', 10002, 'USER', NULL, 0, 45, 3, 0, 1, 31.00, '2026-07-03 10:00:00', 'PUBLISHED'),
  (20003, 'ARTICLE', 'ARTICLE_FORMAT', 'CONTENT_CARD', 'How high pressing changed the rhythm', 'A short tactical article for smoke testing.', 'The pressing trigger came from the forward line and forced rushed passes into midfield.', '/uploads/content/demo-article-cover.jpg', 10001, 'ADMIN', 'South Stand Editorial', 1, 78, 6, 0, 1, 65.00, '2026-07-03 11:00:00', 'PUBLISHED'),
  (20004, 'REPORT', 'ARTICLE_FORMAT', 'CONTENT_CARD', 'Barcelona 2-1 Bayern demo report', 'A demo match report linked to match 50001.', 'Barcelona won the demo match 2-1 with a late spell of pressure after the 60th minute.', '/uploads/content/demo-report-cover.jpg', 10001, 'ADMIN', 'South Stand Editorial', 1, 160, 8, 0, 2, 120.00, '2026-07-03 12:00:00', 'PUBLISHED');

INSERT INTO content_media (id, content_id, media_type, media_url, thumbnail_url, width, height, sort_order)
VALUES
  (62001, 20001, 'IMAGE', '/uploads/content/demo-news-1.jpg', '/uploads/content/demo-news-1-thumb.jpg', 1200, 800, 1),
  (62002, 20003, 'IMAGE', '/uploads/content/demo-article-1.jpg', '/uploads/content/demo-article-1-thumb.jpg', 1200, 800, 1),
  (62003, 20004, 'IMAGE', '/uploads/content/demo-report-1.jpg', '/uploads/content/demo-report-1-thumb.jpg', 1200, 800, 1);

INSERT INTO content_relation (id, content_id, relation_type, relation_id, confidence, source_type)
VALUES
  (61001, 20001, 'TEAM', 30001, 1.0000, 'MANUAL'),
  (61002, 20002, 'PLAYER', 40001, 1.0000, 'MANUAL'),
  (61003, 20004, 'MATCH', 50001, 1.0000, 'MANUAL'),
  (61004, 20004, 'TEAM', 30003, 1.0000, 'MANUAL');

INSERT INTO match_report (id, match_id, content_id, report_type)
VALUES
  (63001, 50001, 20004, 'REPORT');

INSERT INTO comment (id, target_type, target_id, parent_id, user_id, content_text, like_count, reply_count, hot_score, create_time)
VALUES
  (60001, 'CONTENT', 20001, 0, 10002, 'This piece has a useful rhythm note.', 2, 1, 6.00, '2026-07-03 09:10:00'),
  (60002, 'CONTENT', 20001, 0, 10001, 'The team relation is useful for the app demo.', 1, 0, 2.00, '2026-07-03 09:20:00'),
  (60003, 'CONTENT', 20001, 60001, 10001, 'Agreed, the right-side build-up is the key.', 1, 0, 1.00, '2026-07-03 09:30:00');

INSERT INTO follow_record (id, user_id, follow_type, target_id, is_main, status)
VALUES
  (80001, 10002, 'TEAM', 30001, 1, 'ACTIVE');

INSERT INTO like_record (id, user_id, target_type, target_id, status)
VALUES
  (90001, 10002, 'CONTENT', 20001, 'ACTIVE'),
  (90002, 10002, 'COMMENT', 60001, 'ACTIVE');

INSERT INTO favorite_record (id, user_id, target_type, target_id, status)
VALUES
  (91001, 10002, 'CONTENT', 20001, 'ACTIVE');
