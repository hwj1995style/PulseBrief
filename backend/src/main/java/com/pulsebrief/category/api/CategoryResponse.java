package com.pulsebrief.category.api;

public record CategoryResponse(
        Long id,
        String code,
        String name,
        String description,
        Integer sortNo,
        Boolean enabled
) {
}
