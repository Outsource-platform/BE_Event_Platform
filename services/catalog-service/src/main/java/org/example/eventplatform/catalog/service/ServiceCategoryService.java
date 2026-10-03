package org.example.eventplatform.catalog.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.eventplatform.catalog.dto.ServiceCategoryRequest;
import org.example.eventplatform.catalog.dto.ServiceCategoryResponse;
import org.example.eventplatform.catalog.entity.ServiceCategory;
import org.example.eventplatform.catalog.repository.ServiceCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceCategoryService {

    private final ServiceCategoryRepository serviceCategoryRepository;

    @Transactional
    public ServiceCategoryResponse createCategory(ServiceCategoryRequest request) {
        if (serviceCategoryRepository.existsByName(request.getName())) {
            throw new RuntimeException("Tên loại dịch vụ đã tồn tại");
        }
        if (serviceCategoryRepository.existsByCode(request.getCode())) {
            throw new RuntimeException("Code đã tồn tại");
        }

        ServiceCategory category = ServiceCategory.builder()
                .name(request.getName())
                .code(request.getCode())
                .description(request.getDescription())
                .active(true)
                .build();

        return toResponse(serviceCategoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public List<ServiceCategoryResponse> getActiveCategories() {
        return serviceCategoryRepository.findByActiveTrue().stream().map(this::toResponse).toList();
    }

    @Transactional
    public ServiceCategoryResponse updateCategory(Long id, ServiceCategoryRequest request) {
        ServiceCategory category = getOrThrow(id);

        if (!category.getName().equals(request.getName()) && serviceCategoryRepository.existsByName(request.getName())) {
            throw new RuntimeException("Tên loại dịch vụ đã tồn tại");
        }
        if (!category.getCode().equals(request.getCode()) && serviceCategoryRepository.existsByCode(request.getCode())) {
            throw new RuntimeException("Code đã tồn tại");
        }

        category.setName(request.getName());
        category.setCode(request.getCode());
        category.setDescription(request.getDescription());
        if (request.getActive() != null) {
            category.setActive(request.getActive());
        }

        return toResponse(serviceCategoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public List<ServiceCategoryResponse> getAllCategories() {
        return serviceCategoryRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public void deactivateCategory(Long id) {
        ServiceCategory category = getOrThrow(id);
        category.setActive(false);
        serviceCategoryRepository.save(category);
    }

    private ServiceCategory getOrThrow(Long id) {
        return serviceCategoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy loại dịch vụ"));
    }

    private ServiceCategoryResponse toResponse(ServiceCategory category) {
        return ServiceCategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .code(category.getCode())
                .description(category.getDescription())
                .active(category.isActive())
                .build();
    }
}
