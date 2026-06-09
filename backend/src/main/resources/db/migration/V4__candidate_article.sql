CREATE TABLE candidate_article (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '候选资讯ID',
    raw_news_item_id BIGINT NOT NULL COMMENT '原始资讯ID',
    title VARCHAR(512) NOT NULL COMMENT '候选标题，可被Admin编辑',
    summary VARCHAR(2000) COMMENT '候选摘要，可被Admin编辑',
    category_code VARCHAR(64) COMMENT '分类编码，可被Admin调整',
    source_name VARCHAR(128) NOT NULL COMMENT '来源名称',
    original_url VARCHAR(1024) NOT NULL COMMENT '原文链接',
    published_at DATETIME COMMENT '原文发布时间',
    candidate_status VARCHAR(32) NOT NULL COMMENT '候选状态：PENDING_REVIEW/REJECTED/PUBLISHED',
    review_note VARCHAR(1000) COMMENT '审核备注',
    published_article_id BIGINT COMMENT '发布后的文章ID',
    created_at DATETIME COMMENT '创建时间',
    updated_at DATETIME COMMENT '更新时间',
    UNIQUE KEY uk_candidate_article_raw_news_item_id (raw_news_item_id),
    KEY idx_candidate_article_status (candidate_status),
    KEY idx_candidate_article_category_status (category_code, candidate_status),
    CONSTRAINT fk_candidate_article_raw_news_item
        FOREIGN KEY (raw_news_item_id) REFERENCES raw_news_item (id)
) COMMENT='候选资讯审核池';
