package com.southstand.follow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.southstand.auth.security.CurrentUserHolder;
import com.southstand.common.enums.ErrorCode;
import com.southstand.common.exception.BusinessException;
import com.southstand.follow.dto.FollowToggleRequest;
import com.southstand.follow.entity.FollowRecord;
import com.southstand.follow.mapper.FollowRecordMapper;
import com.southstand.follow.vo.FollowToggleResponse;
import com.southstand.football.player.entity.FootballPlayer;
import com.southstand.football.player.mapper.FootballPlayerMapper;
import com.southstand.football.team.entity.FootballTeam;
import com.southstand.football.team.mapper.FootballTeamMapper;
import com.southstand.user.entity.SysUser;
import com.southstand.user.entity.UserProfile;
import com.southstand.user.mapper.SysUserMapper;
import com.southstand.user.mapper.UserProfileMapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FollowService {

    public static final String TYPE_USER = "USER";
    public static final String TYPE_TEAM = "TEAM";
    public static final String TYPE_PLAYER = "PLAYER";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private final FollowRecordMapper followRecordMapper;
    private final FootballTeamMapper footballTeamMapper;
    private final FootballPlayerMapper footballPlayerMapper;
    private final SysUserMapper sysUserMapper;
    private final UserProfileMapper userProfileMapper;

    public FollowService(
            FollowRecordMapper followRecordMapper,
            FootballTeamMapper footballTeamMapper,
            FootballPlayerMapper footballPlayerMapper,
            SysUserMapper sysUserMapper,
            UserProfileMapper userProfileMapper
    ) {
        this.followRecordMapper = followRecordMapper;
        this.footballTeamMapper = footballTeamMapper;
        this.footballPlayerMapper = footballPlayerMapper;
        this.sysUserMapper = sysUserMapper;
        this.userProfileMapper = userProfileMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public FollowToggleResponse toggle(FollowToggleRequest request) {
        Long userId = CurrentUserHolder.get().getUserId();
        String followType = request.getFollowType();
        validateTarget(userId, followType, request.getTargetId());

        FollowRecord record = findRecord(userId, followType, request.getTargetId());
        boolean followed;
        if (record == null) {
            FollowRecord created = new FollowRecord();
            created.setUserId(userId);
            created.setFollowType(followType);
            created.setTargetId(request.getTargetId());
            created.setIsMain(0);
            created.setStatus(STATUS_ACTIVE);
            created.setIsDeleted(0);
            followRecordMapper.insert(created);
            followed = true;
        } else if (STATUS_ACTIVE.equals(record.getStatus()) && Integer.valueOf(0).equals(record.getIsDeleted())) {
            followRecordMapper.update(null, new UpdateWrapper<FollowRecord>()
                    .eq("id", record.getId())
                    .set("status", STATUS_CANCELLED)
                    .set("is_main", 0));
            followed = false;
        } else {
            followRecordMapper.update(null, new UpdateWrapper<FollowRecord>()
                    .eq("id", record.getId())
                    .set("status", STATUS_ACTIVE)
                    .set("is_deleted", 0));
            followed = true;
        }

        FollowCounts counts = syncUserProfileCounts(userId);
        FollowToggleResponse response = new FollowToggleResponse();
        response.setFollowType(followType);
        response.setTargetId(request.getTargetId());
        response.setFollowed(followed);
        response.setTeamFollowCount(counts.getTeamFollowCount());
        response.setPlayerFollowCount(counts.getPlayerFollowCount());
        response.setFollowingCount(counts.getFollowingCount());
        return response;
    }

    public void ensureActiveFollow(Long userId, String followType, Long targetId, boolean isMain) {
        FollowRecord record = findRecord(userId, followType, targetId);
        if (record == null) {
            FollowRecord created = new FollowRecord();
            created.setUserId(userId);
            created.setFollowType(followType);
            created.setTargetId(targetId);
            created.setIsMain(isMain ? 1 : 0);
            created.setStatus(STATUS_ACTIVE);
            created.setIsDeleted(0);
            followRecordMapper.insert(created);
            return;
        }
        followRecordMapper.update(null, new UpdateWrapper<FollowRecord>()
                .eq("id", record.getId())
                .set("status", STATUS_ACTIVE)
                .set("is_deleted", 0)
                .set("is_main", isMain ? 1 : record.getIsMain()));
    }

    public Set<Long> findActiveTargetIds(Long userId, String followType) {
        List<FollowRecord> records = followRecordMapper.selectList(new LambdaQueryWrapper<FollowRecord>()
                .eq(FollowRecord::getUserId, userId)
                .eq(FollowRecord::getFollowType, followType)
                .eq(FollowRecord::getStatus, STATUS_ACTIVE)
                .eq(FollowRecord::getIsDeleted, 0));
        Set<Long> ids = new HashSet<>();
        for (FollowRecord record : records) {
            ids.add(record.getTargetId());
        }
        return ids;
    }

    public List<FollowRecord> findActiveRecords(Long userId, String followType) {
        return followRecordMapper.selectList(new LambdaQueryWrapper<FollowRecord>()
                .eq(FollowRecord::getUserId, userId)
                .eq(FollowRecord::getFollowType, followType)
                .eq(FollowRecord::getStatus, STATUS_ACTIVE)
                .eq(FollowRecord::getIsDeleted, 0)
                .orderByAsc(FollowRecord::getId));
    }

    public FollowCounts syncUserProfileCounts(Long userId) {
        int teamCount = countActive(userId, TYPE_TEAM);
        int playerCount = countActive(userId, TYPE_PLAYER);
        int followingCount = countActive(userId, TYPE_USER);
        userProfileMapper.update(null, new UpdateWrapper<UserProfile>()
                .eq("user_id", userId)
                .set("team_follow_count", teamCount)
                .set("player_follow_count", playerCount)
                .set("following_count", followingCount));
        return new FollowCounts(teamCount, playerCount, followingCount);
    }

    public FootballTeam requireActiveTeam(Long teamId) {
        FootballTeam team = footballTeamMapper.selectById(teamId);
        if (team == null || !STATUS_ACTIVE.equals(team.getStatus()) || Integer.valueOf(1).equals(team.getIsDeleted())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "team not found");
        }
        return team;
    }

    public FootballPlayer requireActivePlayer(Long playerId) {
        FootballPlayer player = footballPlayerMapper.selectById(playerId);
        if (player == null || !STATUS_ACTIVE.equals(player.getStatus()) || Integer.valueOf(1).equals(player.getIsDeleted())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "player not found");
        }
        return player;
    }

    private void validateTarget(Long currentUserId, String followType, Long targetId) {
        if (TYPE_TEAM.equals(followType)) {
            requireActiveTeam(targetId);
            return;
        }
        if (TYPE_PLAYER.equals(followType)) {
            requireActivePlayer(targetId);
            return;
        }
        if (TYPE_USER.equals(followType)) {
            if (currentUserId.equals(targetId)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "cannot follow yourself");
            }
            SysUser target = sysUserMapper.selectById(targetId);
            if (target == null || !STATUS_ACTIVE.equals(target.getStatus()) || Integer.valueOf(1).equals(target.getIsDeleted())) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "user not found");
            }
            return;
        }
        throw new BusinessException(ErrorCode.PARAM_ERROR);
    }

    private FollowRecord findRecord(Long userId, String followType, Long targetId) {
        return followRecordMapper.selectOne(new LambdaQueryWrapper<FollowRecord>()
                .eq(FollowRecord::getUserId, userId)
                .eq(FollowRecord::getFollowType, followType)
                .eq(FollowRecord::getTargetId, targetId)
                .last("LIMIT 1"));
    }

    private int countActive(Long userId, String followType) {
        Long count = followRecordMapper.selectCount(new LambdaQueryWrapper<FollowRecord>()
                .eq(FollowRecord::getUserId, userId)
                .eq(FollowRecord::getFollowType, followType)
                .eq(FollowRecord::getStatus, STATUS_ACTIVE)
                .eq(FollowRecord::getIsDeleted, 0));
        return count == null ? 0 : count.intValue();
    }

    public static class FollowCounts {
        private final int teamFollowCount;
        private final int playerFollowCount;
        private final int followingCount;

        public FollowCounts(int teamFollowCount, int playerFollowCount, int followingCount) {
            this.teamFollowCount = teamFollowCount;
            this.playerFollowCount = playerFollowCount;
            this.followingCount = followingCount;
        }

        public int getTeamFollowCount() {
            return teamFollowCount;
        }

        public int getPlayerFollowCount() {
            return playerFollowCount;
        }

        public int getFollowingCount() {
            return followingCount;
        }
    }
}
