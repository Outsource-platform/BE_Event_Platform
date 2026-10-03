package org.example.eventplatform.catalog.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.eventplatform.catalog.dto.ServiceCategoryRequest;
import org.example.eventplatform.catalog.dto.ServiceCategoryResponse;
import org.example.eventplatform.catalog.service.ServiceCategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/service-categories")
@RequiredArgsConstructor
public class ServiceCategoryController {

    private final ServiceCategoryService serviceCategoryService;

    // Public — tenant admin cần duyệt danh sách này để chọn loại dịch vụ cho vendor profile
    @GetMapping
    public ResponseEntity<List<ServiceCategoryResponse>> getActiveCategories() {
        return ResponseEntity.ok(serviceCategoryService.getActiveCategories());
    }

    /** Gồm cả loại đang ẩn, cho màn quản trị. */
    @GetMapping("/all")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<ServiceCategoryResponse>> getAll() {
        return ResponseEntity.ok(serviceCategoryService.getAllCategories());
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ServiceCategoryResponse> create(@Valid @RequestBody ServiceCategoryRequest request) {
        return new ResponseEntity<>(serviceCategoryService.createCategory(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ServiceCategoryResponse> update(@PathVariable Long id, @Valid @RequestBody ServiceCategoryRequest request) {
        return ResponseEntity.ok(serviceCategoryService.updateCategory(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        serviceCategoryService.deactivateCategory(id);
        return ResponseEntity.noContent().build();
    }
}
