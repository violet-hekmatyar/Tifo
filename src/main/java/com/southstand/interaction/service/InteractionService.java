package com.southstand.interaction.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.southstand.auth.security.CurrentUserHolder;
import com.southstand.common.enums.ErrorCode;
import com.southstand.common.exception.BusinessException;
import com.southstand.content.entity.Content;
import com.southstand.content.mapper.ContentMapper;
import com.southstand.content.service.ContentService;
import com.southstand.interaction.dto.ToggleFavoriteRequest;
import com.southstand.interaction.dto.ToggleLikeRequest;
import com.southstand.interaction.entity.Comment;
import com.southstand.interaction.entity.FavoriteRecord;
import com.southstand.interaction.entity.LikeRecord;
import com.southstand.interaction.mapper.CommentMapper;
import com.southstand.interaction.mapper.FavoriteRecordMapper;
import com.southstand.interaction.mapper.LikeRecordMapper;
import com.southstand.interaction.vo.ToggleFavoriteResponse;
import com.southstand.interaction.vo.ToggleLikeResponse;
import com.southstand.notification.service.NotificationService;
import com.southstand.recommend.model.RecommendationBehaviorType;
import com.southstand.recommend.model.RecommendationTargetType;
import com.southstand.recommend.service.RecommendationBehaviorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InteractionService {

    private static final String TARGET_CONTENT = "CONTENT";
    private static final String TARGET_COMMENT = "COMMENT";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_CANCELLED = "CANCELLED";

    private final LikeRecordMapper likeRecordMapper;
    private final FavoriteRecordMapper favoriteRecordMapper;
    private final ContentMapper contentMapper;
    private final CommentMapper commentMapper;
    private final ContentService contentService;
    private final RecommendationBehaviorService recommendationBehaviorService;
    private final NotificationService notificationService;

    @Autowired
    public InteractionService(LikeRecordMapper likeRecordMapper,
            FavoriteRecordMapper favoriteRecordMapper,
            ContentMapper contentMapper,
            CommentMapper commentMapper,
            ContentService contentService,
            RecommendationBehaviorService recommendationBehaviorService,
            NotificationService notificationService) {
        this.likeRecordMapper = likeRecordMapper;
        this.favoriteRecordMapper = favoriteRecordMapper;
        this.contentMapper = contentMapper;
        this.commentMapper = commentMapper;
        this.contentService = contentService;
        this.recommendationBehaviorService = recommendationBehaviorService;
        this.notificationService = notificationService;
    }

    public InteractionService(LikeRecordMapper likeRecordMapper,
            FavoriteRecordMapper favoriteRecordMapper, ContentMapper contentMapper, CommentMapper commentMapper,
            ContentService contentService, RecommendationBehaviorService recommendationBehaviorService) {
        this(likeRecordMapper, favoriteRecordMapper, contentMapper, commentMapper, contentService,
                recommendationBehaviorService, null);
    }

    public InteractionService(LikeRecordMapper likeRecordMapper,
            FavoriteRecordMapper favoriteRecordMapper,
            ContentMapper contentMapper,
            CommentMapper commentMapper,
            ContentService contentService) {
        this(likeRecordMapper, favoriteRecordMapper, contentMapper, commentMapper, contentService, null, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public ToggleLikeResponse toggleLike(ToggleLikeRequest request) {
        Long userId = CurrentUserHolder.get().getUserId();
        validateLikeTarget(request.getTargetType(), request.getTargetId());
        LikeRecord record = findLike(userId, request.getTargetType(), request.getTargetId());
        boolean liked;
        if (record == null) {
            LikeRecord created = new LikeRecord();
            created.setUserId(userId);
            created.setTargetType(request.getTargetType());
            created.setTargetId(request.getTargetId());
            created.setStatus(STATUS_ACTIVE);
            likeRecordMapper.insert(created);
            incrementLikeCount(request.getTargetType(), request.getTargetId());
            liked = true;
        } else if (STATUS_ACTIVE.equals(record.getStatus())) {
            likeRecordMapper.update(null, new UpdateWrapper<LikeRecord>()
                    .eq("id", record.getId())
                    .set("status", STATUS_CANCELLED));
            decrementLikeCount(request.getTargetType(), request.getTargetId());
            liked = false;
        } else {
            likeRecordMapper.update(null, new UpdateWrapper<LikeRecord>()
                    .eq("id", record.getId())
                    .set("status", STATUS_ACTIVE));
            incrementLikeCount(request.getTargetType(), request.getTargetId());
            liked = true;
        }
        ToggleLikeResponse response = new ToggleLikeResponse();
        response.setTargetType(request.getTargetType());
        response.setTargetId(request.getTargetId());
        response.setLiked(liked);
        if (liked && TARGET_CONTENT.equals(request.getTargetType()) && recommendationBehaviorService != null) {
            recommendationBehaviorService.recordInteractionSafely(userId, RecommendationBehaviorType.LIKE,
                    RecommendationTargetType.CONTENT, request.getTargetId());
        }
        if (liked && notificationService != null) {
            if (TARGET_CONTENT.equals(request.getTargetType())) notificationService.notifyContentLiked(userId, request.getTargetId());
            else if (TARGET_COMMENT.equals(request.getTargetType())) notificationService.notifyCommentLiked(userId, request.getTargetId());
        }
        return response;
    }

    @Transactional(rollbackFor = Exception.class)
    public ToggleFavoriteResponse toggleFavorite(ToggleFavoriteRequest request) {
        Long userId = CurrentUserHolder.get().getUserId();
        if (!TARGET_CONTENT.equals(request.getTargetType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "unsupported targetType");
        }
        contentService.requireVisibleContent(request.getTargetId());
        FavoriteRecord record = findFavorite(userId, request.getTargetType(), request.getTargetId());
        boolean favorited;
        if (record == null) {
            FavoriteRecord created = new FavoriteRecord();
            created.setUserId(userId);
            created.setTargetType(request.getTargetType());
            created.setTargetId(request.getTargetId());
            created.setStatus(STATUS_ACTIVE);
            favoriteRecordMapper.insert(created);
            contentMapper.update(null, new UpdateWrapper<Content>()
                    .eq("id", request.getTargetId())
                    .setSql("favorite_count = favorite_count + 1"));
            favorited = true;
        } else if (STATUS_ACTIVE.equals(record.getStatus())) {
            favoriteRecordMapper.update(null, new UpdateWrapper<FavoriteRecord>()
                    .eq("id", record.getId())
                    .set("status", STATUS_CANCELLED));
            contentMapper.update(null, new UpdateWrapper<Content>()
                    .eq("id", request.getTargetId())
                    .setSql("favorite_count = GREATEST(favorite_count - 1, 0)"));
            favorited = false;
        } else {
            favoriteRecordMapper.update(null, new UpdateWrapper<FavoriteRecord>()
                    .eq("id", record.getId())
                    .set("status", STATUS_ACTIVE));
            contentMapper.update(null, new UpdateWrapper<Content>()
                    .eq("id", request.getTargetId())
                    .setSql("favorite_count = favorite_count + 1"));
            favorited = true;
        }
        ToggleFavoriteResponse response = new ToggleFavoriteResponse();
        response.setTargetType(request.getTargetType());
        response.setTargetId(request.getTargetId());
        response.setFavorited(favorited);
        if (favorited && recommendationBehaviorService != null) {
            recommendationBehaviorService.recordInteractionSafely(userId, RecommendationBehaviorType.FAVORITE,
                    RecommendationTargetType.CONTENT, request.getTargetId());
        }
        return response;
    }

    private void validateLikeTarget(String targetType, Long targetId) {
        if (TARGET_CONTENT.equals(targetType)) {
            contentService.requireVisibleContent(targetId);
            return;
        }
        if (TARGET_COMMENT.equals(targetType)) {
            Comment comment = commentMapper.selectById(targetId);
            if (comment == null || Integer.valueOf(1).equals(comment.getIsDeleted()) || !STATUS_ACTIVE.equals(comment.getStatus())) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "comment not found");
            }
            return;
        }
        throw new BusinessException(ErrorCode.PARAM_ERROR, "unsupported targetType");
    }

    private LikeRecord findLike(Long userId, String targetType, Long targetId) {
        return likeRecordMapper.selectOne(new QueryWrapper<LikeRecord>()
                .eq("user_id", userId)
                .eq("target_type", targetType)
                .eq("target_id", targetId)
                .last("LIMIT 1"));
    }

    private FavoriteRecord findFavorite(Long userId, String targetType, Long targetId) {
        return favoriteRecordMapper.selectOne(new QueryWrapper<FavoriteRecord>()
                .eq("user_id", userId)
                .eq("target_type", targetType)
                .eq("target_id", targetId)
                .last("LIMIT 1"));
    }

    private void incrementLikeCount(String targetType, Long targetId) {
        if (TARGET_CONTENT.equals(targetType)) {
            contentMapper.update(null, new UpdateWrapper<Content>()
                    .eq("id", targetId)
                    .setSql("like_count = like_count + 1"));
        } else {
            commentMapper.update(null, new UpdateWrapper<Comment>()
                    .eq("id", targetId)
                    .setSql("like_count = like_count + 1"));
        }
    }

    private void decrementLikeCount(String targetType, Long targetId) {
        if (TARGET_CONTENT.equals(targetType)) {
            contentMapper.update(null, new UpdateWrapper<Content>()
                    .eq("id", targetId)
                    .setSql("like_count = GREATEST(like_count - 1, 0)"));
        } else {
            commentMapper.update(null, new UpdateWrapper<Comment>()
                    .eq("id", targetId)
                    .setSql("like_count = GREATEST(like_count - 1, 0)"));
        }
    }
}
