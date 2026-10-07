package org.example.eventplatform.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.eventplatform.event.client.CustomerServiceClient;
import org.example.eventplatform.event.client.IdentityServiceClient;
import org.example.eventplatform.event.entity.Event;
import org.example.eventplatform.event.entity.EventStatus;
import org.example.eventplatform.event.entity.EventType;
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
