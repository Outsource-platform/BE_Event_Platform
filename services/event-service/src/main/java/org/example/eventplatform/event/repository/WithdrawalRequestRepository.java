package org.example.eventplatform.event.repository;

import org.example.eventplatform.event.entity.WithdrawalRequest;
import org.example.eventplatform.event.entity.WithdrawalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, Long> {

    List<WithdrawalRequest> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<WithdrawalRequest> findByTenantIdOrderByCreatedAtDesc(Long tenantId);

    Optional<WithdrawalRequest> findByIdAndTenantId(Long id, Long tenantId);

    @Query("SELECT COALESCE(SUM(w.amount), 0) FROM WithdrawalRequest w " +
            "WHERE w.userId = :userId AND w.status IN ('PENDING', 'APPROVED')")
    BigDecimal sumReservedOrPaid(@Param("userId") Long userId);

    long countByUserIdAndStatus(Long userId, WithdrawalStatus status);

    List<WithdrawalRequest> findByTenantIdAndStatusOrderByCreatedAtDesc(Long tenantId, WithdrawalStatus status);
}
