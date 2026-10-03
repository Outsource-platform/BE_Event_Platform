package org.example.eventplatform.catalog.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.eventplatform.catalog.entity.Banner;
import org.example.eventplatform.catalog.entity.Post;
import org.example.eventplatform.catalog.entity.PostStatus;
import org.example.eventplatform.catalog.entity.ServiceCategory;
import org.example.eventplatform.catalog.repository.PostRepository;
import org.example.eventplatform.catalog.repository.BannerRepository;
import org.example.eventplatform.catalog.repository.ServiceCategoryRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
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

    /**
     * Hai bài mẫu của sàn để trang Tin tức không trống khi mới chạy. Chỉ seed khi chưa có bài nào;
     * Super Admin sửa hoặc xoá thoải mái.
     */
    @Bean
    public ApplicationRunner seedPosts(PostRepository repository) {
        return args -> {
            if (repository.count() > 0) {
                return;
            }
            repository.save(Post.builder()
                    .authorName("Occasio")
                    .title("Occasio ra mắt: đặt show và quản lý đoàn biểu diễn trên một nền tảng")
                    .slug("occasio-ra-mat-dat-show-va-quan-ly-doan-bieu-dien")
                    .excerpt("Giới thiệu Occasio: nơi khách thuê tìm đoàn biểu diễn, đơn vị quản lý show, thành viên và chia tiền.")
                    .content("<p>Occasio giúp các đoàn lân sư rồng, ban nhạc, MC và đơn vị tổ chức sự kiện quản lý toàn bộ công việc "
                            + "trên một nơi: lịch show, gói dịch vụ, thành viên và chia tiền.</p>"
                            + "<h2>Dành cho đơn vị biểu diễn</h2><ul><li>Quản lý lịch show và đơn khách đặt</li>"
                            + "<li>Phân công thành viên, chấm công, chia tiền minh bạch</li><li>Bán gói show ngay trên sàn</li></ul>"
                            + "<h2>Dành cho khách thuê</h2><p>Tìm đơn vị theo khu vực, xem gói và giá, gửi yêu cầu đặt show chỉ trong vài bước.</p>")
                    .seoTitle("Occasio - Nền tảng đặt show và quản lý đoàn biểu diễn")
                    .status(PostStatus.PUBLISHED)
                    .publishedAt(LocalDateTime.now())
                    .build());
            repository.save(Post.builder()
                    .authorName("Occasio")
                    .title("Cách chọn đoàn lân sư rồng cho lễ khai trương")
                    .slug("cach-chon-doan-lan-su-rong-cho-le-khai-truong")
                    .excerpt("Vài tiêu chí giúp chủ cửa hàng chọn đoàn lân phù hợp ngân sách, quy mô và không gian khai trương.")
                    .content("<p>Khai trương là dịp quan trọng, chọn đúng đoàn lân sẽ giúp buổi lễ thêm trọn vẹn.</p>"
                            + "<h2>1. Xác định quy mô và không gian</h2><p>Mặt bằng hẹp thích hợp màn múa lân gọn, "
                            + "sân rộng có thể thêm trống hội và rồng.</p>"
                            + "<h2>2. So sánh gói và giá</h2><p>Xem rõ thời lượng, số thành viên và các hạng mục kèm theo trước khi đặt.</p>"
                            + "<h2>3. Đặt sớm và chốt lịch</h2><p>Các ngày đẹp thường kín lịch, nên đặt trước ít nhất một tuần.</p>")
                    .status(PostStatus.PUBLISHED)
                    .publishedAt(LocalDateTime.now().minusDays(1))
                    .build());
            log.info("Seeded sample posts");
        };
    }
}
