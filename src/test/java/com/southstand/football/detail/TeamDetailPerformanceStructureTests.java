package com.southstand.football.detail;
import static org.assertj.core.api.Assertions.*;import java.nio.file.*;import org.junit.jupiter.api.Test;
class TeamDetailPerformanceStructureTests{@Test void rosterLoopContainsNoMapperCalls()throws Exception{String s=Files.readString(Path.of("src/main/java/com/southstand/football/detail/service/FootballDetailService.java"));assertThat(s).contains("selectBatchIds","statMap");assertThat(s).doesNotContain("rows.forEach(r->players.selectById");}}
