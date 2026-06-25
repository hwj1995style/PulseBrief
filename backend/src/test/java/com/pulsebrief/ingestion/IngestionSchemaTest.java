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
        assertThat(tableExists("candidate_article")).isTrue();
        assertThat(tableExists("report_asset")).isTrue();
        assertThat(tableExists("report_asset_file")).isTrue();
        assertThat(tableExists("raw_news_content")).isTrue();
        assertThat(columnExists("news_ingestion_source", "content_access_policy")).isTrue();
        assertThat(columnExists("news_ingestion_source", "max_age_hours")).isTrue();
        assertThat(columnExists("news_ingestion_source", "allow_pdf_download")).isTrue();
        assertThat(columnExists("news_ingestion_source", "allow_full_text")).isTrue();
        assertThat(columnExists("news_ingestion_source", "license_note")).isTrue();
        assertThat(columnExists("raw_news_content", "raw_news_item_id")).isTrue();
        assertThat(columnExists("raw_news_content", "content_text_hash")).isTrue();
        assertThat(columnExists("raw_news_content", "fetch_status")).isTrue();
        assertThat(columnExists("report_asset", "asset_file_id")).isTrue();
        assertThat(columnExists("report_asset", "license_note")).isTrue();
        assertThat(columnExists("report_asset", "cache_status")).isTrue();
        assertThat(columnExists("report_asset", "cache_error_message")).isTrue();
        assertThat(columnExists("report_asset", "cache_completed_at")).isTrue();
        assertThat(columnExists("report_asset", "review_note")).isTrue();
        assertThat(columnExists("report_asset_file", "storage_path")).isTrue();
        assertThat(columnExists("report_asset_file", "mime_type")).isTrue();
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_schema = database() and table_name = ?",
                Integer.class,
                tableName
        );
        return count != null && count > 0;
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from information_schema.columns
                        where table_schema = database() and table_name = ? and column_name = ?
                        """,
                Integer.class,
                tableName,
                columnName
        );
        return count != null && count > 0;
    }
}
