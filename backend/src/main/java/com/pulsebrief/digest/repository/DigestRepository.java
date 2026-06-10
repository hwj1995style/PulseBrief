package com.pulsebrief.digest.repository;

import com.pulsebrief.digest.domain.DailyDigest;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DigestRepository extends JpaRepository<DailyDigest, Long> {
    List<DailyDigest> findByDigestDateAndDigestStatusOrderByPublishTimeAscIdAsc(LocalDate digestDate, String status);

    Optional<DailyDigest> findFirstByDigestStatusOrderByDigestDateDescPublishTimeAsc(String status);

    Optional<DailyDigest> findByIdAndDigestStatus(Long id, String status);

    Page<DailyDigest> findByDigestStatusOrderByDigestDateDescPublishTimeDescIdDesc(String status, Pageable pageable);

    boolean existsByDigestDateAndDigestTypeAndDigestStatus(LocalDate digestDate, String digestType, String status);
}
