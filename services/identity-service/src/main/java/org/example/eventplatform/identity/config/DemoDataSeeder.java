package org.example.eventplatform.identity.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.eventplatform.identity.entity.RegistrationStatus;
import org.example.eventplatform.identity.entity.Role;
import org.example.eventplatform.identity.entity.Tenant;
import org.example.eventplatform.identity.entity.User;
import org.example.eventplatform.identity.entity.UserStatus;
import org.example.eventplatform.identity.repository.RoleRepository;
import org.example.eventplatform.identity.repository.TenantRepository;
import org.example.eventplatform.identity.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Tạo vài đoàn lân sư rồng mẫu ở nhiều tỉnh thành, mỗi đoàn có một tài khoản admin, để có dữ liệu đẹp đi giới thiệu app.
 * Chạy lại nhiều lần không tạo trùng. Tỉnh và phường phải khớp danh mục địa giới trong event-service.
 */
@Component
@Order(10)
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "demo.seed.enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private record DemoTenant(String domain, String name, String province, String ward, String primary, String accent) {
    }

    private static final List<DemoTenant> TENANTS = List.of(
            new DemoTenant("landainam", "Lân Sư Rồng Đại Nam", "Hà Nội", "Ba Đình", "#B71C1C", "#F9A825"),
            new DemoTenant("lanthanglong", "Đoàn Lân Thăng Long", "Hà Nội", "Hoàn Kiếm", "#C62828", "#FFB300"),
            new DemoTenant("langiaphat", "Lân Sư Rồng Gia Phát", "Hồ Chí Minh", "Bến Thành", "#AD1457", "#FFC107"),
            new DemoTenant("lankimlong", "Đoàn Lân Kim Long Chợ Lớn", "Hồ Chí Minh", "Chợ Lớn", "#BF360C", "#FFD54F"),
            new DemoTenant("lanlongvan", "Lân Sư Rồng Long Vân", "Hải Phòng", "Hồng Bàng", "#8E0000", "#FFCA28"),
            new DemoTenant("lanhaichau", "Đoàn Lân Hải Châu", "Đà Nẵng", "Hải Châu", "#D84315", "#FFD600"),
            new DemoTenant("lancodohue", "Đoàn Lân Cố Đô Huế", "Huế", "Phú Xuân", "#6A1B1A", "#FFA000"),
            new DemoTenant("lantaydo", "Lân Sư Rồng Tây Đô", "Cần Thơ", "Ninh Kiều", "#C2185B", "#FFB74D")
    );

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${demo.seed.admin-password:}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminPassword == null || adminPassword.isBlank()) {
            log.warn("demo.seed.enabled bật nhưng thiếu DEMO_ADMIN_PASSWORD — bỏ qua dữ liệu demo");
            return;
        }
        Role adminRole = roleRepository.findByName("ADMIN").orElseThrow();
        int created = 0;
        for (DemoTenant demo : TENANTS) {
            if (tenantRepository.existsByDomain(demo.domain())) {
                continue;
            }
            Tenant tenant = new Tenant();
            tenant.setName(demo.name());
            tenant.setDomain(demo.domain());
            tenant.setEmail(demo.domain() + "@demo.stagio.vn");
            tenant.setCategory("LION_DANCE");
            tenant.setProvince(demo.province());
            tenant.setWard(demo.ward());
            tenant.setPrimaryColorHex(demo.primary());
            tenant.setAccentColorHex(demo.accent());
            tenant.setActive(true);
            tenant.setIsVerified(true);
            tenant.setStatusConfirm(RegistrationStatus.ACTIVE);
            tenant = tenantRepository.save(tenant);

            userRepository.save(User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode(adminPassword))
                    .email(tenant.getEmail())
                    .fullName("Trưởng đoàn " + demo.name())
                    .tenant(tenant)
                    .roles(adminRole)
                    .status(UserStatus.ACTIVE)
                    .isActive(true)
                    .isVerified(true)
                    .build());
            created++;
        }

        // Đơn vị thử nghiệm cũ không nên hiện trên sàn khi đi giới thiệu.
        tenantRepository.findByDomain("qatesttenant").ifPresent(t -> {
            if (t.isActive()) {
                t.setActive(false);
                log.info("Ẩn đơn vị thử nghiệm qatesttenant khỏi sàn");
            }
        });
        log.info("Dữ liệu demo: tạo mới {} đơn vị lân sư rồng", created);
    }
}
