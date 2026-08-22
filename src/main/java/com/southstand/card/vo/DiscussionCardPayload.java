package com.southstand.card.vo;

import com.southstand.content.vo.AuthorVO;
import com.southstand.interaction.vo.HotCommentVO;
import java.time.LocalDateTime;
import java.util.List;

public record DiscussionCardPayload(Long contentId, String title, String summary, AuthorVO author,
                                    LocalDateTime publishTime, int commentCount, int likeCount,
                                    int favoriteCount, HotCommentVO hotComment,
                                    List<RelationTagVO> relationTags) implements HomeCardPayload {
}
