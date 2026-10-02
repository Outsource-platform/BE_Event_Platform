package org.example.eventplatform.catalog.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.eventplatform.catalog.entity.Banner;
import org.example.eventplatform.catalog.entity.ServiceCategory;
import org.example.eventplatform.catalog.repository.BannerRepository;
import org.example.eventplatform.catalog.repository.ServiceCategoryRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Danh mục dịch vụ là trục phân loại của sàn khách hàng, nên phải có sẵn từ lần
 * chạy đầu. {@code code} khớp với {@code Tenant.category} để nhóm được các đơn vị
 * hiện có mà không phải backfill dữ liệu.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataSeeder {

    private record Seed(String code, String name, String description) {
    }

    private static final List<Seed> BASELINE_CATEGORIES = List.of(
            new Seed("LION_DANCE", "Lân Sư Rồng", "Múa lân, múa rồng, trống hội cho khai trương và lễ hội"),
            new Seed("MUSIC", "Ca nhạc", "Ban nhạc, ca sĩ, nhóm nhảy biểu diễn sự kiện"),
            new Seed("MC", "Dẫn chương trình", "MC cho tiệc cưới, sự kiện doanh nghiệp"),
            new Seed("SOUND_LIGHT", "Âm thanh ánh sáng", "Thiết bị âm thanh, ánh sáng, sân khấu"),
            new Seed("WEDDING_PLANNER", "Trang trí tiệc cưới", "Trang trí, cổng hoa, backdrop tiệc cưới")
    );

    @Bean
    public ApplicationRunner seedServiceCategories(ServiceCategoryRepository repository) {
        return args -> BASELINE_CATEGORIES.forEach(seed -> {
            if (repository.findByCode(seed.code()).isEmpty()) {
                repository.save(ServiceCategory.builder()
                        .code(seed.code())
                        .name(seed.name())
                        .description(seed.description())
                        .active(true)
                        .build());
                log.info("Seeded service category {}", seed.code());
            }
        });
    }

    /**
     * Vài banner mẫu (chưa có ảnh, app tự vẽ nền màu) để slider trang chủ có nội dung
     * ngay từ đầu. Chỉ seed khi bảng trống, SUPER_ADMIN sửa hoặc xóa thoải mái.
     */
    @Bean
    public ApplicationRunner seedBanners(BannerRepository repository) {
        return args -> {
            if (repository.count() > 0) {
                return;
            }
            repository.save(Banner.builder().title("Đặt đoàn biểu diễn cho mọi dịp")
                    .subtitle("Xem giá, so sánh và gửi yêu cầu ngay trong app").sortOrder(1).active(true).build());
            repository.save(Banner.builder().title("Lân Sư Rồng đón khai trương")
                    .subtitle("Cầu may mắn, thu hút khách ngày mở cửa").sortOrder(2).active(true).build());
            repository.save(Banner.builder().title("Trọn gói tiệc cưới & sự kiện")
                    .subtitle("Ca nhạc, MC, âm thanh ánh sáng, trang trí").sortOrder(3).active(true).build());
            log.info("Seeded sample banners");
        };
    }
}
