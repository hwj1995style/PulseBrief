package com.pulsebrief.digest.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_digest_article")
public class DailyDigestArticle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "digest_id")
    private Long digestId;

    @Column(name = "article_id")
    private Long articleId;

    @Column(name = "sort_no")
    private Integer sortNo;

    @Column(name = "highlight_text")
    private String highlightText;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    protected DailyDigestArticle() {
    }

    public DailyDigestArticle(Long digestId, Long articleId, Integer sortNo, String highlightText) {
        this.digestId = digestId;
        this.articleId = articleId;
        this.sortNo = sortNo == null ? 0 : sortNo;
        this.highlightText = highlightText;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getDigestId() {
        return digestId;
    }

    public Long getArticleId() {
        return articleId;
    }

    public Integer getSortNo() {
        return sortNo;
    }

    public String getHighlightText() {
        return highlightText;
    }
}
