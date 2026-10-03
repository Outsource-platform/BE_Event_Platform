package org.example.eventplatform.identity.repository;

import org.example.eventplatform.identity.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u " +
            "LEFT JOIN FETCH u.roles r " +
            "LEFT JOIN FETCH r.permissions " +
            "WHERE u.username = :username")
    Optional<User> findByUsername(@Param("username") String username);

    // Dùng cho nhánh tương thích ngược (bản app cũ đăng nhập không gửi tenant_domain) —
    // trả List thay vì Optional vì username không còn đảm bảo duy nhất toàn hệ thống.
    @Query("SELECT u FROM User u " +
            "LEFT JOIN FETCH u.roles r " +
            "LEFT JOIN FETCH r.permissions " +
            "WHERE u.username = :username")
    List<User> findAllByUsername(@Param("username") String username);

    boolean existsByUsername(String username);

    @Query("SELECT u FROM User u " +
            "LEFT JOIN FETCH u.roles r " +
            "LEFT JOIN FETCH r.permissions " +
            "WHERE u.tenant.id = :tenantId AND u.username = :username")
    Optional<User> findByTenantIdAndUsername(@Param("tenantId") Long tenantId, @Param("username") String username);

    boolean existsByTenantIdAndUsername(Long tenantId, String username);

    Page<User> findByTenantId(Long tenantId, Pageable pageable);

    Optional<User> findByIdAndTenantId(Long id, Long tenantId);

    Optional<User> findByTenantIdAndEmail(Long tenantId, String email);

    boolean existsByEmailAndTenantId(String email, Long tenantId);

    @Query("SELECT u FROM User u WHERE u.tenant.id = :tenantId AND u.roles.name = :roleName")
    List<User> findByTenantIdAndRoleName(@Param("tenantId") Long tenantId, @Param("roleName") String roleName);

    @Query("SELECT COUNT(DISTINCT u) FROM User u JOIN u.roles r WHERE r.name = :roleName")
    long countByRoleName(@Param("roleName") String roleName);
}
