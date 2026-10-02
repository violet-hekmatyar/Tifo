package com.southstand.card;

import static org.assertj.core.api.Assertions.assertThat;

import com.southstand.card.config.HomeCardProperties;
import com.southstand.card.service.HomeFeedCompositionService;
import com.southstand.card.vo.FeedCardVO;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class HomeFeedCompositionServiceTests {
    @Test
    void sparseInsertRespectsRatioGapDedupAttributionAndStableKeys() {
        HomeCardProperties p = new HomeCardProperties(); p.setMaxRatio(.2); p.setMinGap(3);
        List<FeedCardVO> core = new ArrayList<>(); for (long i=1;i<=30;i++) core.add(content(i));
        List<FeedCardVO> aux = List.of(discussion(2L), aux("HOT_COMMENT","HOT_COMMENT:7"),
                aux("RANKING","RANKING:STANDING:1"), aux("PLAYER_RATING","PLAYER_RATING:9"));
        var result = new HomeFeedCompositionService(p).compose(core, aux, "request");
        assertThat(result).noneMatch(c -> "CONTENT".equals(c.getCardType()) && Long.valueOf(2L).equals(c.getContentId()));
        long auxCount = result.stream().filter(c -> !List.of("CONTENT","MATCH").contains(c.getCardType())).count();
        assertThat((double) auxCount / result.size()).isLessThanOrEqualTo(.2);
        assertThat(result).extracting(FeedCardVO::getPosition).containsExactlyElementsOf(java.util.stream.IntStream.range(0,result.size()).boxed().toList());
        assertThat(result).allMatch(c -> c.getImpressionId().startsWith("request:") && c.getCardKey()!=null);
        assertThat(new HashSet<>(result.stream().map(FeedCardVO::getImpressionId).toList())).hasSize(result.size());
        assertThat(p.getMinGap()).isEqualTo(3);
        assertThat(result).hasSize(30 + 4 - 1);
    }

    @Test
    void recommendationPrefixPrioritizesExistingTypesAndKeepsPagesDisjoint() {
        HomeCardProperties p = new HomeCardProperties(); p.setMaxRatio(.2); p.setMinGap(3);
        List<FeedCardVO> core = new ArrayList<>();
        core.add(match(50003));
        for (long i=1;i<=20;i++) core.add(content(i));
        core.add(match(50004));
        List<FeedCardVO> auxiliary = List.of(
                discussion(100L), aux("HOT_COMMENT","HOT_COMMENT:1"),
                aux("RANKING","RANKING:STANDING:1"), aux("PLAYER_RATING","PLAYER_RATING:9"));

        var result = new HomeFeedCompositionService(p).compose(core, auxiliary, "r2");

        assertThat(result).extracting(FeedCardVO::getCardType).startsWith(
                "MATCH", "CONTENT", "RANKING", "DISCUSSION", "PLAYER_RATING",
                "CONTENT", "HOT_COMMENT", "CONTENT", "CONTENT");
        assertThat(result.subList(0, 10)).extracting(FeedCardVO::getCardType)
                .contains("MATCH", "RANKING", "PLAYER_RATING", "DISCUSSION", "HOT_COMMENT");
        assertThat(result.subList(0, 10).stream().filter(c -> "CONTENT".equals(c.getCardType())))
                .hasSizeGreaterThanOrEqualTo(3);
        assertThat(result).extracting(FeedCardVO::getPosition)
                .containsExactlyElementsOf(java.util.stream.IntStream.range(0, result.size()).boxed().toList());
        assertThat(result).extracting(FeedCardVO::getCardKey).doesNotHaveDuplicates();
        assertThat(result).allMatch(c -> c.getImpressionId().startsWith("r2:"));
        assertThat(result.subList(0, 10)).extracting(FeedCardVO::getCardKey)
                .doesNotContainAnyElementsOf(result.subList(10, result.size()).stream()
                        .map(FeedCardVO::getCardKey).toList());
    }

    @Test
    void recommendationPrefixFallsBackWhenAuxiliaryTypesAreMissing() {
        HomeCardProperties p = new HomeCardProperties();
        var result = new HomeFeedCompositionService(p).compose(
                List.of(content(1), match(50001), content(2)), List.of(), "fallback");

        assertThat(result).extracting(FeedCardVO::getCardType)
                .containsExactly("MATCH", "CONTENT", "CONTENT");
    }
    @Test void candidateShortageKeepsAllCoreCards(){HomeCardProperties p=new HomeCardProperties();var result=new HomeFeedCompositionService(p).compose(List.of(content(1),content(2)),List.of(),"r");assertThat(result).hasSize(2);}
    private FeedCardVO content(long id){FeedCardVO c=new FeedCardVO();c.setCardId("CONTENT_"+id);c.setCardKey("CONTENT:"+id);c.setCardType("CONTENT");c.setContentId(id);return c;}
    private FeedCardVO discussion(long id){FeedCardVO c=aux("DISCUSSION","DISCUSSION:"+id);c.setContentId(id);return c;}
    private FeedCardVO match(long id){FeedCardVO c=aux("MATCH","MATCH:"+id);c.setMatchId(id);return c;}
    private FeedCardVO aux(String type,String key){FeedCardVO c=new FeedCardVO();c.setCardId(key.replace(':','_'));c.setCardKey(key);c.setCardType(type);return c;}
}
