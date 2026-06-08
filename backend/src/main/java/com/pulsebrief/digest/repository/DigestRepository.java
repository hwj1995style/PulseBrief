package com.pulsebrief.digest.repository;

import com.pulsebrief.digest.domain.DailyDigest;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DigestRepository extends JpaRepository<DailyDigest, Long> {
    List<DailyDigest> findByDigestDateAndDigestStatusOrderByPublishTimeAscIdAsc(LocalDate digestDate, String status);

    Optional<DailyDigest> findFirstByDigestStatusOrderByDigestDateDescPublishTimeAsc(String status);

    Optional<DailyDigest> findByIdAndDigestStatus(Long id, String status);
}
