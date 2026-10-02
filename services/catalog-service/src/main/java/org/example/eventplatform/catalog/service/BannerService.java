package org.example.eventplatform.catalog.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.eventplatform.catalog.dto.BannerRequest;
import org.example.eventplatform.catalog.dto.BannerResponse;
import org.example.eventplatform.catalog.entity.Banner;
import org.example.eventplatform.catalog.repository.BannerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BannerService {

    private final BannerRepository bannerRepository;

    @Transactional(readOnly = true)
    public List<BannerResponse> getActiveBanners() {
        return bannerRepository.findByActiveTrueOrderBySortOrderAscIdAsc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<BannerResponse> getAllBanners() {
        return bannerRepository.findAllByOrderBySortOrderAscIdAsc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public BannerResponse create(BannerRequest request) {
        Banner banner = Banner.builder()
                .title(request.getTitle().trim())
                .subtitle(blankToNull(request.getSubtitle()))
                .imageUrl(blankToNull(request.getImageUrl()))
                .linkUrl(blankToNull(request.getLinkUrl()))
                .sortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder())
                .active(request.getActive() == null || request.getActive())
                .build();
        return toResponse(bannerRepository.save(banner));
    }

    @Transactional
    public BannerResponse update(Long id, BannerRequest request) {
        Banner banner = getOrThrow(id);
        banner.setTitle(request.getTitle().trim());
        banner.setSubtitle(blankToNull(request.getSubtitle()));
        banner.setImageUrl(blankToNull(request.getImageUrl()));
        banner.setLinkUrl(blankToNull(request.getLinkUrl()));
        if (request.getSortOrder() != null) {
            banner.setSortOrder(request.getSortOrder());
        }
        if (request.getActive() != null) {
            banner.setActive(request.getActive());
        }
        return toResponse(bannerRepository.save(banner));
    }

    @Transactional
    public void delete(Long id) {
        bannerRepository.delete(getOrThrow(id));
    }

    private Banner getOrThrow(Long id) {
        return bannerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy banner"));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private BannerResponse toResponse(Banner banner) {
        return BannerResponse.builder()
                .id(banner.getId())
                .title(banner.getTitle())
                .subtitle(banner.getSubtitle())
                .imageUrl(banner.getImageUrl())
                .linkUrl(banner.getLinkUrl())
                .sortOrder(banner.getSortOrder())
                .active(banner.isActive())
                .build();
    }
}
