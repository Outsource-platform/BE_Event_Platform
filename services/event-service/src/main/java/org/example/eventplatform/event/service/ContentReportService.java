package org.example.eventplatform.event.service;

import lombok.RequiredArgsConstructor;
import org.example.eventplatform.event.dto.ReportDtos;
import org.example.eventplatform.event.entity.ChatMessage;
import org.example.eventplatform.event.entity.ContentReport;
import org.example.eventplatform.event.entity.Event;
import org.example.eventplatform.event.entity.ShowRating;
import org.example.eventplatform.event.repository.ChatMessageRepository;
import org.example.eventplatform.event.repository.ContentReportRepository;
import org.example.eventplatform.event.repository.EventRepository;
import org.example.eventplatform.event.repository.ShowRatingRepository;
import org.example.eventplatform.shared.exception.ApiException;
import org.example.eventplatform.shared.time.Clocks;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/** Báo cáo nội dung xấu và xử lý của Super Admin. */
@Service
@RequiredArgsConstructor
public class ContentReportService {

    private static final Set<String> TYPES = Set.of("CHAT_MESSAGE", "RATING", "SHOWCASE");
    private static final Set<String> REASONS = Set.of("SPAM", "OFFENSIVE", "INAPPROPRIATE", "SCAM", "OTHER");
    static final String REMOVED_MESSAGE = "Tin nhắn đã bị gỡ do vi phạm quy định";

    private final ContentReportRepository reportRepository;
    private final ChatMessageRepository messageRepository;
    private final ShowRatingRepository ratingRepository;
    private final EventRepository eventRepository;

    /** Ghi báo cáo; người dùng bấm báo cáo lại cùng nội dung khi báo cáo cũ chưa xử lý thì không tạo thêm. */
    @Transactional
    public ReportDtos.Item create(Long reporterUserId, ReportDtos.CreateRequest request) {
        String type = request.getTargetType().trim().toUpperCase();
        String reason = request.getReason().trim().toUpperCase();
        if (!TYPES.contains(type)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "REPORT_TYPE_INVALID", "Loại nội dung không hợp lệ");
        }
        if (!REASONS.contains(reason)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "REPORT_REASON_INVALID", "Lý do báo cáo không hợp lệ");
        }
        var existing = reportRepository.findFirstByReporterUserIdAndTargetTypeAndTargetIdAndStatus(
                reporterUserId, type, request.getTargetId(), "OPEN");
        if (existing.isPresent()) {
            return toItem(existing.get());
        }
        String note = request.getNote() == null || request.getNote().isBlank() ? null : request.getNote().trim();
        return toItem(reportRepository.save(ContentReport.builder()
                .reporterUserId(reporterUserId).targetType(type).targetId(request.getTargetId())
                .reason(reason).note(note).snapshot(snapshot(type, request.getTargetId())).build()));
    }

    @Transactional(readOnly = true)
    public ReportDtos.Page list(String status, int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        var result = status == null || status.isBlank()
                ? reportRepository.findAllByOrderByCreatedAtDesc(pageable)
                : reportRepository.findByStatusOrderByCreatedAtDesc(status.trim().toUpperCase(), pageable);
        return new ReportDtos.Page(result.getContent().stream().map(ContentReportService::toItem).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    /** Gỡ nội dung bị báo cáo hoặc bỏ qua; mọi báo cáo đang mở về cùng nội dung được đóng theo. */
    @Transactional
    public ReportDtos.Item handle(Long reportId, String status) {
        String next = status.trim().toUpperCase();
        if (!next.equals("REMOVED") && !next.equals("DISMISSED")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "REPORT_STATUS_INVALID", "Cách xử lý không hợp lệ");
        }
        ContentReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "REPORT_NOT_FOUND", "Không tìm thấy báo cáo"));
        if (next.equals("REMOVED")) {
            removeContent(report.getTargetType(), report.getTargetId());
        }
        var now = Clocks.utcNow();
        for (ContentReport r : reportRepository.findByTargetTypeAndTargetIdAndStatus(report.getTargetType(), report.getTargetId(), "OPEN")) {
            r.setStatus(next);
            r.setHandledAt(now);
        }
        report.setStatus(next);
        report.setHandledAt(now);
        return toItem(reportRepository.save(report));
    }

    private void removeContent(String type, Long id) {
        switch (type) {
            case "CHAT_MESSAGE" -> messageRepository.findById(id).ifPresent(m -> {
                m.setContent(REMOVED_MESSAGE);
                m.setImageUrl(null);
                messageRepository.save(m);
            });
            // Giữ số sao để điểm trung bình không đổi, chỉ gỡ lời bình.
            case "RATING" -> ratingRepository.findById(id).ifPresent(r -> {
                r.setComment(null);
                ratingRepository.save(r);
            });
            case "SHOWCASE" -> eventRepository.findById(id).ifPresent(e -> {
                e.setShowcasePublished(false);
                eventRepository.save(e);
            });
            default -> { }
        }
    }

    private String snapshot(String type, Long id) {
        String text = switch (type) {
            case "CHAT_MESSAGE" -> messageRepository.findById(id)
                    .map(m -> (m.getImageUrl() != null ? "[Ảnh] " + m.getImageUrl() + " " : "") + nullToEmpty(m.getContent()))
                    .orElseThrow(ContentReportService::notFound);
            case "RATING" -> ratingRepository.findById(id)
                    .map(r -> r.getStars() + " sao: " + nullToEmpty(r.getComment()))
                    .orElseThrow(ContentReportService::notFound);
            case "SHOWCASE" -> eventRepository.findById(id)
                    .map(e -> nullToEmpty(e.getShowcaseTitle() != null ? e.getShowcaseTitle() : e.getName()))
                    .orElseThrow(ContentReportService::notFound);
            default -> "";
        };
        return text.length() > 500 ? text.substring(0, 497) + "..." : text;
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "REPORT_TARGET_NOT_FOUND", "Không tìm thấy nội dung cần báo cáo");
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static ReportDtos.Item toItem(ContentReport r) {
        return new ReportDtos.Item(r.getId(), r.getReporterUserId(), r.getTargetType(), r.getTargetId(), r.getReason(),
                r.getNote(), r.getSnapshot(), r.getStatus(), r.getCreatedAt(), r.getHandledAt());
    }
}
