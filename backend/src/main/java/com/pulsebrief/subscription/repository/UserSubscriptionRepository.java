package com.pulsebrief.subscription.repository;

import com.pulsebrief.subscription.domain.UserSubscription;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {
    List<UserSubscription> findByUserIdAndStatusOrderBySortNoAscIdAsc(Long userId, Byte status);

    Integer countByUserIdAndStatus(Long userId, Byte status);

    @Transactional
    void deleteByUserId(Long userId);
}
