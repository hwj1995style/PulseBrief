package com.pulsebrief.ingestion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class IngestionSchemaTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsIngestionTables() {
        assertThat(tableExists("news_ingestion_source")).isTrue();
        assertThat(tableExists("news_ingestion_job")).isTrue();
        assertThat(tableExists("raw_news_item")).isTrue();
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_schema = database() and table_name = ?",
                Integer.class,
                tableName
        );
        return count != null && count > 0;
    }
}
