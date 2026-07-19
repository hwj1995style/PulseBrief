CREATE TABLE admin_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(128) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    role_code VARCHAR(32) NOT NULL,
    user_status VARCHAR(32) NOT NULL,
    failed_login_count INT NOT NULL DEFAULT 0,
    locked_until DATETIME NULL,
    last_login_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_admin_user_username (username),
    KEY idx_admin_user_status_role (user_status, role_code)
) COMMENT='Admin账号与角色';

CREATE TABLE admin_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    admin_user_id BIGINT NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expires_at DATETIME NOT NULL,
    revoked_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    last_seen_at DATETIME NOT NULL,
    UNIQUE KEY uk_admin_session_token_hash (token_hash),
    KEY idx_admin_session_user_expiry (admin_user_id, expires_at),
    CONSTRAINT fk_admin_session_user FOREIGN KEY (admin_user_id) REFERENCES admin_user(id)
) COMMENT='Admin不透明登录会话';

ALTER TABLE admin_operation_log
    ADD COLUMN operator_user_id BIGINT NULL AFTER operation_status,
    ADD COLUMN operator_role VARCHAR(32) NULL AFTER operator_name;
