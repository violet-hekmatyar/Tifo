package com.southstand.football.detail;
import static org.assertj.core.api.Assertions.*;import org.junit.jupiter.api.Test;
class PlayerStatsServiceTests{@Test void statsReuseT15Values(){var f=new FootballDetailTestFixture();var v=f.service.playerStats(f.PLAYER,f.SEASON,null,null).get(0);assertThat(v.goals()).isEqualTo(10);assertThat(v.shotAccuracy()).isEqualByComparingTo("50.00");}}
