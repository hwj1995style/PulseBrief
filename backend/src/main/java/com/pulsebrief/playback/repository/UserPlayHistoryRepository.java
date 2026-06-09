package com.pulsebrief.playback.repository;

import com.pulsebrief.playback.domain.UserPlayHistory;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPlayHistoryRepository extends JpaRepository<UserPlayHistory, Long> {
    Integer countByUserId(Long userId);

    List<UserPlayHistory> findByUserIdOrderByPlayTimeDesc(Long userId, Pageable pageable);
}
