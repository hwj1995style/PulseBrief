package com.pulsebrief.user;

import com.pulsebrief.favorite.repository.UserFavoriteRepository;
import com.pulsebrief.playback.repository.UserPlayHistoryRepository;
import com.pulsebrief.subscription.repository.UserSubscriptionRepository;
import com.pulsebrief.user.api.UserProfileSummaryResponse;
import com.pulsebrief.user.service.UserProfileApplicationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileApplicationServiceTest {
    @Mock
    private UserSubscriptionRepository subscriptionRepository;

    @Mock
    private UserFavoriteRepository favoriteRepository;

    @Mock
    private UserPlayHistoryRepository playHistoryRepository;

    @Test
    void aggregatesProfileStatsFromUserTables() {
        when(subscriptionRepository.countByUserIdAndStatus(1L, (byte) 1)).thenReturn(4);
        when(favoriteRepository.countByUserId(1L)).thenReturn(2);
        when(playHistoryRepository.countByUserId(1L)).thenReturn(3);
        UserProfileApplicationService service = new UserProfileApplicationService(
                subscriptionRepository,
                favoriteRepository,
                playHistoryRepository
        );

        UserProfileSummaryResponse profile = service.getProfile(1L);

        assertThat(profile.id()).isEqualTo(1L);
        assertThat(profile.nickname()).isEqualTo("Wenjin");
        assertThat(profile.subscriptionCount()).isEqualTo(4);
        assertThat(profile.favoriteCount()).isEqualTo(2);
        assertThat(profile.readCount()).isZero();
        assertThat(profile.playCount()).isEqualTo(3);
    }
}
