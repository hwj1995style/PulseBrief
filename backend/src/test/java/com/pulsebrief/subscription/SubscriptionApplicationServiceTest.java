package com.pulsebrief.subscription;

import com.pulsebrief.category.domain.NewsCategory;
import com.pulsebrief.category.repository.CategoryRepository;
import com.pulsebrief.subscription.api.PushPreferencesResponse;
import com.pulsebrief.subscription.api.SubscriptionSaveRequest;
import com.pulsebrief.subscription.domain.UserSubscription;
import com.pulsebrief.subscription.repository.UserSubscriptionRepository;
import com.pulsebrief.subscription.service.SubscriptionApplicationService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionApplicationServiceTest {
    @Mock
    private UserSubscriptionRepository subscriptionRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Test
    void flushesDeletedSubscriptionsBeforeSavingReplacementRows() {
        NewsCategory global = category(1L, "global");
        NewsCategory finance = category(2L, "finance");
        when(categoryRepository.findByCategoryCodeIn(List.of("global", "finance")))
                .thenReturn(List.of(global, finance));
        SubscriptionApplicationService service =
                new SubscriptionApplicationService(subscriptionRepository, categoryRepository);

        service.saveSubscriptions(1L, new SubscriptionSaveRequest(
                List.of("global", "finance"),
                new PushPreferencesResponse(true, true, false, false)
        ));

        InOrder inOrder = inOrder(subscriptionRepository);
        inOrder.verify(subscriptionRepository).deleteByUserId(1L);
        inOrder.verify(subscriptionRepository).flush();
        inOrder.verify(subscriptionRepository, times(2)).save(any(UserSubscription.class));
    }

    private NewsCategory category(Long id, String code) {
        NewsCategory category = mock(NewsCategory.class);
        when(category.getId()).thenReturn(id);
        when(category.getCategoryCode()).thenReturn(code);
        return category;
    }
}
