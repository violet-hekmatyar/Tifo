package com.southstand.football.matchdata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.southstand.football.matchdata.entity.FootballUserPlayerRating;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface FootballUserPlayerRatingMapper extends BaseMapper<FootballUserPlayerRating>{
    @Insert("""
      INSERT INTO football_user_player_rating(id,match_id,player_id,user_id,rating,status,is_deleted)
      VALUES(#{row.id},#{row.matchId},#{row.playerId},#{row.userId},#{row.rating},'ACTIVE',0)
      ON DUPLICATE KEY UPDATE rating=VALUES(rating),status='ACTIVE',is_deleted=0,update_time=CURRENT_TIMESTAMP
      """)
    int upsertActive(@Param("row") FootballUserPlayerRating row);

    @Update("""
      UPDATE football_user_player_rating SET status='CANCELLED',is_deleted=1,update_time=CURRENT_TIMESTAMP
      WHERE user_id=#{userId} AND match_id=#{matchId} AND player_id=#{playerId} AND status='ACTIVE' AND is_deleted=0
      """)
    int cancelOwn(@Param("userId")Long userId,@Param("matchId")Long matchId,@Param("playerId")Long playerId);
}
