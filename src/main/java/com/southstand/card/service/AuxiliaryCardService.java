package com.southstand.card.service;

import com.southstand.card.model.HomeFeedUserContext;
import com.southstand.card.vo.FeedCardVO;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AuxiliaryCardService {
    private final DiscussionCardService discussion;
    private final HotCommentCardService hotComment;
    private final RankingCardService ranking;
    private final PlayerRatingCardService playerRating;

    public AuxiliaryCardService(DiscussionCardService discussion, HotCommentCardService hotComment,
                                RankingCardService ranking, PlayerRatingCardService playerRating) {
        this.discussion = discussion; this.hotComment = hotComment;
        this.ranking = ranking; this.playerRating = playerRating;
    }

    public List<FeedCardVO> candidates(HomeFeedUserContext user) {
        List<FeedCardVO> result = new ArrayList<>();
        takeFirst(result, discussion.candidates(user));
        takeFirst(result, hotComment.candidates(user));
        result.addAll(ranking.candidates(user));
        takeFirst(result, playerRating.candidates(user));
        return result;
    }

    private void takeFirst(List<FeedCardVO> target, List<FeedCardVO> source) {
        if (source != null && !source.isEmpty()) target.add(source.get(0));
    }
}
