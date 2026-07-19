ALTER TABLE admin_session
    MODIFY COLUMN token_hash VARCHAR(64) NOT NULL;
