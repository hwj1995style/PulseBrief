package com.pulsebrief.favorite.api;

import com.pulsebrief.common.api.ApiResponse;
import com.pulsebrief.common.security.DevTokenSupport;
import com.pulsebrief.favorite.service.FavoriteService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/articles/{articleId}/favorite")
public class FavoriteController {
    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @PostMapping
    public ApiResponse<FavoriteResponse> favorite(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long articleId
    ) {
        Long userId = DevTokenSupport.requireUserId(authorization);
        return ApiResponse.ok(favoriteService.favoriteArticle(userId, articleId));
    }

    @DeleteMapping
    public ApiResponse<FavoriteResponse> unfavorite(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long articleId
    ) {
        Long userId = DevTokenSupport.requireUserId(authorization);
        return ApiResponse.ok(favoriteService.unfavoriteArticle(userId, articleId));
    }
}
