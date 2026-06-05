CREATE TABLE app_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    mobile VARCHAR(30) COMMENT '手机号',
    email VARCHAR(100) COMMENT '邮箱',
    nickname VARCHAR(100) COMMENT '用户昵称',
    avatar_url VARCHAR(500) COMMENT '头像URL',
    user_status TINYINT DEFAULT 1 COMMENT '用户状态：1正常 0禁用',
    last_login_time DATETIME COMMENT '最近登录时间',
    created_at DATETIME COMMENT '创建时间',
    updated_at DATETIME COMMENT '更新时间',
    UNIQUE KEY uk_mobile (mobile),
    UNIQUE KEY uk_email (email)
) COMMENT='APP用户表';

CREATE TABLE news_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分类ID',
    category_name VARCHAR(100) NOT NULL COMMENT '分类名称',
    category_code VARCHAR(50) NOT NULL COMMENT '分类编码',
    parent_id BIGINT DEFAULT 0 COMMENT '父分类ID',
    sort_no INT DEFAULT 0 COMMENT '排序号',
    status TINYINT DEFAULT 1 COMMENT '状态：1启用 0停用',
    created_at DATETIME COMMENT '创建时间',
    updated_at DATETIME COMMENT '更新时间',
    UNIQUE KEY uk_category_code (category_code)
) COMMENT='资讯分类表';

CREATE TABLE news_source (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '资讯源ID',
    source_name VARCHAR(100) NOT NULL COMMENT '来源名称',
    source_type VARCHAR(30) NOT NULL COMMENT '来源类型：GDELT/RSS/OFFICIAL/IB_PUBLIC/API',
    source_url VARCHAR(500) COMMENT '来源地址',
    country VARCHAR(50) COMMENT '国家或地区',
    language VARCHAR(20) COMMENT '语言',
    default_category_code VARCHAR(50) COMMENT '默认分类编码',
    usage_policy VARCHAR(50) COMMENT '使用策略：FREE/PERSONAL_ONLY/DEV_ONLY/COMMERCIAL_ALLOWED/UNKNOWN',
    allow_title TINYINT DEFAULT 1 COMMENT '是否允许展示标题',
    allow_summary TINYINT DEFAULT 1 COMMENT '是否允许展示摘要',
    allow_fulltext TINYINT DEFAULT 0 COMMENT '是否允许展示全文',
    allow_commercial TINYINT DEFAULT 0 COMMENT '是否允许商业展示',
    risk_level VARCHAR(20) DEFAULT 'MEDIUM' COMMENT '合规风险等级',
    fetch_interval_minutes INT DEFAULT 60 COMMENT '采集间隔分钟数',
    last_fetch_time DATETIME COMMENT '最近采集时间',
    last_fetch_status VARCHAR(30) COMMENT '最近采集状态',
    status TINYINT DEFAULT 1 COMMENT '状态：1启用 0停用',
    created_at DATETIME COMMENT '创建时间',
    updated_at DATETIME COMMENT '更新时间'
) COMMENT='资讯源表';

CREATE TABLE news_article (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '资讯文章ID',
    title VARCHAR(500) NOT NULL COMMENT '标题',
    normalized_title VARCHAR(500) COMMENT '规范化标题',
    summary TEXT COMMENT '来源摘要或短摘要',
    ai_summary TEXT COMMENT 'AI生成摘要',
    key_points TEXT COMMENT '核心要点',
    impact_analysis TEXT COMMENT '可能影响分析',
    source_id BIGINT COMMENT '来源ID',
    source_name VARCHAR(100) COMMENT '来源名称',
    original_url VARCHAR(1000) COMMENT '原文链接',
    category_id BIGINT COMMENT '分类ID',
    category_code VARCHAR(50) COMMENT '分类编码',
    language VARCHAR(20) COMMENT '语言',
    country VARCHAR(50) COMMENT '国家或地区',
    publish_time DATETIME COMMENT '发布时间',
    hot_score DECIMAL(10,2) DEFAULT 0 COMMENT '热度分',
    article_hash VARCHAR(64) NOT NULL COMMENT '文章去重哈希',
    article_status VARCHAR(30) DEFAULT 'PENDING' COMMENT '文章状态',
    is_top TINYINT DEFAULT 0 COMMENT '是否置顶',
    view_count INT DEFAULT 0 COMMENT '阅读次数',
    favorite_count INT DEFAULT 0 COMMENT '收藏次数',
    play_count INT DEFAULT 0 COMMENT '播放次数',
    created_at DATETIME COMMENT '创建时间',
    updated_at DATETIME COMMENT '更新时间',
    UNIQUE KEY uk_article_hash (article_hash),
    KEY idx_category_publish_time (category_code, publish_time),
    KEY idx_hot_score (hot_score),
    KEY idx_publish_time (publish_time),
    KEY idx_source_id (source_id)
) COMMENT='资讯文章表';

CREATE TABLE user_subscription (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '订阅ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    category_id BIGINT NOT NULL COMMENT '分类ID',
    category_code VARCHAR(50) COMMENT '分类编码',
    sort_no INT DEFAULT 0 COMMENT '订阅排序号',
    status TINYINT DEFAULT 1 COMMENT '状态：1启用 0停用',
    created_at DATETIME COMMENT '创建时间',
    updated_at DATETIME COMMENT '更新时间',
    UNIQUE KEY uk_user_category (user_id, category_id)
) COMMENT='用户订阅表';

CREATE TABLE user_favorite (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '收藏ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    article_id BIGINT NOT NULL COMMENT '文章ID',
    created_at DATETIME COMMENT '创建时间',
    UNIQUE KEY uk_user_article (user_id, article_id),
    KEY idx_user_id (user_id)
) COMMENT='用户收藏表';

CREATE TABLE user_read_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '阅读历史ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    article_id BIGINT NOT NULL COMMENT '文章ID',
    read_time DATETIME COMMENT '阅读时间',
    KEY idx_user_time (user_id, read_time)
) COMMENT='用户阅读历史表';

CREATE TABLE user_play_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '播放历史ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    article_id BIGINT COMMENT '文章ID',
    digest_id BIGINT COMMENT '简报ID',
    play_type VARCHAR(30) COMMENT '播放类型：ARTICLE/DIGEST',
    play_title VARCHAR(500) COMMENT '播放标题',
    play_time DATETIME COMMENT '播放时间',
    duration_seconds INT DEFAULT 0 COMMENT '播放时长秒数',
    KEY idx_user_time (user_id, play_time)
) COMMENT='用户播放历史表';

CREATE TABLE daily_digest (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '每日简报ID',
    digest_date DATE NOT NULL COMMENT '简报日期',
    digest_type VARCHAR(30) COMMENT '简报类型：MORNING/NOON/EVENING/TOPIC',
    category_code VARCHAR(50) COMMENT '分类编码',
    title VARCHAR(300) COMMENT '简报标题',
    summary TEXT COMMENT '简报摘要',
    content TEXT COMMENT '简报内容',
    audio_text TEXT COMMENT '播报文案',
    digest_status VARCHAR(30) DEFAULT 'DRAFT' COMMENT '简报状态',
    publish_time DATETIME COMMENT '发布时间',
    created_at DATETIME COMMENT '创建时间',
    updated_at DATETIME COMMENT '更新时间',
    KEY idx_digest_date_type (digest_date, digest_type)
) COMMENT='每日简报表';

CREATE TABLE push_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '推送任务ID',
    task_type VARCHAR(30) COMMENT '任务类型',
    title VARCHAR(200) COMMENT '推送标题',
    content VARCHAR(500) COMMENT '推送内容',
    target_type VARCHAR(30) COMMENT '推送对象类型',
    target_category_code VARCHAR(50) COMMENT '目标分类编码',
    related_digest_id BIGINT COMMENT '关联简报ID',
    related_article_id BIGINT COMMENT '关联文章ID',
    scheduled_time DATETIME COMMENT '计划发送时间',
    send_status VARCHAR(30) DEFAULT 'PENDING' COMMENT '发送状态',
    sent_count INT DEFAULT 0 COMMENT '发送数量',
    click_count INT DEFAULT 0 COMMENT '点击数量',
    created_at DATETIME COMMENT '创建时间',
    updated_at DATETIME COMMENT '更新时间',
    KEY idx_scheduled_time (scheduled_time)
) COMMENT='推送任务表';
