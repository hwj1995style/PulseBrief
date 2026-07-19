ALTER TABLE admin_user
    ADD COLUMN must_change_password TINYINT NOT NULL DEFAULT 0 AFTER user_status,
    ADD COLUMN password_changed_at DATETIME NULL AFTER must_change_password;

UPDATE admin_user
SET password_changed_at = updated_at
WHERE password_changed_at IS NULL;

CREATE INDEX idx_admin_session_cleanup ON admin_session (expires_at, revoked_at);
