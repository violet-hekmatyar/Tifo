package com.southstand.football.detail;
import static org.assertj.core.api.Assertions.*;import org.junit.jupiter.api.Test;
class TeamHonorServiceTests{@Test void yearsAreReturnedAsArray(){var f=new FootballDetailTestFixture();assertThat(f.service.teamHonors(f.TEAM,null).get(0).winningYears()).containsExactly(2025);}}
