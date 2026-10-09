package org.example.eventplatform.event.service;

import lombok.RequiredArgsConstructor;
import org.example.eventplatform.event.client.CustomerServiceClient;
import org.example.eventplatform.event.client.IdentityServiceClient;
import org.example.eventplatform.event.dto.RatingDtos;
import org.example.eventplatform.event.entity.Event;
import org.example.eventplatform.event.entity.EventStatus;
import org.example.eventplatform.event.entity.ShowRating;
import org.example.eventplatform.event.repository.EventRepository;
import org.example.eventplatform.event.repository.ShowRatingRepository;
import org.example.eventplatform.shared.exception.ApiException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ShowRatingService {

    /** Show đã hoặc đang diễn: khách đã được phục vụ, không chỉ mới đặt. */
    private static final Set<EventStatus> USED_STATUSES = EnumSet.of(EventStatus.IN_PROGRESS, EventStatus.COMPLETED);

    private final ShowRatingRepository ratingRepository;
    private final EventRepository eventRepository;
    private final IdentityServiceClient identityServiceClient;
    private final CustomerServiceClient customerServiceClient;

    @Transactional(readOnly = true)
    public RatingDtos.Page list(Long eventId, int page, int size) {
        requirePublished(eventId);
        var result = ratingRepository.findByEventIdOrderByIdDesc(eventId, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50)));
        return new RatingDtos.Page(summary(eventId),
                result.getContent().stream().map(r -> new RatingDtos.Item(r.getId(), mask(r.getUserName()), r.getStars(), r.getComment(), r.getCreatedAt())).toList(),
                result.getNumber(), result.getTotalPages(), result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public RatingDtos.Eligibility eligibility(Long eventId, Long userId) {
        Event event = requirePublished(eventId);
        return new RatingDtos.Eligibility(mayRate(event, userId));
    }

    /**
     * Khách đánh giá show; đã đánh giá rồi thì cập nhật số sao và nhận xét.
     * Lần đầu chỉ được gửi khi đã dùng đúng show này và đã dùng show của đơn vị đó.
     */
    @Transactional
    public RatingDtos.Summary rate(Long eventId, Long userId, RatingDtos.Request request) {
        Event event = requirePublished(eventId);
        if (!mayRate(event, userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "RATING_NOT_ALLOWED",
                    "Chỉ khách đã dùng show này và đã từng dùng show của đơn vị mới được đánh giá");
        }
        ShowRating rating = ratingRepository.findByEventIdAndUserId(eventId, userId).orElseGet(() -> {
            var user = identityServiceClient.findUser(userId);
            return ShowRating.builder().eventId(eventId).userId(userId)
                    .userName(user != null ? user.fullName() : null).build();
        });
        rating.setStars(request.getStars());
        String comment = request.getComment() == null ? null : request.getComment().trim();
        rating.setComment(comment == null || comment.isEmpty() ? null : comment);
        ratingRepository.save(rating);
        return summary(eventId);
    }

    @Transactional(readOnly = true)
    public RatingDtos.Summary summary(Long eventId) {
        Map<Integer, Integer> distribution = new LinkedHashMap<>();
        for (int s = 1; s <= 5; s++) {
            distribution.put(s, 0);
        }
        int total = 0;
        int sum = 0;
        for (Object[] row : ratingRepository.countByStars(eventId)) {
            int stars = ((Number) row[0]).intValue();
            int count = ((Number) row[1]).intValue();
            distribution.put(stars, count);
            total += count;
            sum += stars * count;
        }
        double average = total == 0 ? 0 : Math.round(sum * 10.0 / total) / 10.0;
        return new RatingDtos.Summary(average, total, distribution);
    }

    /**
     * Đã có đánh giá thì được sửa. Lần đầu phải là khách của đúng show (đang diễn hoặc đã xong)
     * và đã dùng ít nhất một show của đơn vị đó. Show này cũng tính vào điều kiện đơn vị.
     */
    private boolean mayRate(Event event, Long userId) {
        if (userId == null) {
            return false;
        }
        if (ratingRepository.findByEventIdAndUserId(event.getId(), userId).isPresent()) {
            return true;
        }
        if (event.getTenantId() == null || !USED_STATUSES.contains(event.getStatus())) {
            return false;
        }
        Set<Long> customerIds = customerServiceClient.findByUserId(userId).stream()
                .filter(c -> event.getTenantId().equals(c.tenantId()))
                .map(CustomerServiceClient.CustomerSummary::id)
                .collect(Collectors.toSet());
        if (customerIds.isEmpty() || event.getCustomerId() == null || !customerIds.contains(event.getCustomerId())) {
            return false;
        }
        return eventRepository.existsByTenantIdAndCustomerIdInAndStatusIn(event.getTenantId(), customerIds, USED_STATUSES);
    }

    /** Khách xoá tài khoản: giữ số sao (để điểm trung bình không đổi), bỏ tên và nhận xét có thể nhận ra người viết. */
    @Transactional
    public void anonymizeUser(Long userId) {
        var ratings = ratingRepository.findByUserId(userId);
        ratings.forEach(r -> {
            r.setUserName(null);
            r.setComment(null);
        });
        ratingRepository.saveAll(ratings);
    }

    private Event requirePublished(Long eventId) {
        return eventRepository.findById(eventId)
                .filter(e -> Boolean.TRUE.equals(e.getShowcasePublished()))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy show này"));
    }

    /**
     * Chỉ che tên (từ cuối cùng). Họ và tên đệm giữ nguyên.
     * "Nguyễn Văn Hùng" -> "Nguyễn Văn H***". Một từ thì coi cả từ là tên.
     */
    static String mask(String name) {
        if (name == null || name.isBlank()) {
            return "Khách***";
        }
        String[] parts = name.trim().split("\\s+");
        String given = parts[parts.length - 1];
        String maskedGiven = given.isEmpty() ? "***" : given.substring(0, 1) + "***";
        if (parts.length == 1) {
            return maskedGiven;
        }
        StringBuilder kept = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            if (i > 0) {
                kept.append(' ');
            }
            kept.append(parts[i]);
        }
        return kept.append(' ').append(maskedGiven).toString();
    }
}
