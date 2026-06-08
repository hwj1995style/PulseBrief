package com.pulsebrief.category.api;

import com.pulsebrief.category.service.CategoryService;
import com.pulsebrief.common.api.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ApiResponse<List<CategoryResponse>> listCategories() {
        return ApiResponse.ok(categoryService.listEnabledCategories());
    }
}
