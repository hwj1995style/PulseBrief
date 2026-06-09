package com.pulsebrief.subscription.service;

import com.pulsebrief.category.domain.NewsCategory;
import com.pulsebrief.category.repository.CategoryRepository;
import com.pulsebrief.subscription.api.PushPreferencesResponse;
import com.pulsebrief.subscription.api.SubscriptionResponse;
import com.pulsebrief.subscription.api.SubscriptionSaveRequest;
import com.pulsebrief.subscription.domain.UserSubscription;
import com.pulsebrief.subscription.repository.UserSubscriptionRepository;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionApplicationService implements SubscriptionService {
    private static final PushPreferencesResponse DEFAULT_PUSH =
            new PushPreferencesResponse(true, true, false, false);

    private final UserSubscriptionRepository subscriptionRepository;
    private final CategoryRepository categoryRepository;

    public SubscriptionApplicationService(
            UserSubscriptionRepository subscriptionRepository,
            CategoryRepository categoryRepository
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public SubscriptionResponse getSubscriptions(Long userId) {
        List<String> codes = subscriptionRepository.findByUserIdAndStatusOrderBySortNoAscIdAsc(userId, (byte) 1)
                .stream()
                .map(UserSubscription::getCategoryCode)
                .toList();
        if (!codes.isEmpty()) {
            return new SubscriptionResponse(codes, 42, DEFAULT_PUSH);
        }
        return new SubscriptionResponse(List.of("global", "finance", "ai", "investment_view", "tech"), 42, DEFAULT_PUSH);
    }

    @Override
    @Transactional
    public SubscriptionResponse saveSubscriptions(Long userId, SubscriptionSaveRequest request) {
        List<String> requestedCodes = request.categoryCodes() == null ? List.of() : request.categoryCodes();
        subscriptionRepository.deleteByUserId(userId);
        subscriptionRepository.flush();
        List<NewsCategory> categories = categoryRepository.findByCategoryCodeIn(requestedCodes)
                .stream()
                .sorted(Comparator.comparing(category -> requestedCodes.indexOf(category.getCategoryCode())))
                .toList();
        for (int index = 0; index < categories.size(); index++) {
            NewsCategory category = categories.get(index);
            subscriptionRepository.save(new UserSubscription(
                    userId,
                    category.getId(),
                    category.getCategoryCode(),
                    index + 1
            ));
        }
        PushPreferencesResponse push = request.pushPreferences() == null ? DEFAULT_PUSH : request.pushPreferences();
        return new SubscriptionResponse(
                categories.stream().map(NewsCategory::getCategoryCode).toList(),
                42,
                push
        );
    }
}
