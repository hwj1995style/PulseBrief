CREATE TABLE daily_digest_article (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '每日简报文章关联ID',
    digest_id BIGINT NOT NULL COMMENT '每日简报ID',
    article_id BIGINT NOT NULL COMMENT '资讯文章ID',
    sort_no INT NOT NULL DEFAULT 0 COMMENT '简报内排序',
    highlight_text VARCHAR(500) COMMENT '热点清单展示文案',
    created_at DATETIME COMMENT '创建时间',
    UNIQUE KEY uk_digest_article (digest_id, article_id),
    KEY idx_digest_sort (digest_id, sort_no, id),
    KEY idx_article_id (article_id)
) COMMENT='每日简报文章关联表';

CREATE INDEX idx_daily_digest_status_date_type
    ON daily_digest (digest_status, digest_date, digest_type);
