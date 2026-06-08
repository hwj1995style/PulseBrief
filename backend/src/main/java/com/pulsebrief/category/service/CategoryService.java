package com.pulsebrief.category.service;

import com.pulsebrief.category.api.CategoryResponse;
import java.util.List;

public interface CategoryService {
    List<CategoryResponse> listEnabledCategories();
}
