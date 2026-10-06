package org.example.eventplatform.customer.repository;

import org.example.eventplatform.customer.entity.CustomerNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerNoteRepository extends JpaRepository<CustomerNote, Long> {

    List<CustomerNote> findByCustomerIdAndTenantIdOrderByCreatedAtDesc(Long customerId, Long tenantId);

    Optional<CustomerNote> findByIdAndCustomerIdAndTenantId(Long id, Long customerId, Long tenantId);
}
