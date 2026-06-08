package com.pulsebrief.category.service;

import com.pulsebrief.category.api.CategoryResponse;
import com.pulsebrief.category.domain.NewsCategory;
import com.pulsebrief.category.repository.CategoryRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CategoryQueryService implements CategoryService {
    private final CategoryRepository categoryRepository;

    public CategoryQueryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<CategoryResponse> listEnabledCategories() {
        return categoryRepository.findByStatusOrderBySortNoAscIdAsc((byte) 1)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private CategoryResponse toResponse(NewsCategory category) {
        return new CategoryResponse(
                category.getId(),
                category.getCategoryCode(),
                category.getCategoryName(),
                descriptionFor(category.getCategoryCode()),
                category.getSortNo(),
                category.getStatus() != null && category.getStatus() == 1
        );
    }

    private String descriptionFor(String code) {
        return switch (code) {
            case "global" -> "追踪全球热点与重大事件";
            case "finance" -> "关注全球市场、利率、汇率、商品与资本动态";
            case "tech" -> "观察科技公司、产品与产业趋势";
            case "ai" -> "跟踪大模型、算力、芯片和应用动态";
            case "macro" -> "聚焦央行、通胀、财政和监管动态";
            case "investment_view" -> "精选投行公开观点摘要";
            default -> "全球资讯主题";
        };
    }
}
