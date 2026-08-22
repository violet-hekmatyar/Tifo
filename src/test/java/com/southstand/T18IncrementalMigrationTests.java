package com.southstand;

import static org.assertj.core.api.Assertions.assertThat;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class T18IncrementalMigrationTests {
    @Test
    void migrationAndSeedRemainIncrementalAndIdempotent() throws Exception {
        String migration = Files.readString(Path.of("scripts/sql/migrations/V018__recommendation_behavior_log.sql")).toUpperCase();
        String seed = Files.readString(Path.of("scripts/sql/seed-t18-recommendation-incremental.sql")).toUpperCase();
        assertThat(migration).contains("CREATE TABLE IF NOT EXISTS", "UNIQUE KEY", "CLIENT_EVENT_ID")
                .doesNotContain("DROP DATABASE", "TRUNCATE", "DELETE FROM");
        assertThat(seed).contains("INSERT IGNORE INTO USER_BEHAVIOR_LOG", "T18-DEMO-")
                .doesNotContain("TRUNCATE", "DELETE FROM", "UPDATE SYS_USER", "UPDATE CONTENT");
    }
}
