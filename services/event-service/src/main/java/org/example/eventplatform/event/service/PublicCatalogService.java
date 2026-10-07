package org.example.eventplatform.event.service;

import lombok.RequiredArgsConstructor;
import org.example.eventplatform.event.client.CatalogServiceClient;
import org.example.eventplatform.event.client.IdentityServiceClient;
import org.example.eventplatform.event.dto.HomeAppResponse;
import org.example.eventplatform.event.dto.PlaceResponse;
import org.example.eventplatform.event.dto.PublicPackageResponse;
import org.example.eventplatform.event.dto.PublicShowPage;
import org.example.eventplatform.event.dto.PublicShowResponse;
import org.example.eventplatform.event.dto.MediaDto;
import org.example.eventplatform.event.dto.PackageOption;
import org.example.eventplatform.event.entity.MediaType;
import org.example.eventplatform.event.dto.PublicTroupeResponse;
import org.example.eventplatform.event.entity.Event;
import org.example.eventplatform.event.entity.EventStatus;
import org.example.eventplatform.event.entity.ShowMedia;
import org.example.eventplatform.event.entity.ShowPackage;
import org.example.eventplatform.event.repository.EventRepository;
import org.example.eventplatform.event.repository.ShowMediaRepository;
import org.example.eventplatform.event.repository.ShowPackageRepository;
import org.example.eventplatform.shared.cache.TtlCache;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.text.Collator;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Sàn công khai cho khách thuê sự kiện: duyệt đoàn và gói show của mọi đoàn.
 * Không nhận JwtPrincipal vì khách vãng lai chưa đăng nhập cũng xem được, nên
 * mọi truy vấn ở đây chỉ được chạm vào dữ liệu đã đánh dấu công khai
 * (đoàn đang hoạt động, gói đang mở bán).
 */
@Service
@RequiredArgsConstructor
public class PublicCatalogService {

    private static final int FEATURED_LIMIT = 10;
    private static final int RELATED_LIMIT = 6;

    private static final List<EventStatus> PUBLIC_SHOW_STATUSES =
            List.of(EventStatus.SCHEDULED, EventStatus.CONFIRMED, EventStatus.IN_PROGRESS, EventStatus.COMPLETED);

    private final EventRepository eventRepository;
    private final ShowMediaRepository showMediaRepository;
    private final ShowPackageRepository showPackageRepository;
    private final IdentityServiceClient identityServiceClient;
    private final CatalogServiceClient catalogServiceClient;
    private final PlaceCatalog placeCatalog;

    @Transactional(readOnly = true)
    public List<PublicTroupeResponse> listTroupes(String category, String province, String ward) {
        Map<Long, List<ShowPackage>> packagesByTenant = activePackagesByTenant();
        return identityServiceClient.findPublicTenants().stream()
                .filter(t -> category == null || category.isBlank() || category.equalsIgnoreCase(t.category()))
                .filter(t -> sameArea(province, t.province()))
                .filter(t -> sameArea(ward, t.ward()))
                .map(tenant -> toTroupe(tenant, packagesByTenant.getOrDefault(tenant.id(), List.of()), false))
                .toList();
    }

    /** Bảng tin Khám phá: các show đoàn đã đăng trưng bày, mới nhất trước, không giới hạn theo tháng. */
    @Transactional(readOnly = true)
    public PublicShowPage listShows(int page, int size) {
        Map<Long, IdentityServiceClient.PublicTenant> tenants = publicTenantsById();
        if (tenants.isEmpty()) {
            return new PublicShowPage(List.of(), 0, size, 0, 0);
        }
        var result = eventRepository.findByShowcasePublishedTrueAndStatusInAndTenantIdIn(PUBLIC_SHOW_STATUSES, tenants.keySet(),
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50),
                        Sort.by(Sort.Direction.DESC, "eventDate").and(Sort.by(Sort.Direction.DESC, "id"))));
        return new PublicShowPage(toShowResponses(result.getContent(), tenants, false), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    /** Chi tiết một show: kèm toàn bộ gói của đoàn, gói đã dùng cho show này có cờ selected. */
    @Transactional(readOnly = true)
    public PublicShowResponse getShow(Long id) {
        Map<Long, IdentityServiceClient.PublicTenant> tenants = publicTenantsById();
        return toShowResponses(List.of(requirePublic(id, tenants)), tenants, true).get(0);
    }

    /**
     * Show liên quan: show của đoàn khác (và cùng đoàn) đã đăng trên bảng tin, có gói giá trong khoảng ±30% giá
     * gói của show này và cùng tỉnh/thành. Show chưa gắn gói thì chỉ xét vị trí.
     */
    @Transactional(readOnly = true)
    public List<PublicShowResponse> relatedShows(Long id) {
        Map<Long, IdentityServiceClient.PublicTenant> tenants = publicTenantsById();
        Event base = requirePublic(id, tenants);
        String province = tenants.get(base.getTenantId()).province();
        BigDecimal basePrice = base.getPackageId() == null ? null
                : showPackageRepository.findById(base.getPackageId()).map(ShowPackage::getPrice).orElse(null);

        List<Event> candidates = eventRepository.findByShowcasePublishedTrueAndStatusInAndTenantIdIn(PUBLIC_SHOW_STATUSES, tenants.keySet(),
                PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "eventDate").and(Sort.by(Sort.Direction.DESC, "id")))).getContent();
        Map<Long, ShowPackage> packages = showPackageRepository.findAllById(
                candidates.stream().map(Event::getPackageId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(ShowPackage::getId, p -> p));

        List<Event> related = candidates.stream()
                .filter(e -> !e.getId().equals(id))
                .filter(e -> province == null || province.equalsIgnoreCase(String.valueOf(tenants.get(e.getTenantId()).province())))
                .filter(e -> {
                    if (basePrice == null) {
                        return true;
                    }
                    ShowPackage pack = e.getPackageId() == null ? null : packages.get(e.getPackageId());
                    return pack != null && pack.getPrice() != null && withinPriceBand(basePrice, pack.getPrice());
                })
                .limit(RELATED_LIMIT)
                .toList();
        return toShowResponses(related, tenants, false);
    }

    static boolean withinPriceBand(BigDecimal base, BigDecimal other) {
        return other.compareTo(base.multiply(new BigDecimal("0.7"))) >= 0 && other.compareTo(base.multiply(new BigDecimal("1.3"))) <= 0;
    }

    private Event requirePublic(Long id, Map<Long, IdentityServiceClient.PublicTenant> tenants) {
        return eventRepository.findById(id)
                .filter(e -> Boolean.TRUE.equals(e.getShowcasePublished()) && PUBLIC_SHOW_STATUSES.contains(e.getStatus())
                        && tenants.containsKey(e.getTenantId()))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy show này"));
    }

    private List<PublicShowResponse> toShowResponses(List<Event> events, Map<Long, IdentityServiceClient.PublicTenant> tenants,
                                                     boolean withTroupePackages) {
        List<Long> ids = events.stream().map(Event::getId).toList();
        // Video luôn đứng đầu, sau đó đến ảnh theo thứ tự đoàn sắp.
        Map<Long, List<MediaDto>> media = ids.isEmpty() ? Map.of()
                : showMediaRepository.findByEventIdInOrderBySortOrderAscIdAsc(ids).stream()
                        .sorted(Comparator.comparing((ShowMedia m) -> m.getType() != MediaType.VIDEO))
                        .collect(Collectors.groupingBy(ShowMedia::getEventId,
                                Collectors.mapping(m -> new MediaDto(m.getType().name(), m.getUrl()), Collectors.toList())));
        Map<Long, ShowPackage> packages = showPackageRepository.findAllById(
                events.stream().map(Event::getPackageId).filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(ShowPackage::getId, p -> p));
        return events.stream().map(e -> {
            IdentityServiceClient.PublicTenant t = tenants.get(e.getTenantId());
            ShowPackage pack = e.getPackageId() == null ? null : packages.get(e.getPackageId());
            List<PackageOption> options = !withTroupePackages ? List.of()
                    : showPackageRepository.findByTenantId(e.getTenantId()).stream()
                            .filter(p -> p.isActive() || p.getId().equals(e.getPackageId()))
                            .map(p -> new PackageOption(p.getId(), p.getName(), p.getDescription(), p.getPrice(), p.getId().equals(e.getPackageId())))
                            .toList();
            return PublicShowResponse.builder()
                    .id(e.getId())
                    .title(e.getShowcaseTitle() != null ? e.getShowcaseTitle() : e.getName())
                    .description(e.getShowcaseDescription())
                    .type(e.getType() == null ? null : e.getType().getDisplayName())
                    .status(e.getStatus().name())
                    .eventDate(e.getEventDate()).startTime(e.getStartTime())
                    .media(media.getOrDefault(e.getId(), List.of()))
                    .showPackage(pack == null ? null : PublicPackageResponse.builder()
                            .id(pack.getId()).name(pack.getName()).description(pack.getDescription()).price(pack.getPrice())
                            .troupeId(t.id()).troupeName(t.name()).troupeLogo(t.logo()).build())
                    .troupePackages(options)
                    .troupeId(t.id()).troupeName(t.name()).troupeLogo(t.logo()).province(t.province())
                    .build();
        }).toList();
    }

    @Transactional(readOnly = true)
    public PublicTroupeResponse getTroupe(Long troupeId) {
        IdentityServiceClient.PublicTenant tenant = identityServiceClient.findPublicTenants().stream()
                .filter(t -> t.id().equals(troupeId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đoàn này"));
        List<ShowPackage> packages = showPackageRepository.findByTenantId(troupeId).stream()
                .filter(ShowPackage::isActive)
                .toList();
        return toTroupe(tenant, packages, true);
    }

    @Transactional(readOnly = true)
    public List<PublicPackageResponse> listPackages() {
        Map<Long, IdentityServiceClient.PublicTenant> tenants = publicTenantsById();
        // Bỏ gói của đoàn đã ngừng hoạt động — findPublicTenants chỉ trả đoàn đang hoạt động.
        return showPackageRepository.findByActiveTrue().stream()
                .filter(pkg -> tenants.containsKey(pkg.getTenantId()))
                .map(pkg -> toPackage(pkg, tenants.get(pkg.getTenantId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlaceResponse.ProvinceView> listProvinces() {
        return placeCatalog.provinces().stream()
                .map(p -> new PlaceResponse.ProvinceView(p.code(), p.name(), p.fullName()))
                .toList();
    }

    /** Phường/xã của tỉnh, nơi có đơn vị lên trước rồi mới tới theo thứ tự chữ cái. */
    @Transactional(readOnly = true)
    public List<PlaceResponse.WardView> listWards(String provinceName) {
        PlaceCatalog.Province province = placeCatalog.findProvince(provinceName)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tỉnh/thành này"));

        Map<String, Long> troupesPerWard = identityServiceClient.findPublicTenants().stream()
                .filter(t -> sameArea(province.name(), t.province()))
                .filter(t -> t.ward() != null && !t.ward().isBlank())
                .collect(Collectors.groupingBy(t -> PlaceCatalog.normalize(t.ward()), Collectors.counting()));

        Collator collator = Collator.getInstance(Locale.forLanguageTag("vi"));
        return province.wards().stream()
                .map(w -> new PlaceResponse.WardView(
                        w.code(), w.name(), troupesPerWard.getOrDefault(PlaceCatalog.normalize(w.name()), 0L).intValue()))
                .sorted(Comparator.comparingInt(PlaceResponse.WardView::troupeCount).reversed()
                        .thenComparing(PlaceResponse.WardView::name, collator))
                .toList();
    }

    /** Trống nghĩa là không lọc; còn lại so khớp không phân biệt hoa thường và dấu. */
    private boolean sameArea(String wanted, String actual) {
        if (wanted == null || wanted.isBlank()) {
            return true;
        }
        return PlaceCatalog.normalize(wanted).equals(PlaceCatalog.normalize(actual));
    }

    /** Gộp mọi thứ trang chủ cần vào một lần gọi. */
    @Transactional(readOnly = true)
    public HomeAppResponse getHomeApp() {
        return homeCache.get(this::buildHomeApp);
    }

    // Trang chủ là endpoint nặng nhất và mọi lượt mở app đều gọi: gom kết quả trong 20 giây.
    private final TtlCache<HomeAppResponse> homeCache = new TtlCache<>(Duration.ofSeconds(20));

    private HomeAppResponse buildHomeApp() {
        List<IdentityServiceClient.PublicTenant> tenants = identityServiceClient.findPublicTenants();
        Map<Long, List<ShowPackage>> packagesByTenant = activePackagesByTenant();

        Map<String, Long> troupesPerCategory = tenants.stream()
                .filter(t -> t.category() != null)
                .collect(Collectors.groupingBy(IdentityServiceClient.PublicTenant::category, Collectors.counting()));

        List<HomeAppResponse.CategoryBrief> categories = catalogServiceClient.listServiceCategories().stream()
                .map(c -> HomeAppResponse.CategoryBrief.builder()
                        .id(c.id())
                        .code(c.code())
                        .name(c.name())
                        .description(c.description())
                        .troupeCount(troupesPerCategory.getOrDefault(c.code(), 0L).intValue())
                        .build())
                .toList();

        // LinkedHashMap để thứ tự khu vực ổn định giữa các lần gọi.
        Map<String, Integer> troupesPerProvince = new LinkedHashMap<>();
        tenants.stream()
                .map(IdentityServiceClient.PublicTenant::province)
                .filter(p -> p != null && !p.isBlank())
                .sorted()
                .forEach(p -> troupesPerProvince.merge(p, 1, Integer::sum));

        List<HomeAppResponse.ProvinceBrief> discovers = troupesPerProvince.entrySet().stream()
                .map(e -> HomeAppResponse.ProvinceBrief.builder()
                        .province(e.getKey())
                        .troupeCount(e.getValue())
                        .build())
                .toList();

        List<PublicTroupeResponse> featuredTroupes = tenants.stream()
                .map(t -> toTroupe(t, packagesByTenant.getOrDefault(t.id(), List.of()), false))
                .sorted(Comparator.comparingInt(PublicTroupeResponse::packageCount).reversed())
                .limit(FEATURED_LIMIT)
                .toList();

        Map<Long, IdentityServiceClient.PublicTenant> tenantsById = tenants.stream()
                .collect(Collectors.toMap(IdentityServiceClient.PublicTenant::id, t -> t, (a, b) -> a));
        List<PublicPackageResponse> featuredPackages = showPackageRepository.findByActiveTrue().stream()
                .filter(pkg -> tenantsById.containsKey(pkg.getTenantId()))
                .map(pkg -> toPackage(pkg, tenantsById.get(pkg.getTenantId())))
                .limit(FEATURED_LIMIT)
                .toList();

        List<HomeAppResponse.BannerBrief> banners = catalogServiceClient.listBanners().stream()
                .map(b -> HomeAppResponse.BannerBrief.builder()
                        .id(b.id())
                        .title(b.title())
                        .subtitle(b.subtitle())
                        .imageUrl(b.imageUrl())
                        .linkUrl(b.linkUrl())
                        .build())
                .toList();

        return HomeAppResponse.builder()
                .banners(banners)
                .categories(categories)
                .discovers(discovers)
                .featuredTroupes(featuredTroupes)
                .featuredPackages(featuredPackages)
                .build();
    }

    private Map<Long, IdentityServiceClient.PublicTenant> publicTenantsById() {
        return identityServiceClient.findPublicTenants().stream()
                .collect(Collectors.toMap(IdentityServiceClient.PublicTenant::id, t -> t, (a, b) -> a));
    }

    private Map<Long, List<ShowPackage>> activePackagesByTenant() {
        return showPackageRepository.findByActiveTrue().stream()
                .collect(Collectors.groupingBy(ShowPackage::getTenantId));
    }

    private PublicTroupeResponse toTroupe(IdentityServiceClient.PublicTenant tenant,
                                          List<ShowPackage> packages,
                                          boolean includePackages) {
        BigDecimal fromPrice = packages.stream()
                .map(ShowPackage::getPrice)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);
        return PublicTroupeResponse.builder()
                .id(tenant.id())
                .name(tenant.name())
                .domain(tenant.domain())
                .logo(tenant.logo())
                .category(tenant.category())
                .province(tenant.province())
                .ward(tenant.ward())
                .primaryColorHex(tenant.primaryColorHex())
                .accentColorHex(tenant.accentColorHex())
                .packageCount(packages.size())
                .fromPrice(fromPrice)
                .packages(includePackages ? packages.stream().map(pkg -> toPackage(pkg, tenant)).toList() : null)
                .build();
    }

    private PublicPackageResponse toPackage(ShowPackage pkg, IdentityServiceClient.PublicTenant tenant) {
        return PublicPackageResponse.builder()
                .id(pkg.getId())
                .name(pkg.getName())
                .description(pkg.getDescription())
                .price(pkg.getPrice())
                .troupeId(tenant != null ? tenant.id() : pkg.getTenantId())
                .troupeName(tenant != null ? tenant.name() : null)
                .troupeLogo(tenant != null ? tenant.logo() : null)
                .build();
    }
}
