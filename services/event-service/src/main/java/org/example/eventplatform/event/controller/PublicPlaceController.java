package org.example.eventplatform.event.controller;

import lombok.RequiredArgsConstructor;
import org.example.eventplatform.event.dto.PlaceResponse;
import org.example.eventplatform.event.service.PublicCatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Danh mục tỉnh/thành và phường/xã cho khách chọn khu vực — không cần đăng nhập. */
@RestController
@RequestMapping("/api/public/places")
@RequiredArgsConstructor
public class PublicPlaceController {

    private final PublicCatalogService publicCatalogService;

    @GetMapping("/provinces")
    public ResponseEntity<List<PlaceResponse.ProvinceView>> provinces() {
        return ResponseEntity.ok(publicCatalogService.listProvinces());
    }

    /** Phường/xã của một tỉnh, nơi có đơn vị đứng trước để khách thấy ngay chỗ có đoàn. */
    @GetMapping("/wards")
    public ResponseEntity<List<PlaceResponse.WardView>> wards(@RequestParam String province) {
        return ResponseEntity.ok(publicCatalogService.listWards(province));
    }
}
