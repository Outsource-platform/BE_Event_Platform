package org.example.eventplatform.event.repository;

import org.example.eventplatform.event.entity.ShowMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ShowMediaRepository extends JpaRepository<ShowMedia, Long> {

    List<ShowMedia> findByEventIdOrderBySortOrderAscIdAsc(Long eventId);

    List<ShowMedia> findByEventIdInOrderBySortOrderAscIdAsc(Collection<Long> eventIds);

    void deleteByEventId(Long eventId);
}
