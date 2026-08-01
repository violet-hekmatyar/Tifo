package com.southstand.football.detail;
import static org.assertj.core.api.Assertions.*;import org.junit.jupiter.api.Test;
class PlayerTeamHistoryServiceTests{@Test void currentTeamIsReturnedWithoutInventedTransfers(){var f=new FootballDetailTestFixture();var rows=f.service.playerTeams(f.PLAYER);assertThat(rows).hasSize(1);assertThat(rows.get(0).current()).isTrue();}}
