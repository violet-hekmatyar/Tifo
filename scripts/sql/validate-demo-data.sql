USE south_stand;
SET NAMES utf8mb4;

-- Every row must return anomaly_count = 0. IDs below 10^16 are base seed data.
SELECT check_name, anomaly_count FROM (
  SELECT 'missing_profile_user' check_name, COUNT(*) anomaly_count FROM user_profile p LEFT JOIN sys_user u ON u.id=p.user_id WHERE p.user_id>=11000000000000001 AND u.id IS NULL
  UNION ALL SELECT 'missing_main_team', COUNT(*) FROM user_profile p JOIN sys_user u ON u.id=p.user_id AND u.id>=11000000000000001 AND u.username LIKE 'demo_user_%' LEFT JOIN football_team t ON t.id=p.main_team_id WHERE (t.id IS NULL OR t.status<>'ACTIVE')
  UNION ALL SELECT 'main_team_follow_mismatch', COUNT(*) FROM user_profile p JOIN sys_user u ON u.id=p.user_id AND u.id>=11000000000000001 AND u.status='ACTIVE' AND u.username LIKE 'demo_user_%' LEFT JOIN follow_record f ON f.user_id=p.user_id AND f.follow_type='TEAM' AND f.target_id=p.main_team_id AND f.status='ACTIVE' AND f.is_deleted=0 WHERE f.id IS NULL
  UNION ALL SELECT 'missing_team_player_team', COUNT(*) FROM team_player tp LEFT JOIN football_team t ON t.id=tp.team_id WHERE tp.player_id>=14000000000000001 AND t.id IS NULL
  UNION ALL SELECT 'missing_team_player_player', COUNT(*) FROM team_player tp LEFT JOIN football_player p ON p.id=tp.player_id WHERE tp.player_id>=14000000000000001 AND p.id IS NULL
  UNION ALL SELECT 'player_multiple_active_teams', COUNT(*) FROM (SELECT player_id FROM team_player WHERE player_id>=14000000000000001 AND status='ACTIVE' AND is_deleted=0 GROUP BY player_id HAVING COUNT(*)<>1) x
  UNION ALL SELECT 'team_shirt_number_duplicate', COUNT(*) FROM (SELECT team_id,shirt_number FROM team_player WHERE player_id>=14000000000000001 AND status='ACTIVE' AND is_deleted=0 GROUP BY team_id,shirt_number HAVING COUNT(*)>1) x
  UNION ALL SELECT 'team_equals_opponent', COUNT(*) FROM match_info WHERE id>=15000000000000001 AND home_team_id=away_team_id
  UNION ALL SELECT 'match_missing_league_or_team', COUNT(*) FROM match_info m LEFT JOIN football_league l ON l.id=m.league_id LEFT JOIN football_team h ON h.id=m.home_team_id LEFT JOIN football_team a ON a.id=m.away_team_id WHERE m.id>=15000000000000001 AND (l.id IS NULL OR h.id IS NULL OR a.id IS NULL)
  UNION ALL SELECT 'scheduled_has_event', COUNT(*) FROM match_event e JOIN match_info m ON m.id=e.match_id WHERE m.id>=15000000000000001 AND m.match_status='SCHEDULED' AND e.status='ACTIVE' AND e.is_deleted=0
  UNION ALL SELECT 'event_team_not_in_match', COUNT(*) FROM match_event e JOIN match_info m ON m.id=e.match_id WHERE e.id>=15100000000000001 AND e.team_id NOT IN (m.home_team_id,m.away_team_id)
  UNION ALL SELECT 'event_player_not_in_team', COUNT(*) FROM match_event e LEFT JOIN team_player tp ON tp.team_id=e.team_id AND tp.player_id=e.player_id AND tp.status='ACTIVE' AND tp.is_deleted=0 WHERE e.id>=15100000000000001 AND e.player_id IS NOT NULL AND tp.id IS NULL
  UNION ALL SELECT 'assist_player_not_in_team', COUNT(*) FROM match_event e LEFT JOIN team_player tp ON tp.team_id=e.team_id AND tp.player_id=e.assist_player_id AND tp.status='ACTIVE' AND tp.is_deleted=0 WHERE e.id>=15100000000000001 AND e.assist_player_id IS NOT NULL AND tp.id IS NULL
  UNION ALL SELECT 'score_goal_mismatch', COUNT(*) FROM match_info m LEFT JOIN (SELECT match_id, SUM(team_id=m2.home_team_id) home_goals, SUM(team_id=m2.away_team_id) away_goals FROM match_event e JOIN match_info m2 ON m2.id=e.match_id WHERE e.event_type='GOAL' AND e.status='ACTIVE' AND e.is_deleted=0 GROUP BY match_id) g ON g.match_id=m.id WHERE m.id>=15000000000000001 AND m.match_status IN ('LIVE','FINISHED') AND (m.home_score<>COALESCE(g.home_goals,0) OR m.away_score<>COALESCE(g.away_goals,0))
  UNION ALL SELECT 'report_non_finished_match', COUNT(*) FROM match_report r JOIN match_info m ON m.id=r.match_id WHERE r.id>=15100000000000001 AND m.match_status<>'FINISHED'
  UNION ALL SELECT 'report_missing_content', COUNT(*) FROM match_report r LEFT JOIN content c ON c.id=r.content_id WHERE r.id>=15100000000000001 AND (c.id IS NULL OR c.content_type<>'REPORT')
  UNION ALL SELECT 'content_missing_author', COUNT(*) FROM content c LEFT JOIN sys_user u ON u.id=c.author_id WHERE c.id>=16000000000000001 AND (u.id IS NULL OR u.status<>'ACTIVE')
  UNION ALL SELECT 'media_missing_content', COUNT(*) FROM content_media m LEFT JOIN content c ON c.id=m.content_id WHERE m.content_id>=16000000000000001 AND c.id IS NULL
  UNION ALL SELECT 'block_missing_content', COUNT(*) FROM content_block b LEFT JOIN content c ON c.id=b.content_id WHERE b.content_id>=16000000000000001 AND c.id IS NULL
  UNION ALL SELECT 'non_article_has_block', COUNT(*) FROM content_block b JOIN content c ON c.id=b.content_id WHERE b.content_id>=16000000000000001 AND c.content_type<>'ARTICLE' AND b.is_deleted=0
  UNION ALL SELECT 'article_duplicate_sort', COUNT(*) FROM (SELECT content_id,sort_order FROM content_block WHERE content_id>=16000000000000001 AND is_deleted=0 GROUP BY content_id,sort_order HAVING COUNT(*)>1) x
  UNION ALL SELECT 'text_block_empty', COUNT(*) FROM content_block WHERE content_id>=16000000000000001 AND block_type='TEXT' AND is_deleted=0 AND (text_content IS NULL OR TRIM(text_content)='')
  UNION ALL SELECT 'image_block_missing_file', COUNT(*) FROM content_block b LEFT JOIN file_resource f ON f.id=b.media_file_id WHERE b.content_id>=16000000000000001 AND b.block_type='IMAGE' AND b.is_deleted=0 AND (b.media_file_id IS NULL OR f.id IS NULL OR f.status<>'ACTIVE' OR f.deleted<>0)
  UNION ALL SELECT 'relation_missing_entity', COUNT(*) FROM content_relation r LEFT JOIN football_team t ON r.relation_type='TEAM' AND t.id=r.relation_id LEFT JOIN football_player p ON r.relation_type='PLAYER' AND p.id=r.relation_id LEFT JOIN match_info m ON r.relation_type='MATCH' AND m.id=r.relation_id WHERE r.content_id>=16000000000000001 AND r.status='ACTIVE' AND r.is_deleted=0 AND ((r.relation_type='TEAM' AND t.id IS NULL) OR (r.relation_type='PLAYER' AND p.id IS NULL) OR (r.relation_type='MATCH' AND m.id IS NULL) OR r.relation_type NOT IN ('TEAM','PLAYER','MATCH'))
  UNION ALL SELECT 'relation_active_duplicate', COUNT(*) FROM (SELECT content_id,relation_type,relation_id FROM content_relation WHERE content_id>=16000000000000001 AND status='ACTIVE' AND is_deleted=0 GROUP BY content_id,relation_type,relation_id HAVING COUNT(*)>1) x
  UNION ALL SELECT 'comment_missing_content_or_user', COUNT(*) FROM comment cm LEFT JOIN content c ON c.id=cm.target_id LEFT JOIN sys_user u ON u.id=cm.user_id WHERE cm.id>=17000000000000001 AND (c.id IS NULL OR u.id IS NULL)
  UNION ALL SELECT 'reply_broken_root_parent', COUNT(*) FROM comment cm LEFT JOIN comment root ON root.id=cm.root_id LEFT JOIN comment parent ON parent.id=cm.parent_id WHERE cm.id>=17000000000000001 AND cm.parent_id<>0 AND (root.id IS NULL OR root.parent_id<>0 OR parent.id IS NULL OR root.target_id<>cm.target_id)
  UNION ALL SELECT 'reply_missing_user', COUNT(*) FROM comment cm LEFT JOIN sys_user u ON u.id=cm.reply_to_user_id WHERE cm.id>=17000000000000001 AND cm.parent_id<>0 AND u.id IS NULL
  UNION ALL SELECT 'like_missing_target', COUNT(*) FROM like_record l LEFT JOIN content c ON l.target_type='CONTENT' AND c.id=l.target_id LEFT JOIN comment cm ON l.target_type='COMMENT' AND cm.id=l.target_id WHERE l.id>=18000000000000001 AND ((l.target_type='CONTENT' AND c.id IS NULL) OR (l.target_type='COMMENT' AND cm.id IS NULL) OR l.target_type NOT IN ('CONTENT','COMMENT'))
  UNION ALL SELECT 'favorite_missing_content', COUNT(*) FROM favorite_record f LEFT JOIN content c ON c.id=f.target_id WHERE f.id>=18000000000000001 AND (f.target_type<>'CONTENT' OR c.id IS NULL)
  UNION ALL SELECT 'follow_missing_target_or_self', COUNT(*) FROM follow_record f LEFT JOIN sys_user u ON f.follow_type='USER' AND u.id=f.target_id LEFT JOIN football_team t ON f.follow_type='TEAM' AND t.id=f.target_id LEFT JOIN football_player p ON f.follow_type='PLAYER' AND p.id=f.target_id WHERE f.id>=18000000000000001 AND ((f.follow_type='USER' AND (u.id IS NULL OR f.user_id=f.target_id)) OR (f.follow_type='TEAM' AND t.id IS NULL) OR (f.follow_type='PLAYER' AND p.id IS NULL))
  UNION ALL SELECT 'duplicate_active_like', COUNT(*) FROM (SELECT user_id,target_type,target_id FROM like_record WHERE id>=18000000000000001 AND status='ACTIVE' GROUP BY user_id,target_type,target_id HAVING COUNT(*)>1) x
  UNION ALL SELECT 'duplicate_active_favorite', COUNT(*) FROM (SELECT user_id,target_type,target_id FROM favorite_record WHERE id>=18000000000000001 AND status='ACTIVE' GROUP BY user_id,target_type,target_id HAVING COUNT(*)>1) x
  UNION ALL SELECT 'duplicate_active_follow', COUNT(*) FROM (SELECT user_id,follow_type,target_id FROM follow_record WHERE id>=18000000000000001 AND status='ACTIVE' AND is_deleted=0 GROUP BY user_id,follow_type,target_id HAVING COUNT(*)>1) x
  UNION ALL SELECT 'content_like_count_mismatch', COUNT(*) FROM content c LEFT JOIN (SELECT target_id,COUNT(*) n FROM like_record WHERE target_type='CONTENT' AND status='ACTIVE' GROUP BY target_id) x ON x.target_id=c.id WHERE c.id>=16000000000000001 AND c.like_count<>COALESCE(x.n,0)
  UNION ALL SELECT 'content_favorite_count_mismatch', COUNT(*) FROM content c LEFT JOIN (SELECT target_id,COUNT(*) n FROM favorite_record WHERE target_type='CONTENT' AND status='ACTIVE' GROUP BY target_id) x ON x.target_id=c.id WHERE c.id>=16000000000000001 AND c.favorite_count<>COALESCE(x.n,0)
  UNION ALL SELECT 'content_comment_count_mismatch', COUNT(*) FROM content c LEFT JOIN (SELECT target_id,COUNT(*) n FROM comment WHERE target_type='CONTENT' AND status='ACTIVE' AND is_deleted=0 GROUP BY target_id) x ON x.target_id=c.id WHERE c.id>=16000000000000001 AND c.comment_count<>COALESCE(x.n,0)
  UNION ALL SELECT 'comment_like_count_mismatch', COUNT(*) FROM comment cm LEFT JOIN (SELECT target_id,COUNT(*) n FROM like_record WHERE target_type='COMMENT' AND status='ACTIVE' GROUP BY target_id) x ON x.target_id=cm.id WHERE cm.id>=17000000000000001 AND cm.like_count<>COALESCE(x.n,0)
  UNION ALL SELECT 'root_reply_count_mismatch', COUNT(*) FROM comment cm LEFT JOIN (SELECT root_id,COUNT(*) n FROM comment WHERE parent_id<>0 AND status='ACTIVE' AND is_deleted=0 GROUP BY root_id) x ON x.root_id=cm.id WHERE cm.id>=17000000000000001 AND cm.parent_id=0 AND cm.reply_count<>COALESCE(x.n,0)
  UNION ALL SELECT 'active_content_below_60', IF(COUNT(*)>=60,0,1) FROM content WHERE id>=16000000000000001 AND status='PUBLISHED' AND is_deleted=0
  UNION ALL SELECT 'active_match_below_60', IF(COUNT(*)>=60,0,1) FROM match_info WHERE id>=15000000000000001 AND status='ACTIVE' AND is_deleted=0
  UNION ALL SELECT 'active_player_below_100', IF(COUNT(*)>=100,0,1) FROM football_player WHERE id>=14000000000000001 AND status='ACTIVE' AND is_deleted=0
  UNION ALL SELECT 'active_comment_below_200', IF(COUNT(*)>=200,0,1) FROM comment WHERE id>=17000000000000001 AND status='ACTIVE' AND is_deleted=0
  UNION ALL SELECT 'users_with_20_likes_below_3', IF(COUNT(*)>=3,0,1) FROM (SELECT user_id FROM like_record WHERE id>=18000000000000001 AND target_type='CONTENT' AND status='ACTIVE' GROUP BY user_id HAVING COUNT(*)>=20) x
  UNION ALL SELECT 'users_with_20_favorites_below_3', IF(COUNT(*)>=3,0,1) FROM (SELECT user_id FROM favorite_record WHERE id>=18000000000000001 AND status='ACTIVE' GROUP BY user_id HAVING COUNT(*)>=20) x
  UNION ALL SELECT 'contents_with_20_comments_below_5', IF(COUNT(*)>=5,0,1) FROM (SELECT target_id FROM comment WHERE id>=17000000000000001 AND status='ACTIVE' AND is_deleted=0 GROUP BY target_id HAVING COUNT(*)>=20) x
  UNION ALL SELECT 'season_missing_league', COUNT(*) FROM football_season s LEFT JOIN football_league l ON l.id=s.league_id WHERE s.id>=20000000000000001 AND l.id IS NULL
  UNION ALL SELECT 'season_invalid_dates', COUNT(*) FROM football_season WHERE id>=20000000000000001 AND start_date>=end_date
  UNION ALL SELECT 'season_multiple_current', COUNT(*) FROM (SELECT league_id FROM football_season WHERE id>=20000000000000001 AND current_flag=1 AND status='ACTIVE' AND is_deleted=0 GROUP BY league_id HAVING COUNT(*)>1) x
  UNION ALL SELECT 'season_duplicate_code', COUNT(*) FROM (SELECT league_id,season_code FROM football_season WHERE id>=20000000000000001 GROUP BY league_id,season_code HAVING COUNT(*)>1) x
  UNION ALL SELECT 'stage_missing_scope', COUNT(*) FROM football_competition_stage st LEFT JOIN football_league l ON l.id=st.league_id LEFT JOIN football_season s ON s.id=st.season_id WHERE st.id>=21000000000000001 AND (l.id IS NULL OR s.id IS NULL OR s.league_id<>st.league_id)
  UNION ALL SELECT 'standing_missing_team', COUNT(*) FROM football_standing fs LEFT JOIN football_team t ON t.id=fs.team_id WHERE fs.id>=22000000000000001 AND t.id IS NULL
  UNION ALL SELECT 'standing_played_formula', COUNT(*) FROM football_standing WHERE id>=22000000000000001 AND played<>won+drawn+lost
  UNION ALL SELECT 'standing_goal_difference_formula', COUNT(*) FROM football_standing WHERE id>=22000000000000001 AND goal_difference<>goals_for-goals_against
  UNION ALL SELECT 'standing_points_formula', COUNT(*) FROM football_standing WHERE id>=22000000000000001 AND points<>won*3+drawn-deduction_points
  UNION ALL SELECT 'standing_duplicate_team', COUNT(*) FROM (SELECT league_id,season_id,stage_id,group_code,team_id FROM football_standing WHERE id>=22000000000000001 GROUP BY league_id,season_id,stage_id,group_code,team_id HAVING COUNT(*)>1) x
  UNION ALL SELECT 'standing_duplicate_rank', COUNT(*) FROM (SELECT league_id,season_id,stage_id,group_code,rank_no FROM football_standing WHERE id>=22000000000000001 GROUP BY league_id,season_id,stage_id,group_code,rank_no HAVING COUNT(*)>1) x
  UNION ALL SELECT 'standing_rank_not_continuous', COUNT(*) FROM (SELECT league_id,season_id,stage_id,group_code,COUNT(*) n,MIN(rank_no) min_rank,MAX(rank_no) max_rank FROM football_standing WHERE id>=22000000000000001 GROUP BY league_id,season_id,stage_id,group_code HAVING min_rank<>1 OR max_rank<>n) x
  UNION ALL SELECT 'standing_rank_order_mismatch', COUNT(*) FROM (SELECT id,rank_no,ROW_NUMBER() OVER(PARTITION BY league_id,season_id,stage_id,group_code ORDER BY points DESC,goal_difference DESC,goals_for DESC,team_id ASC) expected_rank FROM football_standing WHERE id>=22000000000000001) x WHERE rank_no<>expected_rank
  UNION ALL SELECT 'standing_source_not_demo', COUNT(*) FROM football_standing WHERE id>=22000000000000001 AND source<>'DEMO'
  UNION ALL SELECT 'player_stat_missing_scope', COUNT(*) FROM football_player_competition_stat ps LEFT JOIN football_season s ON s.id=ps.season_id LEFT JOIN football_player p ON p.id=ps.player_id LEFT JOIN football_team t ON t.id=ps.team_id LEFT JOIN team_player tp ON tp.player_id=ps.player_id AND tp.team_id=ps.team_id AND tp.status='ACTIVE' AND tp.is_deleted=0 WHERE ps.id>=23000000000000001 AND (s.id IS NULL OR s.league_id<>ps.league_id OR p.id IS NULL OR t.id IS NULL OR tp.id IS NULL)
  UNION ALL SELECT 'player_stat_invalid_totals', COUNT(*) FROM football_player_competition_stat WHERE id>=23000000000000001 AND (appearances<starts OR shots<shots_on_target OR goals>shots_on_target OR LEAST(appearances,starts,minutes,goals,assists,yellow_cards,red_cards,shots,shots_on_target,saves)<0)
  UNION ALL SELECT 'player_stat_rating_range', COUNT(*) FROM football_player_competition_stat WHERE id>=23000000000000001 AND (rating<5.00 OR rating>9.50)
  UNION ALL SELECT 'player_stat_non_goalkeeper_saves', COUNT(*) FROM football_player_competition_stat ps JOIN football_player p ON p.id=ps.player_id WHERE ps.id>=23000000000000001 AND p.position<>'GK' AND ps.saves<>0
  UNION ALL SELECT 'player_stat_source_not_demo', COUNT(*) FROM football_player_competition_stat WHERE id>=23000000000000001 AND source<>'DEMO'
  UNION ALL SELECT 'team_stat_missing_scope', COUNT(*) FROM football_team_competition_stat ts LEFT JOIN football_season s ON s.id=ts.season_id LEFT JOIN football_team t ON t.id=ts.team_id WHERE ts.id>=24000000000000001 AND (s.id IS NULL OR s.league_id<>ts.league_id OR t.id IS NULL)
  UNION ALL SELECT 'team_stat_standing_mismatch', COUNT(*) FROM football_team_competition_stat ts LEFT JOIN football_standing fs ON fs.league_id=ts.league_id AND fs.season_id=ts.season_id AND fs.stage_id=ts.stage_id AND fs.team_id=ts.team_id WHERE ts.id>=24000000000000001 AND (fs.id IS NULL OR ts.played<>fs.played OR ts.goals_for<>fs.goals_for OR ts.goals_against<>fs.goals_against)
  UNION ALL SELECT 'team_stat_invalid_totals', COUNT(*) FROM football_team_competition_stat WHERE id>=24000000000000001 AND (shots<shots_on_target OR assists>goals_for OR LEAST(played,goals_for,goals_against,assists,yellow_cards,red_cards,shots,shots_on_target,corners,fouls,clean_sheets)<0)
  UNION ALL SELECT 'team_stat_source_not_demo', COUNT(*) FROM football_team_competition_stat WHERE id>=24000000000000001 AND source<>'DEMO'
  UNION ALL SELECT 'standing_leagues_below_5', IF(COUNT(*)>=5,0,1) FROM (SELECT league_id FROM football_standing WHERE id>=22000000000000001 GROUP BY league_id HAVING COUNT(*)>=8) x
  UNION ALL SELECT 'player_stats_below_120', IF(COUNT(*)>=120,0,1) FROM football_player_competition_stat WHERE id>=23000000000000001
  UNION ALL SELECT 'team_stats_below_24', IF(COUNT(*)>=24,0,1) FROM football_team_competition_stat WHERE id>=24000000000000001
) checks ORDER BY check_name;

SELECT 'demo_users' metric, COUNT(*) value FROM sys_user WHERE id>=11000000000000001
UNION ALL SELECT 'demo_teams',COUNT(*) FROM football_team WHERE id>=13000000000000001
UNION ALL SELECT 'demo_players',COUNT(*) FROM football_player WHERE id>=14000000000000001
UNION ALL SELECT 'demo_matches',COUNT(*) FROM match_info WHERE id>=15000000000000001
UNION ALL SELECT 'demo_contents',COUNT(*) FROM content WHERE id>=16000000000000001
UNION ALL SELECT 'demo_comments',COUNT(*) FROM comment WHERE id>=17000000000000001
UNION ALL SELECT 'demo_seasons',COUNT(*) FROM football_season WHERE id>=20000000000000001
UNION ALL SELECT 'demo_stages',COUNT(*) FROM football_competition_stage WHERE id>=21000000000000001
UNION ALL SELECT 'demo_standings',COUNT(*) FROM football_standing WHERE id>=22000000000000001
UNION ALL SELECT 'demo_player_stats',COUNT(*) FROM football_player_competition_stat WHERE id>=23000000000000001
UNION ALL SELECT 'demo_team_stats',COUNT(*) FROM football_team_competition_stat WHERE id>=24000000000000001;

SELECT 't17_roster_chain_missing' check_name,COUNT(*) anomaly_count
FROM football_player p
LEFT JOIN team_player tp ON tp.player_id=p.id AND tp.status='ACTIVE' AND tp.is_deleted=0
LEFT JOIN football_team_season_player r ON r.player_id=p.id AND r.status='ACTIVE' AND r.is_deleted=0
LEFT JOIN football_player_team_history h ON h.player_id=p.id AND h.current_flag=1 AND h.is_deleted=0
LEFT JOIN football_player_competition_stat ps ON ps.player_id=p.id AND ps.team_id=r.team_id AND ps.season_id=r.season_id AND ps.is_deleted=0
WHERE p.id>=33000000000000001 AND p.id<33100000000000001 AND p.is_deleted=0 AND (tp.id IS NULL OR r.id IS NULL OR h.id IS NULL OR ps.id IS NULL);

SELECT 't17_complete_team_roster_below_18' check_name,COUNT(*) anomaly_count FROM (
 SELECT l.match_id,l.team_id,COUNT(DISTINCT r.player_id) n
 FROM football_match_lineup l JOIN match_info m ON m.id=l.match_id JOIN football_season s ON s.league_id=m.league_id AND s.current_flag=1 AND s.is_deleted=0
 LEFT JOIN football_team_season_player r ON r.season_id=s.id AND r.team_id=l.team_id AND r.status='ACTIVE' AND r.is_deleted=0
 WHERE l.source='DEMO' AND l.is_deleted=0 GROUP BY l.match_id,l.team_id HAVING n<18
) x;

SELECT 't17_complete_matches_below_12' check_name,IF(COUNT(*)>=12,0,1) anomaly_count FROM (
 SELECT m.id FROM match_info m
 WHERE m.match_status='FINISHED' AND m.is_deleted=0
 AND (SELECT COUNT(*) FROM football_match_lineup l WHERE l.match_id=m.id AND l.is_deleted=0)=2
 AND (SELECT COUNT(*) FROM football_match_player_appearance a WHERE a.match_id=m.id AND a.lineup_type='STARTER' AND a.status='ACTIVE' AND a.is_deleted=0)=22
 AND (SELECT COUNT(*) FROM football_match_player_appearance a WHERE a.match_id=m.id AND a.lineup_type IN('SUBSTITUTE','BENCH') AND a.status='ACTIVE' AND a.is_deleted=0)>=10
 AND (SELECT COUNT(*) FROM football_match_team_stat t WHERE t.match_id=m.id AND t.is_deleted=0)=2
) x;
