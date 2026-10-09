package org.example.eventplatform.event.repository;

import org.example.eventplatform.event.entity.Event;
import org.example.eventplatform.event.entity.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    Page<Event> findByTenantId(Long tenantId, Pageable pageable);

    /** Bảng tin Khám phá: show đoàn đã chọn trưng bày, của các đoàn đang hoạt động. */
    Page<Event> findByShowcasePublishedTrueAndStatusInAndTenantIdIn(java.util.Collection<EventStatus> statuses, java.util.Collection<Long> tenantIds, Pageable pageable);

    long countByStatus(EventStatus status);

    /** Hoa hồng của thành viên từ các show họ tự tạo và đã hoàn thành. */
    @Query("SELECT SUM(e.creatorCommissionAmount) FROM Event e WHERE e.tenantId = :tenantId " +
            "AND e.createdByUserId = :userId AND e.status = org.example.eventplatform.event.entity.EventStatus.COMPLETED")
    java.math.BigDecimal sumCommission(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

    List<Event> findByTenantIdAndEventDateBetween(Long tenantId, LocalDate start, LocalDate end);

    Page<Event> findByTenantIdAndEventDateBetween(Long tenantId, LocalDate start, LocalDate end, Pageable pageable);

    List<Event> findByCustomerIdInOrderByCreatedAtDesc(List<Long> customerIds);

    boolean existsByTenantIdAndCustomerIdInAndStatusIn(Long tenantId, Collection<Long> customerIds, Collection<EventStatus> statuses);

    List<Event> findByTenantIdAndCustomerIdOrderByEventDateDesc(Long tenantId, Long customerId);

    List<Event> findByShowCodeIsNullAndTenantIdIsNotNullAndEventDateIsNotNullOrderByIdAsc();

    @Query("select e.showCode from Event e where e.tenantId = :tenantId and e.showCode like concat(:prefix, '%')")
    List<String> findShowCodesByPrefix(@Param("tenantId") Long tenantId, @Param("prefix") String prefix);
}
