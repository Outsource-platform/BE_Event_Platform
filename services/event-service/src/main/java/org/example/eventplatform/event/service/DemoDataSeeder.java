package org.example.eventplatform.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.eventplatform.event.client.CustomerServiceClient;
import org.example.eventplatform.event.client.IdentityServiceClient;
import org.example.eventplatform.event.entity.Event;
import org.example.eventplatform.event.entity.EventStatus;
import org.example.eventplatform.event.entity.EventType;
import org.example.eventplatform.event.entity.MediaType;
import org.example.eventplatform.event.entity.ShowMedia;
import org.example.eventplatform.event.repository.ShowMediaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.example.eventplatform.event.entity.ShowPackage;
import org.example.eventplatform.event.repository.EventRepository;
import org.example.eventplatform.event.repository.ShowPackageRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Gói biểu diễn và vài show mẫu cho các đơn vị demo do identity-service tạo. Đơn vị nào đã có gói hoặc show thì bỏ qua,
 * nên chạy lại không tạo trùng. Cần chạy sau khi identity-service đã nạp đơn vị demo.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "demo.seed.enabled", havingValue = "true")
public class DemoDataSeeder {

    private record Pack(String name, String description, long price) {
    }

    /** Show đã diễn kèm ảnh để đăng lên bảng tin; tên lấy theo vị trí của đoàn trong DEMO_DOMAINS để các đoàn không trùng tên nhau. */
    private record Gallery(String theme, EventType type, int dayOffset, String packName, List<String> names, String description) {
    }

    private record Demo(String name, EventType type, int dayOffset, LocalTime start, LocalTime end, EventStatus status,
                        String location, double lat, double lng, long total, long deposit, String customer, String phone,
                        String packName, String note) {
    }

    private static final List<Pack> PACKS = List.of(
            new Pack("Lân đơn chúc phúc", "Một cặp lân múa chúc phúc, hái lộc tại cửa hàng hoặc gia đình. Phù hợp khai trương nhỏ, tân gia, mừng thọ. Thời lượng khoảng 20 phút.", 2_500_000),
            new Pack("Lân sư rồng khai trương", "Hai lân kèm ông Địa, đội trống 4 người, múa hái lộc và chúc mừng. Có pháo giấy rải lộc. Thời lượng 30 đến 40 phút.", 4_500_000),
            new Pack("Trọn gói Lân Sư Rồng Hồng Phát", "Đội hình đầy đủ gồm lân, sư tử và rồng, nhạc công trống chiêng, tiết mục múa cọc. Phù hợp lễ hội, sự kiện doanh nghiệp, khai mạc. Thời lượng 45 đến 60 phút.", 9_000_000),
            new Pack("Lân Trung thu cho thiếu nhi", "Lân tương tác cùng các em nhỏ, ông Địa vui nhộn, tặng quà tại chỗ. Dành cho khu phố, trường học, trung tâm thương mại.", 3_500_000));

    private static final List<Demo> EVENTS = List.of(
            new Demo("Khai trương Tiệm Vàng Kim Ngân", EventType.GRAND_OPENING, 5, LocalTime.of(8, 30), LocalTime.of(9, 30), EventStatus.CONFIRMED,
                    "12 Hàng Bạc", 21.0345, 105.8531, 4_500_000, 1_500_000, "Anh Trần Minh Quân", "0900000101", "Lân sư rồng khai trương", "Chuẩn bị lì xì hái lộc tại cửa chính."),
            new Demo("Lễ cưới Minh Anh và Quốc Huy", EventType.WEDDING, 12, LocalTime.of(10, 0), LocalTime.of(11, 0), EventStatus.SCHEDULED,
                    "Trung tâm tiệc cưới Hoa Sen", 21.0278, 105.8342, 3_500_000, 1_000_000, "Chị Nguyễn Thu Hà", "0900000102", "Lân đơn chúc phúc", "Múa đón dâu tại sảnh."),
            new Demo("Mừng thọ cụ Lê Văn Bảo 90 tuổi", EventType.LONGEVITY_WISH, 20, LocalTime.of(9, 0), LocalTime.of(10, 0), EventStatus.SCHEDULED,
                    "Nhà văn hoá phường Ba Đình", 21.0369, 105.8344, 2_500_000, 800_000, "Anh Lê Hoàng Nam", "0900000103", "Lân đơn chúc phúc", null),
            new Demo("Khai mạc hội chợ Xuân Bính Ngọ", EventType.FESTIVAL, 45, LocalTime.of(7, 30), LocalTime.of(9, 0), EventStatus.PENDING_APPROVAL,
                    "Công viên Thống Nhất", 21.0147, 105.8442, 9_000_000, 3_000_000, "Ban tổ chức hội chợ", "0900000104", "Trọn gói Lân Sư Rồng Hồng Phát", "Chờ xác nhận lịch và mặt bằng."),
            new Demo("Lân Trung thu Khu phố 7", EventType.MID_AUTUMN, -12, LocalTime.of(18, 30), LocalTime.of(20, 0), EventStatus.COMPLETED,
                    "Sân khu phố 7", 21.0285, 105.8048, 3_500_000, 1_000_000, "Bà Phạm Thị Lan", "0900000105", "Lân Trung thu cho thiếu nhi", "Đã hoàn thành, khách hài lòng."),
            new Demo("Động thổ nhà xưởng Hưng Thịnh", EventType.GROUNDBREAKING, -25, LocalTime.of(8, 0), LocalTime.of(9, 0), EventStatus.COMPLETED,
                    "KCN Quang Minh", 21.1972, 105.7464, 4_500_000, 1_500_000, "Công ty Hưng Thịnh", "0900000106", "Lân sư rồng khai trương", null));

    private static final List<Gallery> GALLERIES = List.of(
            new Gallery("trungthu", EventType.MID_AUTUMN, -12, "Lân Trung thu cho thiếu nhi", List.of(
                    "Lân Trung thu Khu phố 7", "Lân Trung thu Chung cư Sunrise", "Lân Trung thu Trường tiểu học Lê Chân",
                    "Đêm hội Trăng Rằm phường Hải Châu", "Trung thu Làng Hoa Phú Xuân", "Lân Trung thu Công viên Ninh Kiều",
                    "Lân Trung thu Phố Cổ Hàng Mã", "Đêm Trung thu Nhà văn hoá Chợ Lớn"),
                    "Lân tương tác cùng các em nhỏ, ông Địa vui nhộn tặng bánh và kẹo giữa sân khu phố. Hơn 200 em thiếu nhi cùng phụ huynh tham gia, tiếng trống rộn ràng suốt buổi tối."),
            new Gallery("dongtho", EventType.GROUNDBREAKING, -25, "Lân sư rồng khai trương", List.of(
                    "Động thổ nhà xưởng Hưng Thịnh", "Động thổ dự án Nhà Xanh Bình Thạnh", "Động thổ khu công nghiệp Đình Vũ",
                    "Động thổ toà nhà văn phòng Sông Hàn", "Động thổ resort Hương Giang", "Động thổ khu dân cư Cái Khế",
                    "Động thổ trường mầm non Ánh Dương", "Động thổ nhà máy Phú Mỹ"),
                    "Lân múa chúc khởi công thuận lợi, sau đó chủ đầu tư cùng khách mời thực hiện nghi thức động thổ. Đoàn hoàn thành đúng giờ hoàng đạo theo yêu cầu của gia chủ."),
            new Gallery("khaitruong", EventType.GRAND_OPENING, -40, "Lân sư rồng khai trương", List.of(
                    "Khai trương tiệm vàng Kim Ngân", "Khai trương cửa hàng Minh Châu", "Khai trương showroom nội thất An Cư",
                    "Khai trương nhà hàng Hải Sản Biển Đông", "Khai trương siêu thị mini Phú Gia", "Khai trương phòng khám Tâm Đức",
                    "Khai trương quán cà phê Nắng Mai", "Khai trương cửa hàng điện máy Thành Công"),
                    "Hai lân đỏ cùng đội trống bốn người múa chào chủ, hái lộc trước cửa tiệm rồi rải pháo giấy chúc mừng. Hơn một trăm khách và người qua đường dừng lại xem, chủ nhà hài lòng vì không khí rộn ràng đúng giờ đẹp."),
            new Gallery("damcuoi", EventType.WEDDING, -55, "Lân đơn chúc phúc", List.of(
                    "Lễ cưới Minh Anh và Quốc Huy", "Lễ cưới Thanh Tâm và Đức Thịnh", "Lễ cưới Hồng Nhung và Văn Dũng",
                    "Lễ cưới Khánh Linh và Hoàng Nam", "Lễ cưới Thuỳ Dương và Tuấn Kiệt", "Lễ cưới Bảo Ngọc và Gia Huy",
                    "Lễ cưới Mai Phương và Anh Tú", "Lễ cưới Ngọc Hân và Đình Phong"),
                    "Cặp lân múa đón dâu rể tại sảnh tiệc, chúc phúc hai họ rồi tặng lộc đầu năm cho cô dâu chú rể. Tiết mục kéo dài khoảng hai mươi phút, được hai bên gia đình khen ngợi."),
            new Gallery("mungtho", EventType.LONGEVITY_WISH, -68, "Lân đơn chúc phúc", List.of(
                    "Mừng thọ cụ Lê Văn Bảo 90 tuổi", "Mừng thọ bà Trần Thị Mai 85 tuổi", "Mừng thọ ông Phạm Văn Khôi 88 tuổi",
                    "Mừng thọ bà Nguyễn Thị Sen 80 tuổi", "Mừng thọ ông Đỗ Văn Tín 90 tuổi", "Mừng thọ cụ Hoàng Thị Lụa 92 tuổi",
                    "Mừng thọ ông Bùi Quang Vinh 85 tuổi", "Mừng thọ bà Võ Thị Hạnh 88 tuổi"),
                    "Lân múa chúc thọ trước sân nhà, con cháu quây quần cùng xem. Đoàn mang bộ đồ lân đỏ vàng và nhịp trống nhẹ nhàng, phù hợp với người cao tuổi."));

    /** Tên show riêng cho từng đoàn (cùng thứ tự với EVENTS), để danh sách show chung không bị lặp một mẫu. */
    private record Variant(int dayShift, String city, double lat, double lng, List<String> names) {
    }

    private static final Map<String, Variant> VARIANTS = Map.of(
            "langiaphat", new Variant(2, "TP. Hồ Chí Minh", 10.7725, 106.6980, List.of(
                    "Khai trương showroom nội thất An Cư", "Lễ cưới Thanh Tâm và Đức Thịnh", "Mừng thọ bà Trần Thị Mai 85 tuổi",
                    "Khai mạc lễ hội Xuân Phố Đi Bộ", "Lân Trung thu Chung cư Sunrise", "Động thổ dự án Nhà Xanh Bình Thạnh")),
            "lanlongvan", new Variant(4, "Hải Phòng", 20.8449, 106.6881, List.of(
                    "Khai trương nhà hàng Hải Sản Biển Đông", "Lễ cưới Hồng Nhung và Văn Dũng", "Mừng thọ ông Phạm Văn Khôi 88 tuổi",
                    "Lễ hội Hoa Phượng Đỏ", "Lân Trung thu Trường tiểu học Lê Chân", "Động thổ khu công nghiệp Đình Vũ")));

    // Khớp danh sách đơn vị do identity-service nạp; chỉ ba đoàn đầu có thêm show mẫu.
    private static final List<String> DEMO_DOMAINS = List.of("landainam", "lanthanglong", "langiaphat", "lankimlong",
            "lanlongvan", "lanhaichau", "lancodohue", "lantaydo");
    private static final List<String> SHOWCASE = List.of("landainam", "langiaphat", "lanlongvan");

    private final IdentityServiceClient identityClient;
    private final CustomerServiceClient customerClient;
    private final ShowPackageRepository packageRepository;
    private final EventRepository eventRepository;
    private final ShowCodeService showCodeService;
    private final ShowMediaRepository mediaRepository;

    // Nơi catalog-service phục vụ ảnh minh hoạ demo (chúng được chép sang kho lưu trữ khi catalog khởi động).
    @Value("${demo.seed.media-base-url:https://muong14.xyz}")
    private String mediaBaseUrl;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        try {
            seed();
        } catch (Exception ex) {
            log.error("Không nạp được dữ liệu demo cho event-service", ex);
        }
    }

    private void seed() {
        Map<String, IdentityServiceClient.PublicTenant> byDomain = identityClient.findPublicTenants().stream()
                .filter(t -> t.domain() != null)
                .collect(Collectors.toMap(IdentityServiceClient.PublicTenant::domain, Function.identity(), (a, b) -> a));
        int packs = 0;
        int events = 0;
        for (var entry : byDomain.entrySet()) {
            String domain = entry.getKey();
            Long tenantId = entry.getValue().id();
            if (!DEMO_DOMAINS.contains(domain)) {
                continue;
            }
            if (packageRepository.findByTenantId(tenantId).isEmpty()) {
                packs += seedPackages(tenantId, domain);
            }
            if (SHOWCASE.contains(domain) && !eventRepository.findByTenantId(tenantId, PageRequest.of(0, 1)).hasContent()) {
                events += seedEvents(tenantId, domain);
            }
            varyEvents(tenantId, domain);
            seedGalleries(tenantId, DEMO_DOMAINS.indexOf(domain));
        }
        log.info("Dữ liệu demo: nạp {} gói show, {} show mẫu", packs, events);
    }

    private int seedPackages(Long tenantId, String domain) {
        // Mỗi đoàn lấy 3 gói, lệch nhau một chút để danh sách trên sàn không giống hệt nhau.
        int shift = Math.abs(domain.hashCode()) % PACKS.size();
        BigDecimal factor = BigDecimal.valueOf(100 + (Math.abs(domain.hashCode()) % 4) * 5L, 2);
        int count = 0;
        for (int i = 0; i < 3; i++) {
            Pack pack = PACKS.get((shift + i) % PACKS.size());
            BigDecimal price = BigDecimal.valueOf(pack.price()).multiply(factor)
                    .divide(BigDecimal.valueOf(100_000), 0, java.math.RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100_000));
            packageRepository.save(ShowPackage.builder()
                    .tenantId(tenantId).name(pack.name()).description(pack.description()).price(price).active(true).build());
            count++;
        }
        return count;
    }

    /** Đổi tên, ngày và địa điểm của show mẫu còn mang tên mẫu gốc sang bản riêng của đoàn; chạy lại thì không còn gì để đổi. */
    private void varyEvents(Long tenantId, String domain) {
        Variant variant = VARIANTS.get(domain);
        if (variant == null) {
            return;
        }
        var page = eventRepository.findByTenantId(tenantId, PageRequest.of(0, 100));
        for (Event event : page.getContent()) {
            for (int i = 0; i < EVENTS.size(); i++) {
                if (EVENTS.get(i).name().equals(event.getName())) {
                    event.setName(variant.names().get(i));
                    event.setEventDate(event.getEventDate().plusDays(variant.dayShift()));
                    event.setLocation(variant.city());
                    event.setVenueLat(variant.lat());
                    event.setVenueLng(variant.lng());
                    eventRepository.save(event);
                }
            }
        }
    }

    /** Mỗi đoàn có vài show đã diễn kèm hai ảnh và mô tả, đăng sẵn lên bảng tin Khám phá. Chạy lại không tạo trùng. */
    private void seedGalleries(Long tenantId, int index) {
        var existing = eventRepository.findByTenantId(tenantId, PageRequest.of(0, 200)).getContent();
        var packages = packageRepository.findByTenantId(tenantId);
        var customer = customerClient.findOrCreate(tenantId, "09000002" + String.format("%02d", index), null, "Khách lẻ", null);
        LocalDate today = LocalDate.now();
        for (Gallery g : GALLERIES) {
            String name = g.names().get(index % g.names().size());
            var same = existing.stream().filter(e -> name.equals(e.getName())).findFirst();
            if (same.isPresent()) {
                // Show mẫu cũ trùng tên (do bộ show ban đầu tạo): bật trưng bày cho nó thay vì tạo thêm một show nữa.
                Event old = same.get();
                if (!Boolean.TRUE.equals(old.getShowcasePublished()) && old.getStatus() == EventStatus.COMPLETED) {
                    if (old.getPackageId() == null) {
                        packages.stream().filter(p -> g.packName().equals(p.getName())).findFirst().ifPresent(p -> {
                            old.setPackageId(p.getId());
                            old.setPackageName(p.getName());
                        });
                    }
                    old.setShowcasePublished(true);
                    old.setShowcaseTitle(name);
                    old.setShowcaseDescription(g.description());
                    eventRepository.save(old);
                    addGalleryMedia(old.getId(), g.theme());
                }
                continue;
            }
            var pack = packages.stream().filter(p -> g.packName().equals(p.getName())).findFirst()
                    .orElse(packages.isEmpty() ? null : packages.get(0));
            LocalDate date = today.plusDays(g.dayOffset() - index);
            Event event = Event.builder()
                    .name(name).type(g.type()).status(EventStatus.COMPLETED)
                    .eventDate(date).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 0))
                    .customerId(customer.id()).tenantId(tenantId)
                    .totalAmount(pack == null ? BigDecimal.ZERO : pack.getPrice()).platformFee(BigDecimal.ZERO)
                    .packageId(pack == null ? null : pack.getId()).packageName(pack == null ? null : pack.getName())
                    .showcasePublished(true).showcaseTitle(name).showcaseDescription(g.description())
                    .build();
            showCodeService.assign(event);
            event = eventRepository.save(event);
            addGalleryMedia(event.getId(), g.theme());
        }
    }

    private void addGalleryMedia(Long eventId, String theme) {
        if (!mediaRepository.findByEventIdOrderBySortOrderAscIdAsc(eventId).isEmpty()) {
            return;
        }
        for (int i = 1; i <= 2; i++) {
            mediaRepository.save(ShowMedia.builder().eventId(eventId).type(MediaType.IMAGE)
                    .url(mediaBaseUrl.replaceAll("/$", "") + "/api/files/local/demo-" + theme + "-" + i + ".jpg")
                    .sortOrder(i - 1).build());
        }
    }

    private int seedEvents(Long tenantId, String domain) {
        LocalDate today = LocalDate.now();
        int count = 0;
        for (Demo d : EVENTS) {
            var customer = customerClient.findOrCreate(tenantId, d.phone(), null, d.customer(), null);
            Event event = Event.builder()
                    .name(d.name()).type(d.type()).status(d.status())
                    .eventDate(today.plusDays(d.dayOffset())).startTime(d.start()).endTime(d.end())
                    .location(d.location()).venueLat(d.lat()).venueLng(d.lng()).checkinRadiusMeters(150)
                    .customerId(customer.id()).tenantId(tenantId)
                    .totalAmount(BigDecimal.valueOf(d.total())).depositAmount(BigDecimal.valueOf(d.deposit()))
                    .platformFee(BigDecimal.ZERO)
                    .concentrateTime(d.start().minusHours(1)).concentrateLocation("Kho lân của đoàn")
                    .packageName(d.packName()).description(d.note())
                    .build();
            showCodeService.assign(event);
            eventRepository.save(event);
            count++;
        }
        return count;
    }
}
