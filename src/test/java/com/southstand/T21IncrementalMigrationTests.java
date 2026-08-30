package com.southstand;

import static org.assertj.core.api.Assertions.assertThat;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class T21IncrementalMigrationTests {
    @Test void notificationMigrationIsIncrementalAndIdempotent() throws Exception {
        String sql=Files.readString(Path.of("scripts/sql/migrations/V019__notification_center.sql")).toUpperCase();
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS NOTIFICATION","UNIQUE KEY UK_NOTIFICATION_DEDUP","IDX_NOTIFICATION_RECIPIENT_READ_TIME")
                .doesNotContain("DROP DATABASE","DROP TABLE","TRUNCATE","DELETE FROM","UPDATE SYS_USER");
    }
}
