package org.example.eventplatform.event.repository;

import org.example.eventplatform.event.entity.AssignStatus;
import org.example.eventplatform.event.entity.UserEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserEventRepository extends JpaRepository<UserEvent, Long> {

    List<UserEvent> findByEventId(Long eventId);

    @Query("SELECT ue FROM UserEvent ue JOIN FETCH ue.event WHERE ue.userId = :userId")
    List<UserEvent> findByUserId(@Param("userId") Long userId);

    Optional<UserEvent> findByEventIdAndUserId(Long eventId, Long userId);

    // checkOut() sets COMPLETED — that is the real terminal status for a finished
    // assignment (CHECKED_OUT is never actually assigned by any transition).
    @Query("SELECT COUNT(ue) FROM UserEvent ue WHERE ue.event.tenantId = :tenantId " +
            "AND ue.userId = :userId AND ue.status = 'COMPLETED'")
    long countFinishedShows(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

    @Query("SELECT COUNT(ue) FROM UserEvent ue WHERE ue.event.tenantId = :tenantId " +
            "AND ue.userId = :userId AND (ue.status = 'ACCEPTED' OR ue.status = 'CHECKIN_CONCENTRATE')")
    long countPendingShows(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

    /** Điểm thực nhận từ các show đã hoàn thành: số đã chốt (sau khi trừ quỹ), show cũ chưa chốt thì lấy salary. */
    @Query("SELECT SUM(COALESCE(ue.netAmount, ue.salary)) FROM UserEvent ue WHERE ue.event.tenantId = :tenantId " +
            "AND ue.userId = :userId AND ue.status = 'COMPLETED'")
    BigDecimal sumTotalEarnings(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

    @Query("SELECT SUM(ue.salary) FROM UserEvent ue WHERE ue.event.tenantId = :tenantId " +
            "AND ue.userId = :userId AND ue.status = 'COMPLETED'")
    BigDecimal sumGrossEarnings(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

    @Query("SELECT SUM(ue.fundAmount) FROM UserEvent ue WHERE ue.event.tenantId = :tenantId " +
            "AND ue.userId = :userId AND ue.status = 'COMPLETED'")
    BigDecimal sumFundDeducted(@Param("tenantId") Long tenantId, @Param("userId") Long userId);
}
