ALTER TABLE candidate_article
    ADD COLUMN suggested_category_code VARCHAR(64) NULL AFTER category_code,
    ADD COLUMN classification_confidence DECIMAL(5,4) NULL AFTER suggested_category_code,
    ADD COLUMN classification_rule VARCHAR(128) NULL AFTER classification_confidence,
    ADD COLUMN category_override_reason VARCHAR(500) NULL AFTER classification_rule;

UPDATE candidate_article
SET suggested_category_code = category_code,
    classification_confidence = 0.5000,
    classification_rule = 'LEGACY_BACKFILL'
WHERE suggested_category_code IS NULL;
