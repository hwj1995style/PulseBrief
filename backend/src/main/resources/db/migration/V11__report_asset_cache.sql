CREATE TABLE report_asset_file (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'PDF物理缓存文件ID',
    file_hash VARCHAR(128) NOT NULL COMMENT 'PDF文件SHA-256哈希',
    storage_provider VARCHAR(32) NOT NULL COMMENT '存储提供方：LOCAL/S3/OSS/MINIO',
    storage_path VARCHAR(1024) NOT NULL COMMENT '缓存文件相对存储路径',
    file_name VARCHAR(255) NOT NULL COMMENT '规范化文件名',
    file_size_bytes BIGINT NOT NULL COMMENT '文件大小字节数',
    mime_type VARCHAR(128) NOT NULL COMMENT '文件MIME类型',
    downloaded_at DATETIME NOT NULL COMMENT '下载完成时间',
    created_at DATETIME COMMENT '创建时间',
    updated_at DATETIME COMMENT '更新时间',
    UNIQUE KEY uk_report_asset_file_hash (file_hash),
    KEY idx_report_asset_file_created_at (created_at)
) COMMENT='授权PDF物理缓存文件表';

ALTER TABLE report_asset
    ADD COLUMN asset_file_id BIGINT COMMENT '关联PDF物理缓存文件ID' AFTER candidate_article_id,
    ADD COLUMN license_note VARCHAR(512) COMMENT 'PDF授权说明快照' AFTER license_policy,
    ADD COLUMN cache_status VARCHAR(32) NOT NULL DEFAULT 'NOT_CACHED' COMMENT 'PDF缓存状态：NOT_CACHED/PENDING/SUCCESS/FAILED/SKIPPED' AFTER asset_status,
    ADD COLUMN cache_error_message VARCHAR(1000) COMMENT 'PDF缓存失败或跳过原因' AFTER cache_status,
    ADD COLUMN cache_requested_at DATETIME COMMENT 'PDF缓存请求时间' AFTER cache_error_message,
    ADD COLUMN cache_completed_at DATETIME COMMENT 'PDF缓存完成时间' AFTER cache_requested_at,
    ADD COLUMN review_note VARCHAR(1000) COMMENT 'Admin PDF合规审核备注' AFTER cache_completed_at,
    ADD COLUMN reviewed_at DATETIME COMMENT 'Admin PDF合规审核时间' AFTER review_note,
    ADD COLUMN reviewed_by VARCHAR(128) COMMENT 'Admin PDF合规审核人' AFTER reviewed_at,
    ADD KEY idx_report_asset_asset_file (asset_file_id),
    ADD KEY idx_report_asset_cache_status (cache_status),
    ADD CONSTRAINT fk_report_asset_file
        FOREIGN KEY (asset_file_id) REFERENCES report_asset_file(id);
