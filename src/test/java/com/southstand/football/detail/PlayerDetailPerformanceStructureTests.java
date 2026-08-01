package com.southstand.football.detail;
import static org.assertj.core.api.Assertions.*;import java.nio.file.*;import org.junit.jupiter.api.Test;
class PlayerDetailPerformanceStructureTests{@Test void careerUsesOneStatQueryAndBatchNames()throws Exception{String s=Files.readString(Path.of("src/main/java/com/southstand/football/detail/service/FootballDetailService.java"));assertThat(s).contains("List<FootballPlayerCompetitionStat> rows=playerStats.selectList","selectBatchIds(seasonIds)","selectBatchIds(teamIds)");}}
