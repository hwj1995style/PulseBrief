package com.pulsebrief.category;

import com.pulsebrief.category.api.CategoryController;
import com.pulsebrief.category.api.CategoryResponse;
import com.pulsebrief.category.service.CategoryService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    void returnsEnabledCategoriesOrderedForAppNavigation() throws Exception {
        when(categoryService.listEnabledCategories()).thenReturn(List.of(
                new CategoryResponse(1L, "finance", "财经市场", "关注全球市场、利率、汇率、商品与资本动态", 20, true),
                new CategoryResponse(2L, "ai", "AI 前沿", "跟踪大模型、算力、芯片和应用动态", 40, true)
        ));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data[0].code").value("finance"))
                .andExpect(jsonPath("$.data[0].name").value("财经市场"))
                .andExpect(jsonPath("$.data[1].code").value("ai"));
    }
}
