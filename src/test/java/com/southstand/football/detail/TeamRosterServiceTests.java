package com.southstand.football.detail;
import static org.assertj.core.api.Assertions.*;import org.junit.jupiter.api.Test;
class TeamRosterServiceTests{@Test void rosterIncludesT15StatsAndCaptain(){var f=new FootballDetailTestFixture();var r=f.service.teamPlayers(f.TEAM,f.SEASON,null,null,1,50).getRecords().get(0);assertThat(r.captain()).isTrue();assertThat(r.goals()).isEqualTo(10);assertThat(r.position()).isEqualTo("FORWARD");}}
