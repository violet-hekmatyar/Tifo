package com.southstand.football.detail;
import static org.assertj.core.api.Assertions.*;import org.junit.jupiter.api.Test;
class TeamStatsServiceTests{@Test void statsDeriveDifferenceAndAccuracy(){var f=new FootballDetailTestFixture();var v=f.service.teamStats(f.TEAM,f.SEASON,null);assertThat(v.goalDifference()).isEqualTo(20);assertThat(v.shotAccuracy()).isEqualByComparingTo("50.00");assertThat(v.standingRank()).isEqualTo(1);}}
