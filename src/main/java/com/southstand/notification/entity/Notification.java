package com.southstand.notification.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("notification")
public class Notification {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long recipientUserId;
    private Long actorUserId;
    private String notificationType;
    private String targetType;
    private Long targetId;
    private String secondaryTargetType;
    private Long secondaryTargetId;
    private String title;
    private String content;
    private Integer readFlag;
    private LocalDateTime readTime;
    private String status;
    private String dedupKey;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;

    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Long getRecipientUserId(){return recipientUserId;} public void setRecipientUserId(Long v){recipientUserId=v;}
    public Long getActorUserId(){return actorUserId;} public void setActorUserId(Long v){actorUserId=v;}
    public String getNotificationType(){return notificationType;} public void setNotificationType(String v){notificationType=v;}
    public String getTargetType(){return targetType;} public void setTargetType(String v){targetType=v;}
    public Long getTargetId(){return targetId;} public void setTargetId(Long v){targetId=v;}
    public String getSecondaryTargetType(){return secondaryTargetType;} public void setSecondaryTargetType(String v){secondaryTargetType=v;}
    public Long getSecondaryTargetId(){return secondaryTargetId;} public void setSecondaryTargetId(Long v){secondaryTargetId=v;}
    public String getTitle(){return title;} public void setTitle(String v){title=v;}
    public String getContent(){return content;} public void setContent(String v){content=v;}
    public Integer getReadFlag(){return readFlag;} public void setReadFlag(Integer v){readFlag=v;}
    public LocalDateTime getReadTime(){return readTime;} public void setReadTime(LocalDateTime v){readTime=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public String getDedupKey(){return dedupKey;} public void setDedupKey(String v){dedupKey=v;}
    public LocalDateTime getCreateTime(){return createTime;} public void setCreateTime(LocalDateTime v){createTime=v;}
    public LocalDateTime getUpdateTime(){return updateTime;} public void setUpdateTime(LocalDateTime v){updateTime=v;}
    public Integer getIsDeleted(){return isDeleted;} public void setIsDeleted(Integer v){isDeleted=v;}
}
