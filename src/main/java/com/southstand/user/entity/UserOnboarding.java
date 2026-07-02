package com.southstand.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("user_onboarding")
public class UserOnboarding {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long userId;
    private Long mainTeamId;
    private String selectedTeamIds;
    private String selectedPlayerIds;
    private Integer completed;
    private LocalDateTime completedTime;
    private String status;
    private Integer isDeleted;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getMainTeamId() {
        return mainTeamId;
    }

    public void setMainTeamId(Long mainTeamId) {
        this.mainTeamId = mainTeamId;
    }

    public String getSelectedTeamIds() {
        return selectedTeamIds;
    }

    public void setSelectedTeamIds(String selectedTeamIds) {
        this.selectedTeamIds = selectedTeamIds;
    }

    public String getSelectedPlayerIds() {
        return selectedPlayerIds;
    }

    public void setSelectedPlayerIds(String selectedPlayerIds) {
        this.selectedPlayerIds = selectedPlayerIds;
    }

    public Integer getCompleted() {
        return completed;
    }

    public void setCompleted(Integer completed) {
        this.completed = completed;
    }

    public LocalDateTime getCompletedTime() {
        return completedTime;
    }

    public void setCompletedTime(LocalDateTime completedTime) {
        this.completedTime = completedTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Integer isDeleted) {
        this.isDeleted = isDeleted;
    }
}
