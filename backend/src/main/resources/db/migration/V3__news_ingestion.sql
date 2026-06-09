CREATE TABLE news_ingestion_source (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '采集源ID',
    code VARCHAR(64) NOT NULL COMMENT '采集源编码',
    name VARCHAR(128) NOT NULL COMMENT '采集源名称',
    provider_type VARCHAR(32) NOT NULL COMMENT 'Provider类型：FIXTURE/RSS/API',
    base_url VARCHAR(512) NOT NULL COMMENT '采集源基础地址或资源位置',
    default_category_code VARCHAR(64) COMMENT '默认分类编码',
    enabled TINYINT DEFAULT 1 COMMENT '是否启用：1启用 0停用',
    rate_limit_per_hour INT DEFAULT 60 COMMENT '每小时请求上限',
    created_at DATETIME COMMENT '创建时间',
    updated_at DATETIME COMMENT '更新时间',
    UNIQUE KEY uk_ingestion_source_code (code)
) COMMENT='资讯采集源配置表';

CREATE TABLE news_ingestion_job (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '采集任务ID',
    source_code VARCHAR(64) NOT NULL COMMENT '采集源编码',
    trigger_type VARCHAR(32) NOT NULL COMMENT '触发类型：MANUAL/SCHEDULED',
    job_status VARCHAR(32) NOT NULL COMMENT '任务状态：RUNNING/SUCCESS/PARTIAL_SUCCESS/FAILED',
    started_at DATETIME COMMENT '开始时间',
    finished_at DATETIME COMMENT '结束时间',
    fetched_count INT DEFAULT 0 COMMENT '拉取条目数量',
    new_count INT DEFAULT 0 COMMENT '新增条目数量',
    duplicate_count INT DEFAULT 0 COMMENT '重复条目数量',
    candidate_count INT DEFAULT 0 COMMENT '生成候选数量',
    error_message VARCHAR(1024) COMMENT '错误信息',
    created_at DATETIME COMMENT '创建时间',
    updated_at DATETIME COMMENT '更新时间',
    KEY idx_ingestion_job_source_time (source_code, started_at),
    KEY idx_ingestion_job_status (job_status)
) COMMENT='资讯采集任务日志表';

CREATE TABLE raw_news_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '原始资讯ID',
    source_code VARCHAR(64) NOT NULL COMMENT '采集源编码',
    provider_item_id VARCHAR(128) COMMENT '外部数据源条目ID',
    title VARCHAR(512) NOT NULL COMMENT '原始标题',
    summary VARCHAR(2000) COMMENT '原始摘要或描述',
    source_name VARCHAR(128) NOT NULL COMMENT '原始来源名称',
    original_url VARCHAR(1024) NOT NULL COMMENT '原文链接',
    original_url_hash VARCHAR(128) NOT NULL COMMENT '原文链接去重哈希',
    image_url VARCHAR(1024) COMMENT '原始图片链接',
    published_at DATETIME COMMENT '外部发布时间',
    fetched_at DATETIME COMMENT '采集时间',
    language VARCHAR(16) COMMENT '语言',
    country VARCHAR(32) COMMENT '国家或地区',
    raw_payload JSON COMMENT '外部返回原始元数据JSON',
    content_hash VARCHAR(128) NOT NULL COMMENT '内容去重指纹',
    item_status VARCHAR(32) NOT NULL COMMENT '原始条目状态：NEW/DUPLICATE/CANDIDATE/REJECTED/PUBLISHED/ERROR',
    duplicate_of_id BIGINT COMMENT '重复目标原始资讯ID',
    created_at DATETIME COMMENT '创建时间',
    updated_at DATETIME COMMENT '更新时间',
    UNIQUE KEY uk_raw_news_original_url_hash (original_url_hash),
    KEY idx_raw_news_source_hash (source_code, content_hash),
    KEY idx_raw_news_status (item_status),
    KEY idx_raw_news_fetched_at (fetched_at)
) COMMENT='原始资讯采集池';
