package org.example.eventplatform.event.service;

import lombok.RequiredArgsConstructor;
import org.example.eventplatform.event.client.CatalogServiceClient;
import org.example.eventplatform.event.client.IdentityServiceClient;
import org.example.eventplatform.event.dto.HomeAppResponse;
import org.example.eventplatform.event.dto.PlaceResponse;
import org.example.eventplatform.event.dto.PublicPackageResponse;
import org.example.eventplatform.event.dto.PublicTroupeResponse;
import org.example.eventplatform.event.entity.ShowPackage;
import org.example.eventplatform.event.repository.ShowPackageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
