package com.pulsebrief.category.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "news_category")
public class NewsCategory {
    @Id
    private Long id;

    @Column(name = "category_name")
    private String categoryName;

    @Column(name = "category_code")
    private String categoryCode;

    @Column(name = "sort_no")
    private Integer sortNo;

    private Byte status;

    protected NewsCategory() {
    }

    public Long getId() {
        return id;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public Integer getSortNo() {
        return sortNo;
    }

    public Byte getStatus() {
        return status;
    }
}
