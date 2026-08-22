package com.southstand.card.vo;

import com.southstand.content.vo.AuthorVO;
import java.math.BigDecimal;

public record HotCommentCardPayload(Long commentId, Long contentId, String commentText,
                                    AuthorVO commentAuthor, int likeCount, int replyCount,
                                    BigDecimal hotScore, String contentTitle,
                                    String contentType) implements HomeCardPayload {
}
