package org.example.eventplatform.customer.repository;

import org.example.eventplatform.customer.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    @Query("SELECT c FROM Customer c WHERE c.tenantId = :tenantId AND " +
            "(:ownerId IS NULL OR c.assignedToUserId = :ownerId) AND " +
            "(:keyword IS NULL OR :keyword = '' OR " +
            "LOWER(c.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "c.phone LIKE CONCAT('%', :keyword, '%'))")
    Page<Customer> searchCustomers(@Param("tenantId") Long tenantId,
                                    @Param("ownerId") Long ownerId,
                                    @Param("keyword") String keyword,
                                    Pageable pageable);

    boolean existsByPhoneAndTenantId(String phone, Long tenantId);

    Optional<Customer> findByIdAndTenantId(Long id, Long tenantId);

    Optional<Customer> findByPhoneAndTenantId(String phone, Long tenantId);

    List<Customer> findByUserId(Long userId);
}
