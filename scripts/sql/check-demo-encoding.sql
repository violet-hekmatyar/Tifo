USE south_stand;
SET NAMES utf8mb4;

SELECT DATABASE() AS database_name;
SELECT @@character_set_client AS character_set_client,
       @@character_set_connection AS character_set_connection,
       @@character_set_results AS character_set_results,
       @@character_set_database AS character_set_database,
       @@collation_connection AS collation_connection,
       @@collation_database AS collation_database;

SELECT check_name, total_count, cjk_count, multibyte_count, question_run_count, all_question_count,
       IF(total_count > 0
          AND cjk_count = total_count
          AND multibyte_count = total_count
          AND question_run_count = 0
          AND all_question_count = 0, 0, 1) AS anomaly_count
FROM (
  SELECT 'league_encoding' AS check_name, COUNT(*) AS total_count,
         SUM(league_name REGEXP '[一-龥]') AS cjk_count,
         SUM(CHAR_LENGTH(league_name) < OCTET_LENGTH(league_name)) AS multibyte_count,
         SUM(league_name LIKE '%??%') AS question_run_count,
         SUM(league_name REGEXP '^\\?+$') AS all_question_count
  FROM football_league WHERE id >= 12000000000000001 AND id < 13000000000000001
  UNION ALL
  SELECT 'team_encoding', COUNT(*), SUM(team_name REGEXP '[一-龥]'),
         SUM(CHAR_LENGTH(team_name) < OCTET_LENGTH(team_name)),
         SUM(team_name LIKE '%??%'), SUM(team_name REGEXP '^\\?+$')
  FROM football_team WHERE id >= 13000000000000001 AND id < 14000000000000001
  UNION ALL
  SELECT 'player_encoding', COUNT(*), SUM(player_name REGEXP '[一-龥]'),
         SUM(CHAR_LENGTH(player_name) < OCTET_LENGTH(player_name)),
         SUM(player_name LIKE '%??%'), SUM(player_name REGEXP '^\\?+$')
  FROM football_player WHERE id >= 14000000000000001 AND id < 15000000000000001
  UNION ALL
  SELECT 'profile_encoding', COUNT(*), SUM(nickname REGEXP '[一-龥]'),
         SUM(CHAR_LENGTH(nickname) < OCTET_LENGTH(nickname)),
         SUM(nickname LIKE '%??%'), SUM(nickname REGEXP '^\\?+$')
  FROM user_profile WHERE user_id >= 11000000000000001 AND user_id < 12000000000000001
  UNION ALL
  SELECT 'content_title_encoding', COUNT(*), SUM(title REGEXP '[一-龥]'),
         SUM(CHAR_LENGTH(title) < OCTET_LENGTH(title)),
         SUM(title LIKE '%??%'), SUM(title REGEXP '^\\?+$')
  FROM content WHERE id >= 16000000000000001 AND id < 17000000000000001
  UNION ALL
  SELECT 'content_body_encoding', COUNT(*), SUM(body REGEXP '[一-龥]'),
         SUM(CHAR_LENGTH(body) < OCTET_LENGTH(body)),
         SUM(body LIKE '%??%'), SUM(body REGEXP '^\\?+$')
  FROM content WHERE id >= 16000000000000001 AND id < 17000000000000001
  UNION ALL
  SELECT 'comment_encoding', COUNT(*), SUM(content_text REGEXP '[一-龥]'),
         SUM(CHAR_LENGTH(content_text) < OCTET_LENGTH(content_text)),
         SUM(content_text LIKE '%??%'), SUM(content_text REGEXP '^\\?+$')
  FROM comment WHERE id >= 17000000000000001 AND id < 18000000000000001
) encoding_checks
ORDER BY check_name;

SELECT 'team_hex_sample' AS sample_name, id, team_name AS sample_value, HEX(team_name) AS sample_hex
FROM football_team WHERE id >= 13000000000000001 AND id < 14000000000000001 ORDER BY id LIMIT 1;
SELECT 'content_hex_sample' AS sample_name, id, title AS sample_value, HEX(title) AS sample_hex
FROM content WHERE id >= 16000000000000001 AND id < 17000000000000001 ORDER BY id LIMIT 1;
