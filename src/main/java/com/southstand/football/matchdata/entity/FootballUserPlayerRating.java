package com.southstand.football.matchdata.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("football_user_player_rating")
public class FootballUserPlayerRating {
    @TableId(type=IdType.ASSIGN_ID) private Long id;
    private Long matchId; private Long playerId; private Long userId; private BigDecimal rating; private String status; private LocalDateTime createTime; private LocalDateTime updateTime; private Integer isDeleted;
    public Long getId(){return id;} public void setId(Long v){id=v;} public Long getMatchId(){return matchId;} public void setMatchId(Long v){matchId=v;} public Long getPlayerId(){return playerId;} public void setPlayerId(Long v){playerId=v;} public Long getUserId(){return userId;} public void setUserId(Long v){userId=v;}
    public BigDecimal getRating(){return rating;} public void setRating(BigDecimal v){rating=v;} public String getStatus(){return status;} public void setStatus(String v){status=v;} public LocalDateTime getCreateTime(){return createTime;} public void setCreateTime(LocalDateTime v){createTime=v;} public LocalDateTime getUpdateTime(){return updateTime;} public void setUpdateTime(LocalDateTime v){updateTime=v;} public Integer getIsDeleted(){return isDeleted;} public void setIsDeleted(Integer v){isDeleted=v;}
}
