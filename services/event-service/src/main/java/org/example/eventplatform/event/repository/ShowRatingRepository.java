package org.example.eventplatform.event.repository;

import org.example.eventplatform.event.entity.ShowRating;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShowRatingRepository extends JpaRepository<ShowRating, Long> {

    Page<ShowRating> findByEventIdOrderByIdDesc(Long eventId, Pageable pageable);

    Optional<ShowRating> findByEventIdAndUserId(Long eventId, Long userId);

    /** Mỗi dòng: [số sao, số lượt]. */
    @Query("SELECT r.stars, COUNT(r) FROM ShowRating r WHERE r.eventId = :eventId GROUP BY r.stars")
    List<Object[]> countByStars(@Param("eventId") Long eventId);
}
