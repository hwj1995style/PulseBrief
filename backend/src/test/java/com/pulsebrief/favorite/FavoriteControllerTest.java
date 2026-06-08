package com.pulsebrief.favorite;

import com.pulsebrief.favorite.api.FavoriteController;
import com.pulsebrief.favorite.api.FavoriteResponse;
import com.pulsebrief.favorite.service.FavoriteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
}
