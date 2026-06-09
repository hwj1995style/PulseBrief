CREATE TABLE report_asset (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '报告资产ID',
    candidate_article_id BIGINT NOT NULL COMMENT '候选资讯ID',
    source_code VARCHAR(64) NOT NULL COMMENT '采集源编码',
    title VARCHAR(512) NOT NULL COMMENT 'PDF标题',
    original_url VARCHAR(1024) NOT NULL COMMENT 'PDF原始链接',
    file_name VARCHAR(255) NOT NULL COMMENT '文件名',
    file_size_bytes BIGINT COMMENT '文件大小字节数',
    file_hash VARCHAR(128) NOT NULL COMMENT '文件哈希',
    license_policy VARCHAR(32) NOT NULL COMMENT 'PDF授权策略',
    asset_status VARCHAR(32) NOT NULL COMMENT '资产状态：PENDING_REVIEW/APPROVED/REJECTED/PUBLISHED',
    downloaded_at DATETIME COMMENT '下载时间',
    created_at DATETIME COMMENT '创建时间',
    updated_at DATETIME COMMENT '更新时间',
    UNIQUE KEY uk_report_asset_file_hash (file_hash),
    KEY idx_report_asset_candidate (candidate_article_id),
    KEY idx_report_asset_status (asset_status),
    CONSTRAINT fk_report_asset_candidate_article
        FOREIGN KEY (candidate_article_id) REFERENCES candidate_article (id)
) COMMENT='授权PDF报告资产元数据表';
