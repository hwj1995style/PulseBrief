package com.pulsebrief.user.service;

import com.pulsebrief.favorite.repository.UserFavoriteRepository;
import com.pulsebrief.playback.repository.UserPlayHistoryRepository;
import com.pulsebrief.readhistory.repository.UserReadHistoryRepository;
import com.pulsebrief.subscription.repository.UserSubscriptionRepository;
import com.pulsebrief.user.api.UserProfileSummaryResponse;
import org.springframework.stereotype.Service;

@Service
public class UserProfileApplicationService implements UserProfileService {
    private final UserSubscriptionRepository subscriptionRepository;
    private final UserFavoriteRepository favoriteRepository;
    private final UserPlayHistoryRepository playHistoryRepository;
    private final UserReadHistoryRepository readHistoryRepository;

    public UserProfileApplicationService(
            UserSubscriptionRepository subscriptionRepository,
            UserFavoriteRepository favoriteRepository,
            UserPlayHistoryRepository playHistoryRepository,
            UserReadHistoryRepository readHistoryRepository
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.favoriteRepository = favoriteRepository;
        this.playHistoryRepository = playHistoryRepository;
        this.readHistoryRepository = readHistoryRepository;
    }

    @Override
    public UserProfileSummaryResponse getProfile(Long userId) {
        return new UserProfileSummaryResponse(
                userId,
                nicknameFor(userId),
                "",
                bioFor(userId),
                subscriptionRepository.countByUserIdAndStatus(userId, (byte) 1),
                favoriteRepository.countByUserId(userId),
                readHistoryRepository.countByUserId(userId),
                playHistoryRepository.countByUserId(userId)
        );
    }

    private String nicknameFor(Long userId) {
        if (Long.valueOf(1L).equals(userId)) {
            return "Wenjin";
        }
        return "PulseBrief 用户";
    }

    private String bioFor(Long userId) {
        if (Long.valueOf(1L).equals(userId)) {
            return "每天几分钟，掌握全球脉搏";
        }
        return "关注全球热点与市场脉搏";
    }
}
