package com.pulsebrief.playback.repository;

import com.pulsebrief.playback.domain.UserPlayHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPlayHistoryRepository extends JpaRepository<UserPlayHistory, Long> {
}
