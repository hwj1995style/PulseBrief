package com.pulsebrief.favorite;

import com.pulsebrief.article.api.ArticleCardResponse;
import com.pulsebrief.common.api.PageResponse;
import com.pulsebrief.favorite.api.FavoriteController;
import com.pulsebrief.favorite.api.FavoriteResponse;
import com.pulsebrief.favorite.service.FavoriteService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FavoriteController.class)
class FavoriteControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FavoriteService favoriteService;

    @Test
    void favoritesArticleForLoggedInUser() throws Exception {
        when(favoriteService.favoriteArticle(1L, 10L)).thenReturn(new FavoriteResponse(10L, true));

        mockMvc.perform(post("/api/articles/10/favorite")
                        .header("Authorization", "Bearer dev-token-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.articleId").value(10))
                .andExpect(jsonPath("$.data.favorited").value(true));
    }

    @Test
    void unfavoritesArticleForLoggedInUser() throws Exception {
        when(favoriteService.unfavoriteArticle(1L, 10L)).thenReturn(new FavoriteResponse(10L, false));

        mockMvc.perform(delete("/api/articles/10/favorite")
                        .header("Authorization", "Bearer dev-token-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.articleId").value(10))
                .andExpect(jsonPath("$.data.favorited").value(false));
    }

    @Test
    void listsFavoritesForLoggedInUser() throws Exception {
        when(favoriteService.listFavorites(1L, 1, 20)).thenReturn(PageResponse.of(List.of(new ArticleCardResponse(
                10L,
                "高盛：AI 基建投资仍将持续",
                "Goldman Sachs Research",
                "2026-06-09T09:30:00+08:00",
                "investment_view",
                "投行观点",
                "AI 基础设施投资仍处于扩张阶段。",
                "",
                "02:48",
                true,
                false,
                true
        )), 1, 20, 1L));

        mockMvc.perform(get("/api/user/favorites")
                        .header("Authorization", "Bearer dev-token-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.items[0].id").value(10))
                .andExpect(jsonPath("$.data.items[0].favorited").value(true))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.hasMore").value(false));
    }
}
