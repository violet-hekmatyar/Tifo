package com.southstand;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ExistingDataPreservationTests {

    @Test
    void preservationQueryHashesAllCriticalExistingDomains() throws Exception {
        String sql = Files.readString(Path.of("scripts/sql/check-existing-data-preserved.sql")).toLowerCase();
        assertThat(sql).contains("sha2(", "password_hash", "from sys_user", "from content", "from comment",
                "from like_record", "from favorite_record", "from follow_record", "from football_league",
                "from football_team", "from football_player", "from match_info");
    }

    @Test
    void afterCheckRequiresEveryOldIdAndFingerprint() throws Exception {
        String script = Files.readString(Path.of("scripts/windows/check-existing-data-preserved.ps1"));
        assertThat(script).contains("row missing:", "row changed:", "row count decreased:");
    }
}
