package org.example.eventplatform.event.repository;

import org.example.eventplatform.event.entity.Event;
import org.example.eventplatform.event.entity.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    Page<Event> findByTenantId(Long tenantId, Pageable pageable);

    long countByStatus(EventStatus status);

    List<Event> findByTenantIdAndEventDateBetween(Long tenantId, LocalDate start, LocalDate end);

    Page<Event> findByTenantIdAndEventDateBetween(Long tenantId, LocalDate start, LocalDate end, Pageable pageable);

    List<Event> findByCustomerIdInOrderByCreatedAtDesc(List<Long> customerIds);
}
