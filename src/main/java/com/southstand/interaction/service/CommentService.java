package com.southstand.interaction.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.southstand.auth.security.CurrentUserHolder;
import com.southstand.auth.security.LoginUserContext;
import com.southstand.common.enums.ErrorCode;
import com.southstand.common.exception.BusinessException;
import com.southstand.common.result.PageResult;
import com.southstand.content.entity.Content;
import com.southstand.content.mapper.ContentMapper;
import com.southstand.content.service.ContentService;
import com.southstand.interaction.dto.CreateCommentRequest;
import com.southstand.interaction.entity.Comment;
import com.southstand.interaction.entity.LikeRecord;
import com.southstand.interaction.mapper.CommentMapper;
import com.southstand.interaction.mapper.LikeRecordMapper;
import com.southstand.interaction.vo.CommentVO;
import com.southstand.interaction.vo.CreateCommentResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {

    public static final String TARGET_CONTENT = "CONTENT";
    public static final String STATUS_ACTIVE = "ACTIVE";

    private final CommentMapper commentMapper;
    private final ContentMapper contentMapper;
    private final LikeRecordMapper likeRecordMapper;
    private final ContentService contentService;

    public CommentService(CommentMapper commentMapper,
            ContentMapper contentMapper,
            LikeRecordMapper likeRecordMapper,
            ContentService contentService) {
        this.commentMapper = commentMapper;
        this.contentMapper = contentMapper;
        this.likeRecordMapper = likeRecordMapper;
        this.contentService = contentService;
    }

    public PageResult<CommentVO> list(String targetType, Long targetId, String sort, Long parentId, long pageNum, long pageSize) {
        validateTargetType(targetType);
        contentService.requireVisibleContent(targetId);
        long safePageNum = pageNum <= 0 ? 1 : pageNum;
        long safePageSize = Math.min(Math.max(pageSize <= 0 ? 10 : pageSize, 1), 100);
        long actualParentId = parentId == null ? 0L : parentId;

        List<Comment> comments = commentMapper.selectList(new QueryWrapper<Comment>()
                .eq("target_type", targetType)
                .eq("target_id", targetId)
                .eq("parent_id", actualParentId)
                .eq("status", STATUS_ACTIVE)
                .eq("is_deleted", 0)
                .orderByDesc("create_time"));
        if (actualParentId == 0 && !"time".equalsIgnoreCase(sort)) {
            LocalDateTime now = LocalDateTime.now();
            comments.sort(Comparator
                    .comparing((Comment c) -> calculateHotScore(nvl(c.getLikeCount()), nvl(c.getReplyCount()), c.getCreateTime(), now))
                    .reversed()
                    .thenComparing(Comment::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder())));
        }

        int from = (int) Math.min((safePageNum - 1) * safePageSize, comments.size());
        int to = (int) Math.min(from + safePageSize, comments.size());
        Long userId = currentUserIdOrNull();
        List<CommentVO> records = new ArrayList<>();
        for (Comment comment : comments.subList(from, to)) {
            records.add(toVO(comment, userId, actualParentId == 0));
        }
        return PageResult.of(records, comments.size(), safePageNum, safePageSize);
    }

    @Transactional(rollbackFor = Exception.class)
    public CreateCommentResponse create(CreateCommentRequest request) {
        validateTargetType(request.getTargetType());
        contentService.requireVisibleContent(request.getTargetId());
        Long userId = CurrentUserHolder.get().getUserId();
        Long parentId = request.getParentId() == null ? 0L : request.getParentId();

        Comment parent = null;
        if (parentId > 0) {
            parent = commentMapper.selectById(parentId);
            if (parent == null || Integer.valueOf(1).equals(parent.getIsDeleted())
                    || !STATUS_ACTIVE.equals(parent.getStatus())
                    || !request.getTargetType().equals(parent.getTargetType())
                    || !request.getTargetId().equals(parent.getTargetId())) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "parent comment not found");
            }
            if (parent.getParentId() != null && parent.getParentId() > 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "only two comment levels are supported");
            }
        }

        Comment comment = new Comment();
        LocalDateTime now = LocalDateTime.now();
        comment.setTargetType(request.getTargetType());
        comment.setTargetId(request.getTargetId());
        comment.setParentId(parentId);
        comment.setUserId(userId);
        comment.setContentText(request.getContentText().trim());
        comment.setLikeCount(0);
        comment.setReplyCount(0);
        comment.setHotScore(BigDecimal.ZERO);
        comment.setIsTop(0);
        comment.setStatus(STATUS_ACTIVE);
        comment.setIsDeleted(0);
        comment.setCreateTime(now);
        commentMapper.insert(comment);

        contentMapper.update(null, new UpdateWrapper<Content>()
                .eq("id", request.getTargetId())
                .setSql("comment_count = comment_count + 1"));
        if (parent != null) {
            commentMapper.update(null, new UpdateWrapper<Comment>()
                    .eq("id", parentId)
                    .setSql("reply_count = reply_count + 1"));
        }

        CreateCommentResponse response = new CreateCommentResponse();
        response.setCommentId(comment.getId());
        response.setParentId(parentId);
        response.setCreateTime(comment.getCreateTime());
        return response;
    }

    public static BigDecimal calculateHotScore(int likeCount, int replyCount, LocalDateTime createTime, LocalDateTime now) {
        int base = likeCount + replyCount * 2;
        double coefficient = 0.3;
        if (createTime != null && now != null) {
            long hours = Math.max(Duration.between(createTime, now).toHours(), 0);
            if (hours < 2) {
                coefficient = 1.5;
            } else if (hours < 12) {
                coefficient = 1.0;
            } else if (hours < 24) {
                coefficient = 0.7;
            }
        }
        return BigDecimal.valueOf(base * coefficient).setScale(2, RoundingMode.HALF_UP);
    }

    private CommentVO toVO(Comment comment, Long currentUserId, boolean includeReplies) {
        CommentVO vo = new CommentVO();
        vo.setCommentId(comment.getId());
        vo.setTargetType(comment.getTargetType());
        vo.setTargetId(comment.getTargetId());
        vo.setParentId(comment.getParentId());
        vo.setAuthor(contentService.authorOf(comment.getUserId()));
        vo.setContentText(comment.getContentText());
        vo.setLikeCount(nvl(comment.getLikeCount()));
        vo.setReplyCount(nvl(comment.getReplyCount()));
        vo.setHotScore(calculateHotScore(nvl(comment.getLikeCount()), nvl(comment.getReplyCount()), comment.getCreateTime(), LocalDateTime.now()));
        vo.setLiked(isActiveLike(currentUserId, "COMMENT", comment.getId()));
        vo.setCreateTime(comment.getCreateTime());
        if (includeReplies) {
            vo.setReplies(replyVOs(comment.getId(), currentUserId));
        } else {
            vo.setReplies(List.of());
        }
        return vo;
    }

    private List<CommentVO> replyVOs(Long parentId, Long currentUserId) {
        List<Comment> replies = commentMapper.selectList(new QueryWrapper<Comment>()
                .eq("parent_id", parentId)
                .eq("status", STATUS_ACTIVE)
                .eq("is_deleted", 0)
                .orderByAsc("create_time")
                .last("LIMIT 3"));
        List<CommentVO> result = new ArrayList<>();
        for (Comment reply : replies) {
            result.add(toVO(reply, currentUserId, false));
        }
        return result;
    }

    private boolean isActiveLike(Long userId, String targetType, Long targetId) {
        if (userId == null) {
            return false;
        }
        LikeRecord record = likeRecordMapper.selectOne(new QueryWrapper<LikeRecord>()
                .eq("user_id", userId)
                .eq("target_type", targetType)
                .eq("target_id", targetId)
                .last("LIMIT 1"));
        return record != null && STATUS_ACTIVE.equals(record.getStatus());
    }

    private Long currentUserIdOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUserContext context) {
            return context.getUserId();
        }
        return null;
    }

    private void validateTargetType(String targetType) {
        if (!TARGET_CONTENT.equals(targetType)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "unsupported targetType");
        }
    }

    private static int nvl(Integer value) {
        return value == null ? 0 : value;
    }
}
