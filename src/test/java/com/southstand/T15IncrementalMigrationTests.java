package com.southstand;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class T15IncrementalMigrationTests {

    private static String read(String relativePath) throws Exception {
        return Files.readString(Path.of(relativePath));
    }

    @Test
    void migrationOnlyCreatesMissingTablesAndIndexes() throws Exception {
        String sql = read("scripts/sql/migrations/V015__season_standings_ranks.sql").toUpperCase();
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS", "INFORMATION_SCHEMA.STATISTICS");
        assertThat(sql).doesNotContain("DROP TABLE", "TRUNCATE TABLE", "DROP DATABASE");
    }

    @Test
    void incrementalSeedUsesInsertIfMissingAndNoBusinessDeletes() throws Exception {
        String sql = read("scripts/sql/seed-t15-incremental.sql").toUpperCase();
        assertThat(sql).contains("WHERE NOT EXISTS", "START TRANSACTION", "COMMIT");
        assertThat(sql).doesNotContain("DELETE FROM SYS_USER", "DELETE FROM CONTENT", "DELETE FROM FOOTBALL_TEAM",
                "DELETE FROM FOOTBALL_PLAYER", "DELETE FROM MATCH_INFO", "TRUNCATE TABLE", "DROP DATABASE");
    }

    @Test
    void defaultInitializerIsIncrementalAndResetNeedsConfirmation() throws Exception {
        String init = read("scripts/windows/init-demo-data.ps1");
        String reset = read("scripts/windows/reset-dev-db.ps1");
        assertThat(init).contains("[string]$Mode = \"Incremental\"");
        assertThat(reset).contains("-ConfirmReset", "RESET south_stand");
    }
}
