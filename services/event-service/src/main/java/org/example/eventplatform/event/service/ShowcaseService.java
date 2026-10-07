package org.example.eventplatform.event.service;

import lombok.RequiredArgsConstructor;
import org.example.eventplatform.event.dto.MediaDto;
import org.example.eventplatform.event.dto.ShowcaseRequest;
import org.example.eventplatform.event.dto.ShowcaseResponse;
import org.example.eventplatform.event.entity.Event;
import org.example.eventplatform.event.entity.MediaType;
import org.example.eventplatform.event.entity.ShowMedia;
import org.example.eventplatform.event.repository.EventRepository;
import org.example.eventplatform.event.repository.ShowMediaRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/** Đoàn soạn nội dung trưng bày (tiêu đề, mô tả, ảnh, video) cho show của mình để đăng lên bảng tin Khám phá. */
@Service
@RequiredArgsConstructor
public class ShowcaseService {

    private static final int MAX_MEDIA = 10;

    private final EventRepository eventRepository;
    private final ShowMediaRepository mediaRepository;

    @Transactional(readOnly = true)
    public ShowcaseResponse get(Long eventId, Long tenantId) {
        Event event = load(eventId, tenantId);
        return toResponse(event, mediaRepository.findByEventIdOrderBySortOrderAscIdAsc(eventId));
    }

    @Transactional
    public ShowcaseResponse save(Long eventId, Long tenantId, ShowcaseRequest request) {
        Event event = load(eventId, tenantId);
        List<ShowMedia> items = new ArrayList<>();
        int order = 0;
        for (ShowcaseRequest.MediaItem item : request.getMedia() == null ? List.<ShowcaseRequest.MediaItem>of() : request.getMedia()) {
            MediaType type;
            try {
                type = MediaType.valueOf(item.getType().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Loại tệp phải là IMAGE hoặc VIDEO");
            }
            String url = item.getUrl().trim();
            if (!url.startsWith("https://") && !url.startsWith("http://")) {
                throw new IllegalArgumentException("Đường dẫn ảnh hoặc video không hợp lệ");
            }
            items.add(ShowMedia.builder().eventId(eventId).type(type).url(url).sortOrder(order++).build());
        }
        if (items.size() > MAX_MEDIA) {
            throw new IllegalArgumentException("Mỗi show tối đa " + MAX_MEDIA + " ảnh và video");
        }
        if (items.stream().filter(m -> m.getType() == MediaType.VIDEO).count() > 1) {
            throw new IllegalArgumentException("Mỗi show chỉ có một video, các mục còn lại là ảnh");
        }
        // Video luôn đứng đầu rồi mới đến ảnh; sort ổn định nên thứ tự ảnh do đoàn sắp được giữ.
        items.sort(java.util.Comparator.comparing((ShowMedia m) -> m.getType() != MediaType.VIDEO));
        for (int i = 0; i < items.size(); i++) {
            items.get(i).setSortOrder(i);
        }
        if (request.isPublished() && items.isEmpty()) {
            throw new IllegalArgumentException("Thêm ít nhất một ảnh hoặc video trước khi đăng lên Khám phá");
        }
        event.setShowcasePublished(request.isPublished());
        event.setShowcaseTitle(blankToNull(request.getTitle()));
        event.setShowcaseDescription(blankToNull(request.getDescription()));
        eventRepository.save(event);
        mediaRepository.deleteByEventId(eventId);
        mediaRepository.flush();
        return toResponse(event, mediaRepository.saveAll(items));
    }

    private Event load(Long eventId, Long tenantId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy show"));
        if (event.getTenantId() == null || !event.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Show này không thuộc đơn vị của bạn");
        }
        return event;
    }

    private static ShowcaseResponse toResponse(Event event, List<ShowMedia> media) {
        return ShowcaseResponse.builder()
                .eventId(event.getId())
                .published(Boolean.TRUE.equals(event.getShowcasePublished()))
                .title(event.getShowcaseTitle())
                .description(event.getShowcaseDescription())
                .media(media.stream().map(m -> new MediaDto(m.getType().name(), m.getUrl())).toList())
                .build();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
