package org.example.eventplatform.identity.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.eventplatform.identity.entity.Role;
import org.example.eventplatform.identity.entity.User;
import org.example.eventplatform.identity.entity.UserStatus;
import org.example.eventplatform.identity.repository.RoleRepository;
import org.example.eventplatform.identity.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Bootstraps baseline roles and the first SUPER_ADMIN so the platform is
 * testable from a blank database without a separate seed script.
 */
// Phải chạy trước mọi bộ nạp khác (vd DemoDataSeeder cần vai trò ADMIN đã tồn tại): ApplicationRunner không có @Order chạy sau cùng.
@Order(0)
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements ApplicationRunner {

    private static final List<String> BASELINE_ROLES = List.of("SUPER_ADMIN", "ADMIN", "TN_MEMBER", "CUSTOMER");

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${bootstrap.super-admin.username}")
    private String superAdminUsername;

    @Value("${bootstrap.super-admin.password}")
    private String superAdminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        BASELINE_ROLES.forEach(this::ensureRole);

        if (!userRepository.existsByUsername(superAdminUsername)) {
            Role superAdminRole = roleRepository.findByName("SUPER_ADMIN").orElseThrow();
            User superAdmin = User.builder()
                    .username(superAdminUsername)
                    .password(passwordEncoder.encode(superAdminPassword))
                    .fullName("Platform Super Admin")
                    .tenant(null)
                    .roles(superAdminRole)
                    .status(UserStatus.ACTIVE)
                    .isActive(true)
                    .isVerified(true)
                    .build();
            userRepository.save(superAdmin);
            log.warn("Seeded bootstrap SUPER_ADMIN '{}' — change its password after first login", superAdminUsername);
        }
    }

    private void ensureRole(String name) {
        if (roleRepository.findByName(name).isEmpty()) {
            Role role = Role.builder().name(name).build();
            roleRepository.save(role);
        }
    }
}
