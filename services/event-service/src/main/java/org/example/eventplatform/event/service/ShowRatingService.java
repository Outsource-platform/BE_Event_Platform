package org.example.eventplatform.event.service;

import lombok.RequiredArgsConstructor;
import org.example.eventplatform.event.client.IdentityServiceClient;
import org.example.eventplatform.event.dto.RatingDtos;
import org.example.eventplatform.event.entity.ShowRating;
import org.example.eventplatform.event.repository.EventRepository;
import org.example.eventplatform.event.repository.ShowRatingRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ShowRatingService {

    private final ShowRatingRepository ratingRepository;
    private final EventRepository eventRepository;
    private final IdentityServiceClient identityServiceClient;

    @Transactional(readOnly = true)
    public RatingDtos.Page list(Long eventId, int page, int size) {
        requirePublished(eventId);
        var result = ratingRepository.findByEventIdOrderByIdDesc(eventId, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50)));
        return new RatingDtos.Page(summary(eventId),
                result.getContent().stream().map(r -> new RatingDtos.Item(r.getId(), mask(r.getUserName()), r.getStars(), r.getComment(), r.getCreatedAt())).toList(),
                result.getNumber(), result.getTotalPages(), result.getTotalElements());
    }

    /** Khách đánh giá show; đã đánh giá rồi thì cập nhật số sao và nhận xét. */
    @Transactional
    public RatingDtos.Summary rate(Long eventId, Long userId, RatingDtos.Request request) {
        requirePublished(eventId);
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

    private void requirePublished(Long eventId) {
        eventRepository.findById(eventId)
                .filter(e -> Boolean.TRUE.equals(e.getShowcasePublished()))
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy show này"));
    }

    /** "Nguyễn Văn A" -> "Ngu***", như rencity: không để lộ tên đầy đủ của khách. */
    static String mask(String name) {
        if (name == null || name.isBlank()) {
            return "Khách***";
        }
        String n = name.trim();
        return (n.length() >= 3 ? n.substring(0, 3) : n) + "***";
    }
}
