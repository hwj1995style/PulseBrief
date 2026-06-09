package com.pulsebrief.common.api;

import java.util.List;

public record PageResponse<T>(
        List<T> items,
        Integer page,
        Integer pageSize,
        Long total,
        Boolean hasMore
) {
    public static <T> PageResponse<T> of(List<T> items, Integer page, Integer pageSize, Long total) {
        int safePage = Math.max(page == null ? 1 : page, 1);
        int safePageSize = Math.min(Math.max(pageSize == null ? 20 : pageSize, 1), 50);
        long safeTotal = total == null ? items.size() : total;
        return new PageResponse<>(
                items,
                safePage,
                safePageSize,
                safeTotal,
                (long) safePage * safePageSize < safeTotal
        );
    }
}
