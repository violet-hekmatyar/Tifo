package com.southstand.football.matchdata;
import static org.assertj.core.api.Assertions.assertThat;import static org.assertj.core.api.Assertions.assertThatThrownBy;import static org.mockito.ArgumentMatchers.any;import static org.mockito.Mockito.verify;import static org.mockito.Mockito.when;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;import com.southstand.auth.security.CurrentUserHolder;import com.southstand.auth.security.LoginUserContext;import com.southstand.common.exception.BusinessException;import java.math.BigDecimal;import java.util.List;import org.junit.jupiter.api.AfterEach;import org.junit.jupiter.api.Test;
class UserPlayerRatingServiceTests{
 @AfterEach void clear(){CurrentUserHolder.clear();}
 @Test void rejectsNonHalfStep(){var f=new MatchDataTestFixture();CurrentUserHolder.set(new LoginUserContext(f.USER,"u","USER"));assertThatThrownBy(()->f.service.submitRating(f.MATCH,f.PLAYER,new BigDecimal("8.3"))).isInstanceOf(BusinessException.class);}
 @Test void submitUsesAtomicUpsertAndReturnsAggregate(){var f=new MatchDataTestFixture();CurrentUserHolder.set(new LoginUserContext(f.USER,"u","USER"));when(f.ratings.selectList(any(QueryWrapper.class))).thenReturn(List.of(f.rating(f.USER,"8.5")));var v=f.service.submitRating(f.MATCH,f.PLAYER,new BigDecimal("8.5"));verify(f.ratings).upsertActive(any());assertThat(v.ratingCount()).isEqualTo(1);assertThat(v.myRating()).isEqualByComparingTo("8.5");}
 @Test void cancelIsIdempotent(){var f=new MatchDataTestFixture();CurrentUserHolder.set(new LoginUserContext(f.USER,"u","USER"));var v=f.service.cancelRating(f.MATCH,f.PLAYER);verify(f.ratings).cancelOwn(f.USER,f.MATCH,f.PLAYER);assertThat(v.ratingCount()).isZero();}
}
