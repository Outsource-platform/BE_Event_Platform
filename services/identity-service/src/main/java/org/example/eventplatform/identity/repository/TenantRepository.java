package org.example.eventplatform.identity.repository;

import org.example.eventplatform.identity.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findByDomain(String domain);

    Optional<Tenant> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByDomain(String domain);

    long countByActiveTrue();
}
