USE south_stand;
SET NAMES utf8mb4;

-- Emit only identifiers and SHA-256 fingerprints. Sensitive values never leave MySQL.
SELECT table_name, row_id, fingerprint FROM (
  SELECT 'sys_user' table_name,CAST(id AS CHAR) row_id,SHA2(CONCAT_WS('|',username,IFNULL(phone,''),IFNULL(email,''),password_hash,role_type,onboarding_completed,status,is_deleted),256) fingerprint FROM sys_user
  UNION ALL SELECT 'user_profile',CAST(id AS CHAR),SHA2(CONCAT_WS('|',user_id,nickname,IFNULL(avatar_url,''),IFNULL(bio,''),IFNULL(main_team_id,''),post_count,follower_count,following_count,team_follow_count,player_follow_count,status,is_deleted),256) FROM user_profile
  UNION ALL SELECT 'content',CAST(id AS CHAR),SHA2(CONCAT_WS('|',content_type,content_format,card_type,title,IFNULL(summary,''),IFNULL(body,''),author_id,source_type,status,is_deleted),256) FROM content
  UNION ALL SELECT 'content_block',CAST(id AS CHAR),SHA2(CONCAT_WS('|',content_id,block_type,IFNULL(text_content,''),IFNULL(media_file_id,''),IFNULL(media_url,''),IFNULL(embed_url,''),sort_order,status,is_deleted),256) FROM content_block
  UNION ALL SELECT 'comment',CAST(id AS CHAR),SHA2(CONCAT_WS('|',target_type,target_id,parent_id,IFNULL(root_id,''),IFNULL(reply_to_user_id,''),user_id,content_text,status,is_deleted),256) FROM comment
  UNION ALL SELECT 'like_record',CAST(id AS CHAR),SHA2(CONCAT_WS('|',user_id,target_type,target_id,status),256) FROM like_record
  UNION ALL SELECT 'favorite_record',CAST(id AS CHAR),SHA2(CONCAT_WS('|',user_id,target_type,target_id,status),256) FROM favorite_record
  UNION ALL SELECT 'follow_record',CAST(id AS CHAR),SHA2(CONCAT_WS('|',user_id,follow_type,target_id,is_main,status,is_deleted),256) FROM follow_record
  UNION ALL SELECT 'file_resource',CAST(id AS CHAR),SHA2(CONCAT_WS('|',IFNULL(user_id,''),biz_type,original_name,storage_name,object_key,relative_path,url,content_type,extension,size_bytes,storage_type,status,deleted),256) FROM file_resource
  UNION ALL SELECT 'football_league',CAST(id AS CHAR),SHA2(CONCAT_WS('|',league_name,IFNULL(league_name_en,''),IFNULL(country,''),IFNULL(season,''),league_type,status,is_deleted),256) FROM football_league
  UNION ALL SELECT 'football_team',CAST(id AS CHAR),SHA2(CONCAT_WS('|',team_name,IFNULL(team_name_en,''),IFNULL(short_name,''),IFNULL(country,''),IFNULL(city,''),IFNULL(home_stadium,''),status,is_deleted),256) FROM football_team
  UNION ALL SELECT 'football_player',CAST(id AS CHAR),SHA2(CONCAT_WS('|',player_name,IFNULL(player_name_en,''),IFNULL(nationality,''),IFNULL(shirt_number,''),IFNULL(position,''),retired,status,is_deleted),256) FROM football_player
  UNION ALL SELECT 'team_player',CAST(id AS CHAR),SHA2(CONCAT_WS('|',team_id,player_id,team_type,IFNULL(season,''),IFNULL(shirt_number,''),IFNULL(position,''),status,is_deleted),256) FROM team_player
  UNION ALL SELECT 'match_info',CAST(id AS CHAR),SHA2(CONCAT_WS('|',league_id,IFNULL(season,''),IFNULL(round_name,''),home_team_id,away_team_id,IFNULL(home_score,''),IFNULL(away_score,''),match_time,match_status,status,is_deleted),256) FROM match_info
) preserved ORDER BY table_name,row_id;
