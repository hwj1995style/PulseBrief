CREATE TABLE raw_news_content (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '原始资讯正文内容ID',
    raw_news_item_id BIGINT NOT NULL COMMENT '原始资讯ID',
    source_code VARCHAR(64) NOT NULL COMMENT '采集源编码',
    capture_mode VARCHAR(32) NOT NULL COMMENT '正文采集模式：SNIPPET/FULLTEXT',
    content_text MEDIUMTEXT COMMENT '授权正文片段或授权全文',
    content_text_hash VARCHAR(128) COMMENT '正文内容哈希',
    license_policy VARCHAR(32) NOT NULL COMMENT '正文授权策略快照',
    license_note VARCHAR(1000) COMMENT '正文授权说明快照',
    fetch_status VARCHAR(32) NOT NULL COMMENT '抓取状态：PENDING/SUCCESS/FAILED/SKIPPED',
    error_message VARCHAR(1024) COMMENT '错误信息',
    fetched_at DATETIME COMMENT '抓取时间',
    created_at DATETIME COMMENT '创建时间',
    updated_at DATETIME COMMENT '更新时间',
    UNIQUE KEY uk_raw_news_content_raw_item_mode (raw_news_item_id, capture_mode),
    KEY idx_raw_news_content_hash (content_text_hash),
    KEY idx_raw_news_content_status (fetch_status),
    KEY idx_raw_news_content_source (source_code),
    CONSTRAINT fk_raw_news_content_raw_item
        FOREIGN KEY (raw_news_item_id) REFERENCES raw_news_item (id)
) COMMENT='原始资讯授权正文内容表';
