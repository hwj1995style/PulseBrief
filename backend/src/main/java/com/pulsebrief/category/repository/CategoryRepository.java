package com.pulsebrief.category.repository;

import com.pulsebrief.category.domain.NewsCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<NewsCategory, Long> {
    List<NewsCategory> findByStatusOrderBySortNoAscIdAsc(Byte status);

    List<NewsCategory> findByCategoryCodeIn(List<String> categoryCodes);
}
