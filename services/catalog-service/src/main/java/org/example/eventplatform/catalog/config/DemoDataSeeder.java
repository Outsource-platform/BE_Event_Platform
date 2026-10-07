package org.example.eventplatform.catalog.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.eventplatform.catalog.entity.Banner;
import org.example.eventplatform.catalog.entity.Post;
import org.example.eventplatform.catalog.entity.PostStatus;
import org.example.eventplatform.catalog.repository.BannerRepository;
import org.example.eventplatform.catalog.repository.PostRepository;
import org.example.eventplatform.catalog.storage.StorageProperties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Banner trang chủ và bài viết mẫu về múa lân sư rồng để có nội dung đẹp khi giới thiệu app.
 * Banner chưa có ảnh nên app tự vẽ nền màu cùng tiêu đề; muốn ảnh thật thì tải lên trong web quản trị sau.
 * Chạy lại nhiều lần không tạo trùng; banner và bài của sàn cũ bị ẩn đúng một lần, lúc nạp bộ demo.
 */
@Component
@Order(10)
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "demo.seed.enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final String FIRST_BANNER = "Đón Tết cùng lân sư rồng";
    private static final String FIRST_POST_SLUG = "y-nghia-mua-lan-su-rong-trong-ngay-tet";

    private final BannerRepository bannerRepository;
    private final PostRepository postRepository;
    private final StorageProperties storage;

    // Ảnh minh hoạ tự vẽ (không phải ảnh thật): ghép tiêu đề banner và bài viết với tệp trong resources/demo-media.
    private static final Map<String, String> BANNER_IMAGES = Map.of(
            FIRST_BANNER, "demo-banner-tet",
            "Khai trương hồng phát", "demo-banner-khaitruong",
            "Trung thu rộn ràng", "demo-banner-trungthu",
            "Đặt show chỉ vài chạm", "demo-banner-datshow");
    private static final Map<String, String> POST_IMAGES = Map.of(
            FIRST_POST_SLUG, "demo-lehoi-1",
            "cach-chon-doan-lan-cho-le-khai-truong", "demo-khaitruong-1",
            "quy-trinh-dat-show-lan-su-rong-tren-stagio", "demo-damcuoi-2",
            "phan-mem-quan-ly-doan-lan-stagio", "demo-mungtho-1",
            "mua-lan-trung-thu-chuan-bi-gi", "demo-trungthu-1");

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        copyDemoMedia();
        seedBanners();
        seedPosts();
        applyImages();
    }

    /** Chép ảnh minh hoạ trong jar ra thư mục lưu trữ để phục vụ qua /api/files/local. Không ghi đè tệp đã có. */
    private void copyDemoMedia() {
        try {
            Path dir = Paths.get(storage.getLocalDir(), "images");
            Files.createDirectories(dir);
            for (Resource res : new PathMatchingResourcePatternResolver().getResources("classpath:demo-media/*.jpg")) {
                Path target = dir.resolve(res.getFilename());
                if (!Files.exists(target)) {
                    try (InputStream in = res.getInputStream()) {
                        Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Không chép được ảnh minh hoạ demo", e);
        }
    }

    private String mediaUrl(String name) {
        return storage.getPublicBaseUrl().replaceAll("/$", "") + "/api/files/local/" + name + ".jpg";
    }

    /** Gắn ảnh cho banner và bài viết demo còn thiếu ảnh; ảnh người dùng đã tải lên không bị đè. */
    private void applyImages() {
        for (Banner banner : bannerRepository.findAll()) {
            String image = BANNER_IMAGES.get(banner.getTitle());
            if (image != null && (banner.getImageUrl() == null || banner.getImageUrl().isBlank())) {
                banner.setImageUrl(mediaUrl(image));
                bannerRepository.save(banner);
            }
        }
        for (Post post : postRepository.findAll()) {
            String image = POST_IMAGES.get(post.getSlug());
            if (image != null && (post.getCoverImage() == null || post.getCoverImage().isBlank())) {
                post.setCoverImage(mediaUrl(image));
                postRepository.save(post);
            }
        }
    }

    private void seedBanners() {
        List<Banner> existing = bannerRepository.findAll();
        if (existing.stream().anyMatch(b -> FIRST_BANNER.equals(b.getTitle()))) {
            return;
        }
        existing.forEach(b -> b.setActive(false));
        bannerRepository.saveAll(existing);

        bannerRepository.saveAll(List.of(
                Banner.builder().title(FIRST_BANNER).subtitle("Đặt đoàn lân uy tín, lịch rõ ràng, giá minh bạch").sortOrder(1).build(),
                Banner.builder().title("Khai trương hồng phát").subtitle("Lân sư rồng chúc mừng, mở hàng đón lộc").sortOrder(2).build(),
                Banner.builder().title("Trung thu rộn ràng").subtitle("Múa lân đường phố, hội trăng rằm cho cả khu phố").sortOrder(3).build(),
                Banner.builder().title("Đặt show chỉ vài chạm").subtitle("Chọn đoàn, chọn gói, xác nhận ngay trên app").sortOrder(4).build()));
        log.info("Dữ liệu demo: đã nạp 4 banner, ẩn {} banner cũ", existing.size());
    }

    private void seedPosts() {
        if (postRepository.existsBySlug(FIRST_POST_SLUG)) {
            return;
        }
        for (Post old : postRepository.findByStatusOrderByPublishedAtDesc(PostStatus.PUBLISHED)) {
            if (old.getTenantId() == null) {
                old.setStatus(PostStatus.HIDDEN);
                postRepository.save(old);
            }
        }
        LocalDateTime now = LocalDateTime.now();
        List<Object[]> items = List.of(
                new Object[]{FIRST_POST_SLUG, "Ý nghĩa múa lân sư rồng trong ngày Tết",
                        "Vì sao tiếng trống lân lại không thể thiếu mỗi dịp xuân về? Cùng tìm hiểu ý nghĩa tâm linh và nét đẹp văn hoá của điệu múa truyền thống.",
                        """
                        <p>Mỗi dịp Tết đến xuân về, tiếng trống lân rộn ràng vang khắp phố phường đã trở thành một phần quen thuộc của văn hoá Việt. Múa lân sư rồng không chỉ là màn trình diễn vui mắt mà còn gửi gắm mong ước về một năm mới bình an, buôn bán thuận lợi.</p>
                        <h2>Lân mang theo điều gì?</h2>
                        <p>Theo quan niệm dân gian, con lân là linh vật xua đuổi điều xui rủi và mang lại may mắn. Khi lân vào nhà, vào cửa hàng, chủ nhà thường treo lì xì và "hái lộc" trên cao để lân vươn mình lấy, tượng trưng cho việc đón nhận tài lộc trọn vẹn.</p>
                        <h2>Sự khác nhau giữa lân, sư và rồng</h2>
                        <ul>
                        <li><strong>Lân:</strong> hai người điều khiển, nhịp trống nhanh, linh hoạt, thường diễn ở cửa tiệm và nhà dân.</li>
                        <li><strong>Sư tử:</strong> dáng vẻ oai nghiêm, thường kết hợp các màn trèo cột, nhảy cọc.</li>
                        <li><strong>Rồng:</strong> cần nhiều người cầm, uốn lượn theo đội hình, thích hợp lễ hội và sân khấu lớn.</li>
                        </ul>
                        <p>Hiểu rõ từng loại sẽ giúp bạn chọn đúng tiết mục cho không gian và ngân sách của mình.</p>
                        """},
                new Object[]{"cach-chon-doan-lan-cho-le-khai-truong", "Cách chọn đoàn lân cho lễ khai trương: 5 điều cần biết",
                        "Năm tiêu chí giúp chủ cửa hàng chọn được đoàn lân phù hợp ngân sách, đúng giờ đẹp và để lại ấn tượng tốt với khách đến chúc mừng.",
                        """
                        <p>Lễ khai trương là dịp quan trọng nhất của một cửa hàng mới, và màn múa lân thường là điểm nhấn thu hút người qua đường. Để buổi lễ suôn sẻ, hãy cân nhắc những điều sau.</p>
                        <h2>1. Xem đơn vị đã làm những show nào</h2>
                        <p>Đoàn có hình ảnh, đánh giá và lịch sử show rõ ràng sẽ đáng tin hơn. Trên Stagio, hồ sơ mỗi đoàn hiển thị khu vực hoạt động và các gói đang mở bán.</p>
                        <h2>2. Chốt giờ đẹp và thời lượng</h2>
                        <p>Thông thường lân múa 15 đến 30 phút gồm chào chủ, hái lộc và chúc phúc. Báo trước giờ khai trương để đoàn sắp xếp đội hình và xe di chuyển.</p>
                        <h2>3. Hỏi rõ trọn gói gồm gì</h2>
                        <ul>
                        <li>Số lượng nhạc công, người múa và trống.</li>
                        <li>Phí di chuyển, phụ thu ngoài giờ.</li>
                        <li>Có tặng thêm tiết mục, pháo giấy hay không.</li>
                        </ul>
                        <h2>4. Đặt cọc minh bạch</h2>
                        <p>Hãy thống nhất mức cọc và cách thanh toán ngay từ đầu, ghi rõ trong phiếu đặt show để hai bên cùng nắm.</p>
                        <h2>5. Đặt sớm vào mùa cao điểm</h2>
                        <p>Các ngày đẹp trong tháng và dịp cận Tết thường kín lịch từ sớm, nên đặt trước ít nhất một đến hai tuần.</p>
                        """},
                new Object[]{"quy-trinh-dat-show-lan-su-rong-tren-stagio", "Đặt show lân sư rồng trên Stagio chỉ với vài bước",
                        "Từ chọn đoàn, chọn gói đến xác nhận lịch diễn: toàn bộ quy trình đặt show được gói gọn ngay trong ứng dụng.",
                        """
                        <p>Trước đây muốn thuê đoàn lân, bạn phải gọi điện hỏi giá, hẹn lịch và tự theo dõi từng cuộc trao đổi. Với Stagio, mọi thứ nằm gọn trong một ứng dụng.</p>
                        <h2>Các bước thực hiện</h2>
                        <ol>
                        <li>Mở ứng dụng, xem các đoàn theo khu vực hoặc theo gói show nổi bật.</li>
                        <li>Chọn đoàn phù hợp, xem mô tả và giá từng gói.</li>
                        <li>Điền ngày giờ, địa điểm tổ chức và gửi yêu cầu đặt show.</li>
                        <li>Đoàn xác nhận, bạn nhận thông báo và theo dõi trạng thái ngay trên app.</li>
                        </ol>
                        <h2>Lợi ích cho khách thuê</h2>
                        <p>Lịch diễn, mã show và thông tin liên hệ được lưu lại rõ ràng, tránh nhầm lẫn giờ giấc và hạn chế phát sinh ngoài thoả thuận.</p>
                        """},
                new Object[]{"phan-mem-quan-ly-doan-lan-stagio", "Quản lý đoàn lân không còn là nỗi lo giấy tờ",
                        "Lịch show, thành viên, gói diễn và khách hàng được gom về một nơi, giúp trưởng đoàn tiết kiệm thời gian mỗi tuần.",
                        """
                        <p>Quản lý một đoàn lân nhiều người, nhiều show mỗi tuần đòi hỏi sự rõ ràng: ai đi show nào, khách là ai, đã cọc bao nhiêu. Ghi sổ tay hay nhắn tin nhóm rất dễ sót.</p>
                        <h2>Stagio giúp được gì cho trưởng đoàn</h2>
                        <ul>
                        <li>Lịch show theo ngày, theo tháng, tự cấp mã show để tra cứu nhanh.</li>
                        <li>Danh sách gói biểu diễn với mức giá riêng của đoàn.</li>
                        <li>Hồ sơ khách hàng và ghi chú sau mỗi show.</li>
                        <li>Quản lý thành viên và phân công người đi diễn.</li>
                        </ul>
                        <p>Đăng bài giới thiệu đoàn trên trang tin tức là hoàn toàn miễn phí, giúp khách tìm thấy bạn dễ hơn trên Google.</p>
                        """},
                new Object[]{"mua-lan-trung-thu-chuan-bi-gi", "Tổ chức múa lân Trung thu: cần chuẩn bị những gì?",
                        "Gợi ý cho ban tổ chức khu phố, trường học về địa điểm, an toàn và kịch bản một buổi múa lân Trung thu đáng nhớ.",
                        """
                        <p>Múa lân là tiết mục không thể thiếu của đêm hội trăng rằm. Để các em nhỏ vừa vui vừa an toàn, ban tổ chức nên chuẩn bị sớm.</p>
                        <h2>Địa điểm và an toàn</h2>
                        <p>Chọn sân rộng, mặt phẳng, có rào hoặc vạch phân cách khu biểu diễn với khu khán giả. Báo trước cho đoàn về diện tích để chọn tiết mục phù hợp.</p>
                        <h2>Kịch bản gợi ý</h2>
                        <ol>
                        <li>Lân ra mắt cùng tiếng trống khai hội.</li>
                        <li>Tương tác, tặng quà và bánh cho các em nhỏ.</li>
                        <li>Màn múa lân trên cọc hoặc ông Địa vui nhộn kết thúc chương trình.</li>
                        </ol>
                        <p>Đặt đoàn qua Stagio giúp bạn so sánh nhiều gói và chốt lịch nhanh chóng trước mùa cao điểm.</p>
                        """});

        int i = 0;
        for (Object[] item : items) {
            postRepository.save(Post.builder()
                    .authorName("Stagio")
                    .slug((String) item[0])
                    .title((String) item[1])
                    .excerpt((String) item[2])
                    .content((String) item[3])
                    .status(PostStatus.PUBLISHED)
                    .publishedAt(now.minusDays(i * 3L + 1))
                    .build());
            i++;
        }
        log.info("Dữ liệu demo: đã nạp {} bài viết", items.size());
    }
}
