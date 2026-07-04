package com.southstand.file.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.southstand.auth.security.CurrentUserHolder;
import com.southstand.common.enums.ErrorCode;
import com.southstand.common.exception.BusinessException;
import com.southstand.file.domain.FileResourceEntity;
import com.southstand.file.vo.BindAvatarVO;
import com.southstand.user.entity.UserProfile;
import com.southstand.user.mapper.UserProfileMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FileBindingService {

    private final FileStorageService fileStorageService;
    private final UserProfileMapper userProfileMapper;

    public FileBindingService(FileStorageService fileStorageService, UserProfileMapper userProfileMapper) {
        this.fileStorageService = fileStorageService;
        this.userProfileMapper = userProfileMapper;
    }

    public BindAvatarVO bindAvatar(Long fileId) {
        if (fileId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "fileId required");
        }
        Long userId = CurrentUserHolder.get().getUserId();
        FileResourceEntity file = requireOwnedFile(userId, fileId);
        if (!"AVATAR".equals(file.getBizType()) && !"GENERAL_IMAGE".equals(file.getBizType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "file bizType is not allowed for avatar");
        }
        UserProfile profile = userProfileMapper.selectOne(new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, userId)
                .eq(UserProfile::getIsDeleted, 0)
                .last("LIMIT 1"));
        if (profile == null) {
            profile = new UserProfile();
            profile.setUserId(userId);
            profile.setNickname("user-" + userId);
            profile.setAvatarUrl(file.getUrl());
            profile.setPostCount(0);
            profile.setFollowerCount(0);
            profile.setFollowingCount(0);
            profile.setTeamFollowCount(0);
            profile.setPlayerFollowCount(0);
            profile.setStatus("ACTIVE");
            profile.setIsDeleted(0);
            userProfileMapper.insert(profile);
        } else {
            userProfileMapper.update(null, new LambdaUpdateWrapper<UserProfile>()
                    .eq(UserProfile::getUserId, userId)
                    .set(UserProfile::getAvatarUrl, file.getUrl()));
        }
        BindAvatarVO vo = new BindAvatarVO();
        vo.setAvatarUrl(file.getUrl());
        return vo;
    }

    public List<FileResourceEntity> validateContentImages(Long userId, List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return List.of();
        }
        List<FileResourceEntity> result = new ArrayList<>();
        for (Long fileId : fileIds) {
            FileResourceEntity file = requireOwnedFile(userId, fileId);
            if (!"CONTENT_IMAGE".equals(file.getBizType()) && !"GENERAL_IMAGE".equals(file.getBizType())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "file bizType is not allowed for content media");
            }
            result.add(file);
        }
        return result;
    }

    private FileResourceEntity requireOwnedFile(Long userId, Long fileId) {
        FileResourceEntity file = fileStorageService.requireActiveFile(fileId);
        if (!userId.equals(file.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "file does not belong to current user");
        }
        return file;
    }
}
