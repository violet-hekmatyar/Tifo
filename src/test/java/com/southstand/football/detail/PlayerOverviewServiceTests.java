package com.southstand.football.detail;
import static org.assertj.core.api.Assertions.*;import org.junit.jupiter.api.Test;
class PlayerOverviewServiceTests{@Test void overviewUsesCurrentHistoryAndComputedAge(){var f=new FootballDetailTestFixture();var v=f.service.playerOverview(f.PLAYER,f.SEASON);assertThat(v.currentTeamId()).isEqualTo(f.TEAM);assertThat(v.age()).isPositive();assertThat(v.captain()).isTrue();}}
