CREATE TABLE ai_usage_event (
    id BIGINT NOT NULL AUTO_INCREMENT,
    operation_type VARCHAR(32) NOT NULL,
    provider_type VARCHAR(32) NOT NULL,
    model_name VARCHAR(128) NOT NULL,
    request_status VARCHAR(32) NOT NULL,
    prompt_tokens INT NOT NULL DEFAULT 0,
    completion_tokens INT NOT NULL DEFAULT 0,
    estimated_cost_usd DECIMAL(14, 8) NOT NULL DEFAULT 0,
    error_code VARCHAR(128) NULL,
    created_at DATETIME NOT NULL,
    completed_at DATETIME NULL,
    PRIMARY KEY (id),
    INDEX idx_ai_usage_created_at (created_at),
    INDEX idx_ai_usage_provider_status (provider_type, request_status, created_at)
);
