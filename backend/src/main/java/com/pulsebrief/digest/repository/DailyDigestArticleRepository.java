package com.pulsebrief.digest.repository;

import com.pulsebrief.digest.domain.DailyDigestArticle;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyDigestArticleRepository extends JpaRepository<DailyDigestArticle, Long> {
    List<DailyDigestArticle> findByDigestIdOrderBySortNoAscIdAsc(Long digestId);

    void deleteByDigestId(Long digestId);

    long countByDigestId(Long digestId);
}
