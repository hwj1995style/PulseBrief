ALTER TABLE candidate_article
    ADD COLUMN tag_names VARCHAR(512) COMMENT '候选运营标签，逗号分隔';

ALTER TABLE news_article
    ADD COLUMN tag_names VARCHAR(512) COMMENT '文章运营标签，逗号分隔';
