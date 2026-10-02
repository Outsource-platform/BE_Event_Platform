package org.example.eventplatform.event.service;

import tools.jackson.databind.json.JsonMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Danh mục tỉnh/thành và phường/xã theo địa giới 2 cấp hiện hành (34 tỉnh/thành,
 * hơn 3.300 phường/xã), nạp một lần từ file đóng gói sẵn trong jar để không phải
 * phụ thuộc API bên ngoài lúc chạy.
 */
@Component
public class PlaceCatalog {

    public record Ward(int code, String name) {
    }

    /** {@code name} là tên ngắn dùng lưu trữ ("Hà Nội"), {@code fullName} là tên đầy đủ ("Thành phố Hà Nội"). */
    public record Province(int code, String name, String fullName, List<Ward> wards) {
    }

    private final List<Province> provinces;

    public PlaceCatalog() throws IOException {
        // Dùng mapper riêng: mapper của ứng dụng đặt snake_case toàn cục nên sẽ không
        // đọc được các trường camelCase (fullName) của file dữ liệu.
        try (InputStream in = new ClassPathResource("places/vn-places.json").getInputStream()) {
            this.provinces = List.of(JsonMapper.builder().build().readValue(in, Province[].class));
        }
    }

    public List<Province> provinces() {
        return provinces;
    }

    /** Tìm tỉnh theo tên ngắn hoặc đầy đủ, không phân biệt hoa thường và dấu. */
    public Optional<Province> findProvince(String name) {
        String key = normalize(name);
        if (key.isEmpty()) {
            return Optional.empty();
        }
        return provinces.stream()
                .filter(p -> normalize(p.name()).equals(key) || normalize(p.fullName()).equals(key))
                .findFirst();
    }

    /**
     * Chuẩn hoá để so khớp tên: bỏ dấu, hạ chữ thường, bỏ tiền tố hành chính. Tên do người dùng
     * hoặc dịch vụ bản đồ trả về hay lệch nhau ở dấu/tiền tố ("Thành phố Hà Nội" / "Hà Nội").
     */
    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
        return decomposed.replaceFirst("^(thanh pho|tinh|phuong|xa|dac khu) ", "");
    }
}
