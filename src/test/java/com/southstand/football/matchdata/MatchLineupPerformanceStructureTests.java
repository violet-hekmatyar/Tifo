package com.southstand.football.matchdata;
import static org.assertj.core.api.Assertions.assertThat;import java.nio.file.Files;import java.nio.file.Path;import org.junit.jupiter.api.Test;
class MatchLineupPerformanceStructureTests{@Test void lineupUsesBatchLookupsAndNoMapperInsideLoop()throws Exception{String s=Files.readString(Path.of("src/main/java/com/southstand/football/matchdata/service/MatchDataService.java"));assertThat(s).contains("batchPlayers(playerIds)","batchTeams(Set.of");assertThat(s).doesNotContain("for(var ap:rows){players.selectById");}}
