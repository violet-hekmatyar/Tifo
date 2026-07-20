package com.southstand.content.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.southstand.auth.security.CurrentUserHolder;
import com.southstand.auth.security.LoginUserContext;
import com.southstand.common.enums.ErrorCode;
import com.southstand.common.exception.BusinessException;
import com.southstand.content.dto.ContentRelationRequest;
import com.southstand.content.dto.ArticleBlockRequest;
import com.southstand.content.dto.ArticleRequest;
import com.southstand.content.dto.CreatePostRequest;
import com.southstand.content.entity.Content;
import com.southstand.content.entity.ContentBlock;
import com.southstand.content.entity.ContentMedia;
import com.southstand.content.entity.ContentRelation;
import com.southstand.content.mapper.ContentBlockMapper;
import com.southstand.content.mapper.ContentMapper;
import com.southstand.content.mapper.ContentMediaMapper;
import com.southstand.content.mapper.ContentRelationMapper;
import com.southstand.content.vo.ArticleBlockVO;
import com.southstand.content.vo.AuthorVO;
import com.southstand.content.vo.ContentDetailVO;
import com.southstand.content.vo.CreatePostResponse;
import com.southstand.content.vo.MediaVO;
import com.southstand.content.vo.RelationVO;
import com.southstand.football.player.entity.FootballPlayer;
import com.southstand.football.player.mapper.FootballPlayerMapper;
import com.southstand.football.team.entity.FootballTeam;
import com.southstand.football.team.mapper.FootballTeamMapper;
import com.southstand.football.match.entity.MatchInfo;
import com.southstand.football.match.mapper.MatchInfoMapper;
import com.southstand.file.domain.FileResourceEntity;
import com.southstand.file.service.FileBindingService;
import com.southstand.interaction.entity.FavoriteRecord;
import com.southstand.interaction.entity.LikeRecord;
import com.southstand.interaction.mapper.FavoriteRecordMapper;
import com.southstand.interaction.mapper.LikeRecordMapper;
import com.southstand.user.entity.SysUser;
import com.southstand.user.entity.UserProfile;
import com.southstand.user.mapper.SysUserMapper;
import com.southstand.user.mapper.UserProfileMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContentService {

    public static final String TARGET_CONTENT = "CONTENT";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_PUBLISHED = "PUBLISHED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    private final ContentMapper contentMapper;
    private final ContentBlockMapper contentBlockMapper;
    private final ContentMediaMapper contentMediaMapper;
    private final ContentRelationMapper contentRelationMapper;
    private final LikeRecordMapper likeRecordMapper;
    private final FavoriteRecordMapper favoriteRecordMapper;
    private final SysUserMapper sysUserMapper;
    private final UserProfileMapper userProfileMapper;
    private final FootballTeamMapper footballTeamMapper;
    private final FootballPlayerMapper footballPlayerMapper;
    private final MatchInfoMapper matchInfoMapper;
    private final FileBindingService fileBindingService;

    public ContentService(ContentMapper contentMapper,
            ContentBlockMapper contentBlockMapper,
            ContentMediaMapper contentMediaMapper,
            ContentRelationMapper contentRelationMapper,
            LikeRecordMapper likeRecordMapper,
            FavoriteRecordMapper favoriteRecordMapper,
            SysUserMapper sysUserMapper,
            UserProfileMapper userProfileMapper,
            FootballTeamMapper footballTeamMapper,
            FootballPlayerMapper footballPlayerMapper,
            MatchInfoMapper matchInfoMapper,
            FileBindingService fileBindingService) {
        this.contentMapper = contentMapper;
        this.contentBlockMapper = contentBlockMapper;
        this.contentMediaMapper = contentMediaMapper;
        this.contentRelationMapper = contentRelationMapper;
        this.likeRecordMapper = likeRecordMapper;
        this.favoriteRecordMapper = favoriteRecordMapper;
        this.sysUserMapper = sysUserMapper;
        this.userProfileMapper = userProfileMapper;
        this.footballTeamMapper = footballTeamMapper;
        this.footballPlayerMapper = footballPlayerMapper;
        this.matchInfoMapper = matchInfoMapper;
        this.fileBindingService = fileBindingService;
    }

    public ContentDetailVO detail(Long contentId) {
        Content content = requireVisibleContent(contentId);
        try {
            contentMapper.update(null, new UpdateWrapper<Content>()
                    .eq("id", contentId)
                    .setSql("view_count = view_count + 1"));
            content.setViewCount(nvl(content.getViewCount()) + 1);
        } catch (RuntimeException ignored) {
            // View count is best-effort and must not break content reading.
        }
        Long userId = currentUserIdOrNull();
        return toDetail(content, userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public CreatePostResponse createPost(CreatePostRequest request) {
        Long userId = CurrentUserHolder.get().getUserId();
        List<FileResourceEntity> mediaFiles = fileBindingService.validateContentImages(userId, request.getMediaFileIds());
        if (isBlank(request.getBody()) && isEmpty(request.getMediaUrls()) && mediaFiles.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "body or media required");
        }
        List<ContentRelationRequest> relations = validateAndDeduplicateRelations(request.getRelationList());

        LocalDateTime now = LocalDateTime.now();
        Content content = new Content();
        content.setContentType("POST");
        content.setContentFormat("POST_FORMAT");
        content.setCardType("CONTENT_CARD");
        content.setTitle(request.getTitle().trim());
        content.setSummary(summaryOf(request.getBody()));
        content.setBody(request.getBody());
        content.setCoverUrl(firstMediaUrl(request.getMediaUrls(), mediaFiles));
        content.setAuthorId(userId);
        content.setSourceType("USER");
        content.setIsOfficial(0);
        content.setViewCount(0);
        content.setLikeCount(0);
        content.setCommentCount(0);
        content.setFavoriteCount(0);
        content.setHotScore(BigDecimal.ZERO);
        content.setPublishTime(now);
        content.setStatus(STATUS_PUBLISHED);
        content.setIsDeleted(0);
        contentMapper.insert(content);

        insertMedia(content.getId(), request.getMediaUrls(), mediaFiles);
        insertRelations(content.getId(), relations);
        userProfileMapper.update(null, new UpdateWrapper<UserProfile>()
                .eq("user_id", userId)
                .setSql("post_count = post_count + 1"));

        CreatePostResponse response = new CreatePostResponse();
        response.setContentId(content.getId());
        response.setContentType(content.getContentType());
        response.setTitle(content.getTitle());
        response.setPublishTime(content.getPublishTime());
        return response;
    }

    @Transactional(rollbackFor = Exception.class)
    public CreatePostResponse createArticle(ArticleRequest request) {
        Long userId = CurrentUserHolder.get().getUserId();
        ArticlePayload payload = validateArticlePayload(userId, request);
        LocalDateTime now = LocalDateTime.now();

        Content content = new Content();
        content.setContentType("ARTICLE");
        content.setContentFormat("ARTICLE_BLOCKS");
        content.setCardType("CONTENT_CARD");
        content.setTitle(payload.title);
        content.setSummary(payload.summary);
        content.setBody(firstText(payload.blocks));
        content.setCoverUrl(payload.coverFile == null ? firstImageUrl(payload.blocks) : payload.coverFile.getUrl());
        content.setAuthorId(userId);
        content.setSourceType("USER");
        content.setIsOfficial(0);
        content.setViewCount(0);
        content.setLikeCount(0);
        content.setCommentCount(0);
        content.setFavoriteCount(0);
        content.setHotScore(BigDecimal.ZERO);
        content.setPublishTime(now);
        content.setStatus(STATUS_PUBLISHED);
        content.setIsDeleted(0);
        contentMapper.insert(content);

        insertArticleBlocks(content.getId(), payload.blocks);
        insertArticleMedia(content.getId(), payload.coverFile, payload.blocks);
        insertRelations(content.getId(), payload.relations);
        userProfileMapper.update(null, new UpdateWrapper<UserProfile>()
                .eq("user_id", userId)
                .setSql("post_count = post_count + 1"));

        CreatePostResponse response = new CreatePostResponse();
        response.setContentId(content.getId());
        response.setContentType(content.getContentType());
        response.setTitle(content.getTitle());
        response.setPublishTime(content.getPublishTime());
        return response;
    }

    @Transactional(rollbackFor = Exception.class)
    public ContentDetailVO updateArticle(Long contentId, ArticleRequest request) {
        Long userId = CurrentUserHolder.get().getUserId();
        Content content = requireVisibleContent(contentId);
        if (!"ARTICLE".equals(content.getContentType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "content is not ARTICLE");
        }
        LoginUserContext current = CurrentUserHolder.get();
        if (!userId.equals(content.getAuthorId()) && !"ADMIN".equals(current.getRoleType())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        ArticlePayload payload = validateArticlePayload(userId, request);
        contentMapper.update(null, new UpdateWrapper<Content>()
                .eq("id", contentId)
                .set("title", payload.title)
                .set("summary", payload.summary)
                .set("body", firstText(payload.blocks))
                .set("cover_url", payload.coverFile == null ? firstImageUrl(payload.blocks) : payload.coverFile.getUrl()));

        contentBlockMapper.delete(new QueryWrapper<ContentBlock>().eq("content_id", contentId));
        contentMediaMapper.delete(new QueryWrapper<ContentMedia>().eq("content_id", contentId));
        contentRelationMapper.delete(new QueryWrapper<ContentRelation>().eq("content_id", contentId));

        insertArticleBlocks(contentId, payload.blocks);
        insertArticleMedia(contentId, payload.coverFile, payload.blocks);
        insertRelations(contentId, payload.relations);
        return detail(contentId);
    }

    public Content requireVisibleContent(Long contentId) {
        Content content = contentMapper.selectById(contentId);
        if (content == null || Integer.valueOf(1).equals(content.getIsDeleted())
                || !(STATUS_PUBLISHED.equals(content.getStatus()) || STATUS_ACTIVE.equals(content.getStatus()))) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "content not found");
        }
        return content;
    }

    public AuthorVO authorOf(Long userId) {
        AuthorVO author = new AuthorVO();
        author.setUserId(userId);
        author.setNickname("user-" + userId);
        author.setVerified(false);

        UserProfile profile = userProfileMapper.selectOne(new QueryWrapper<UserProfile>()
                .eq("user_id", userId)
                .eq("is_deleted", 0)
                .last("LIMIT 1"));
        if (profile != null) {
            author.setNickname(profile.getNickname());
            author.setAvatarUrl(profile.getAvatarUrl());
        }
        SysUser user = sysUserMapper.selectById(userId);
        author.setVerified(user != null && "ADMIN".equals(user.getRoleType()));
        return author;
    }

    private ContentDetailVO toDetail(Content content, Long currentUserId) {
        ContentDetailVO vo = new ContentDetailVO();
        vo.setContentId(content.getId());
        vo.setContentType(content.getContentType());
        vo.setContentFormat(content.getContentFormat());
        vo.setCardType(content.getCardType());
        vo.setTitle(content.getTitle());
        vo.setSummary(content.getSummary());
        vo.setBody(content.getBody());
        vo.setCoverUrl(content.getCoverUrl());
        vo.setAuthor(authorOf(content.getAuthorId()));
        vo.setBlocks(blocks(content.getId()));
        vo.setMediaList(mediaList(content.getId()));
        vo.setRelationList(relationList(content.getId()));
        vo.setLikeCount(nvl(content.getLikeCount()));
        vo.setCommentCount(nvl(content.getCommentCount()));
        vo.setFavoriteCount(nvl(content.getFavoriteCount()));
        vo.setViewCount(nvl(content.getViewCount()));
        vo.setLiked(isActiveLike(currentUserId, TARGET_CONTENT, content.getId()));
        vo.setFavorited(isActiveFavorite(currentUserId, TARGET_CONTENT, content.getId()));
        vo.setPublishTime(content.getPublishTime());
        return vo;
    }

    private List<MediaVO> mediaList(Long contentId) {
        List<ContentMedia> mediaList = contentMediaMapper.selectList(new QueryWrapper<ContentMedia>()
                .eq("content_id", contentId)
                .eq("status", STATUS_ACTIVE)
                .eq("is_deleted", 0)
                .orderByAsc("sort_order", "id"));
        List<MediaVO> result = new ArrayList<>();
        for (ContentMedia media : mediaList) {
            MediaVO vo = new MediaVO();
            vo.setMediaId(media.getId());
            vo.setMediaType(media.getMediaType());
            vo.setMediaUrl(media.getMediaUrl());
            vo.setThumbnailUrl(media.getThumbnailUrl());
            vo.setWidth(media.getWidth());
            vo.setHeight(media.getHeight());
            result.add(vo);
        }
        return result;
    }

    private List<RelationVO> relationList(Long contentId) {
        List<ContentRelation> relations = contentRelationMapper.selectList(new QueryWrapper<ContentRelation>()
                .eq("content_id", contentId)
                .eq("status", STATUS_ACTIVE)
                .eq("is_deleted", 0)
                .orderByAsc("id"));
        List<RelationVO> result = new ArrayList<>();
        for (ContentRelation relation : relations) {
            RelationVO vo = new RelationVO();
            vo.setRelationType(relation.getRelationType());
            vo.setRelationId(relation.getRelationId());
            vo.setRelationName(resolveRelationName(relation.getRelationType(), relation.getRelationId()));
            result.add(vo);
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

    private boolean isActiveFavorite(Long userId, String targetType, Long targetId) {
        if (userId == null) {
            return false;
        }
        FavoriteRecord record = favoriteRecordMapper.selectOne(new QueryWrapper<FavoriteRecord>()
                .eq("user_id", userId)
                .eq("target_type", targetType)
                .eq("target_id", targetId)
                .last("LIMIT 1"));
        return record != null && STATUS_ACTIVE.equals(record.getStatus());
    }

    private List<ContentRelationRequest> validateAndDeduplicateRelations(List<ContentRelationRequest> relations) {
        if (relations == null || relations.isEmpty()) {
            return List.of();
        }
        Map<String, ContentRelationRequest> deduped = new LinkedHashMap<>();
        for (ContentRelationRequest relation : relations) {
            if (relation == null || isBlank(relation.getRelationType()) || relation.getRelationId() == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "invalid relation");
            }
            String type = relation.getRelationType().trim().toUpperCase();
            if ("TEAM".equals(type)) {
                FootballTeam team = footballTeamMapper.selectById(relation.getRelationId());
                if (team == null || !"ACTIVE".equals(team.getStatus()) || Integer.valueOf(1).equals(team.getIsDeleted())) {
                    throw new BusinessException(ErrorCode.NOT_FOUND, "team not found");
                }
            } else if ("PLAYER".equals(type)) {
                FootballPlayer player = footballPlayerMapper.selectById(relation.getRelationId());
                if (player == null || !"ACTIVE".equals(player.getStatus()) || Integer.valueOf(1).equals(player.getIsDeleted())) {
                    throw new BusinessException(ErrorCode.NOT_FOUND, "player not found");
                }
            } else if ("MATCH".equals(type)) {
                MatchInfo match = matchInfoMapper.selectById(relation.getRelationId());
                if (match == null || Integer.valueOf(1).equals(match.getIsDeleted())) {
                    throw new BusinessException(ErrorCode.NOT_FOUND, "match not found");
                }
            } else {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "unsupported relation type");
            }
            ContentRelationRequest normalized = new ContentRelationRequest();
            normalized.setRelationType(type);
            normalized.setRelationId(relation.getRelationId());
            deduped.putIfAbsent(type + ":" + relation.getRelationId(), normalized);
        }
        return new ArrayList<>(deduped.values());
    }

    private void insertMedia(Long contentId, List<String> mediaUrls, List<FileResourceEntity> mediaFiles) {
        int sortOrder = 1;
        if (mediaUrls != null) {
            for (String url : mediaUrls) {
                if (isBlank(url)) {
                    continue;
                }
                insertMediaUrl(contentId, url, sortOrder++);
            }
        }
        if (mediaFiles != null) {
            for (FileResourceEntity file : mediaFiles) {
                insertMediaUrl(contentId, file.getUrl(), sortOrder++);
            }
        }
    }

    private void insertMediaUrl(Long contentId, String url, int sortOrder) {
        if (isBlank(url)) {
            return;
        }
        ContentMedia media = new ContentMedia();
        media.setContentId(contentId);
        media.setMediaType("IMAGE");
        media.setMediaUrl(url);
        media.setSortOrder(sortOrder);
        media.setStatus(STATUS_ACTIVE);
        media.setIsDeleted(0);
        contentMediaMapper.insert(media);
    }

    private boolean isEmpty(List<?> values) {
        return values == null || values.isEmpty();
    }

    private static String firstMediaUrl(List<String> mediaUrls, List<FileResourceEntity> mediaFiles) {
        if (mediaUrls != null) {
            for (String url : mediaUrls) {
                if (!isBlank(url)) {
                    return url;
                }
            }
        }
        if (mediaFiles != null && !mediaFiles.isEmpty()) {
            return mediaFiles.get(0).getUrl();
        }
        return null;
    }

    private void insertRelations(Long contentId, List<ContentRelationRequest> relations) {
        if (relations == null || relations.isEmpty()) {
            return;
        }
        for (ContentRelationRequest request : relations) {
            ContentRelation relation = new ContentRelation();
            relation.setContentId(contentId);
            relation.setRelationType(request.getRelationType());
            relation.setRelationId(request.getRelationId());
            relation.setConfidence(BigDecimal.ONE);
            relation.setSourceType("USER");
            relation.setStatus(STATUS_ACTIVE);
            relation.setIsDeleted(0);
            contentRelationMapper.insert(relation);
        }
    }

    private List<ArticleBlockVO> blocks(Long contentId) {
        List<ContentBlock> blocks = contentBlockMapper.selectList(new QueryWrapper<ContentBlock>()
                .eq("content_id", contentId)
                .eq("status", STATUS_ACTIVE)
                .eq("is_deleted", 0)
                .orderByAsc("sort_order")
                .orderByAsc("id"));
        List<ArticleBlockVO> result = new ArrayList<>();
        for (ContentBlock block : blocks) {
            ArticleBlockVO vo = new ArticleBlockVO();
            vo.setBlockId(block.getId());
            vo.setBlockType(block.getBlockType());
            vo.setText(block.getTextContent());
            vo.setMediaFileId(block.getMediaFileId());
            vo.setMediaUrl(block.getMediaUrl());
            vo.setSortOrder(block.getSortOrder());
            result.add(vo);
        }
        return result;
    }

    private ArticlePayload validateArticlePayload(Long userId, ArticleRequest request) {
        if (request == null || isBlank(request.getTitle())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "title required");
        }
        String title = request.getTitle().trim();
        if (title.length() > 200) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "title length must be 1-200");
        }
        String summary = request.getSummary() == null ? null : request.getSummary().trim();
        if (summary != null && summary.length() > 500) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "summary length must be 0-500");
        }
        if (request.getBlocks() == null || request.getBlocks().isEmpty() || request.getBlocks().size() > 100) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "blocks size must be 1-100");
        }
        FileResourceEntity coverFile = request.getCoverFileId() == null ? null : fileBindingService.validateContentImage(userId, request.getCoverFileId());
        List<ResolvedArticleBlock> blocks = new ArrayList<>();
        Set<Integer> sortOrders = new HashSet<>();
        int imageCount = 0;
        for (ArticleBlockRequest block : request.getBlocks()) {
            if (block == null || isBlank(block.getBlockType()) || block.getSortOrder() == null || block.getSortOrder() <= 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "invalid block");
            }
            if (!sortOrders.add(block.getSortOrder())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "duplicate sortOrder");
            }
            String type = block.getBlockType().trim().toUpperCase();
            if ("TEXT".equals(type)) {
                if (isBlank(block.getText()) || block.getText().trim().length() > 10000 || block.getMediaFileId() != null) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "invalid TEXT block");
                }
                blocks.add(new ResolvedArticleBlock(type, block.getText().trim(), null, null, block.getSortOrder()));
            } else if ("IMAGE".equals(type)) {
                imageCount++;
                if (imageCount > 30 || block.getMediaFileId() == null) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "invalid IMAGE block");
                }
                FileResourceEntity file = fileBindingService.validateContentImage(userId, block.getMediaFileId());
                blocks.add(new ResolvedArticleBlock(type, null, file.getId(), file.getUrl(), block.getSortOrder()));
            } else {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "unsupported blockType");
            }
        }
        blocks.sort(Comparator.comparing(ResolvedArticleBlock::sortOrder));
        return new ArticlePayload(title, summary, coverFile, blocks, validateAndDeduplicateRelations(request.getRelationList()));
    }

    private void insertArticleBlocks(Long contentId, List<ResolvedArticleBlock> blocks) {
        for (ResolvedArticleBlock block : blocks) {
            ContentBlock entity = new ContentBlock();
            entity.setContentId(contentId);
            entity.setBlockType(block.blockType());
            entity.setTextContent(block.text());
            entity.setMediaFileId(block.mediaFileId());
            entity.setMediaUrl(block.mediaUrl());
            entity.setSortOrder(block.sortOrder());
            entity.setStatus(STATUS_ACTIVE);
            entity.setIsDeleted(0);
            contentBlockMapper.insert(entity);
        }
    }

    private void insertArticleMedia(Long contentId, FileResourceEntity coverFile, List<ResolvedArticleBlock> blocks) {
        int sortOrder = 1;
        if (coverFile != null) {
            insertMediaUrl(contentId, coverFile.getUrl(), sortOrder++);
        }
        for (ResolvedArticleBlock block : blocks) {
            if ("IMAGE".equals(block.blockType()) && !isBlank(block.mediaUrl())) {
                insertMediaUrl(contentId, block.mediaUrl(), sortOrder++);
            }
        }
    }

    private static String firstText(List<ResolvedArticleBlock> blocks) {
        for (ResolvedArticleBlock block : blocks) {
            if ("TEXT".equals(block.blockType()) && !isBlank(block.text())) {
                return block.text().length() <= 10000 ? block.text() : block.text().substring(0, 10000);
            }
        }
        return null;
    }

    private static String firstImageUrl(List<ResolvedArticleBlock> blocks) {
        for (ResolvedArticleBlock block : blocks) {
            if ("IMAGE".equals(block.blockType()) && !isBlank(block.mediaUrl())) {
                return block.mediaUrl();
            }
        }
        return null;
    }

    private String resolveRelationName(String relationType, Long relationId) {
        try {
            if ("TEAM".equals(relationType)) {
                FootballTeam team = footballTeamMapper.selectById(relationId);
                return team == null ? null : team.getTeamName();
            }
            if ("PLAYER".equals(relationType)) {
                FootballPlayer player = footballPlayerMapper.selectById(relationId);
                return player == null ? null : player.getPlayerName();
            }
            if ("MATCH".equals(relationType)) {
                return "match-" + relationId;
            }
        } catch (RuntimeException ignored) {
            return null;
        }
        return null;
    }

    private Long currentUserIdOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUserContext context) {
            return context.getUserId();
        }
        return null;
    }

    private static int nvl(Integer value) {
        return value == null ? 0 : value;
    }

    private static String summaryOf(String body) {
        if (isBlank(body)) {
            return null;
        }
        String text = body.trim();
        return text.length() <= 120 ? text : text.substring(0, 120);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private record ResolvedArticleBlock(String blockType, String text, Long mediaFileId, String mediaUrl, Integer sortOrder) {
    }

    private record ArticlePayload(String title, String summary, FileResourceEntity coverFile, List<ResolvedArticleBlock> blocks, List<ContentRelationRequest> relations) {
    }
}
