package org.example.eventplatform.event.service;

import org.example.eventplatform.shared.time.Clocks;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.eventplatform.event.client.IdentityServiceClient;
import org.example.eventplatform.event.dto.WithdrawalCreateRequest;
import org.example.eventplatform.event.dto.WithdrawalResponse;
import org.example.eventplatform.event.entity.WithdrawalRequest;
import org.example.eventplatform.event.entity.WithdrawalStatus;
import org.example.eventplatform.event.repository.UserEventRepository;
import org.example.eventplatform.event.repository.WithdrawalRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Withdrawal requests never touch real money — this is purely a request/approve
 * workflow so the tenant admin knows who is waiting to be paid outside the app
 * (see WithdrawalRequest's javadoc for why: Apple IAP review risk).
 */
@Service
@RequiredArgsConstructor
public class WithdrawalService {

    private final WithdrawalRequestRepository withdrawalRequestRepository;
    private final UserEventRepository userEventRepository;
    private final IdentityServiceClient identityServiceClient;
    private final NotificationPublisher notificationPublisher;

    @Transactional(readOnly = true)
    public BigDecimal availableBalance(Long tenantId, Long userId) {
        BigDecimal totalEarned = userEventRepository.sumTotalEarnings(tenantId, userId);
        BigDecimal reservedOrPaid = withdrawalRequestRepository.sumReservedOrPaid(userId);
        return (totalEarned == null ? BigDecimal.ZERO : totalEarned).subtract(reservedOrPaid);
    }

    @Transactional
    public WithdrawalResponse create(Long tenantId, Long userId, WithdrawalCreateRequest request) {
        BigDecimal available = availableBalance(tenantId, userId);
        if (request.getAmount().compareTo(available) > 0) {
            throw new IllegalArgumentException("Số điểm muốn rút vượt quá số dư khả dụng (" + available + ")");
        }

        WithdrawalRequest saved = withdrawalRequestRepository.save(WithdrawalRequest.builder()
                .userId(userId)
                .tenantId(tenantId)
                .amount(request.getAmount())
                .note(request.getNote())
                .build());

        notificationPublisher.publish(
                "WITHDRAWAL_REQUESTED",
                null,
                tenantId,
                "Yêu cầu rút điểm mới",
                memberLabel(userId) + " yêu cầu rút " + request.getAmount() + " điểm",
                Map.of("withdrawalId", String.valueOf(saved.getId()))
        );

        return toResponse(saved, null);
    }

    @Transactional(readOnly = true)
    public List<WithdrawalResponse> listForMember(Long userId) {
        return withdrawalRequestRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(w -> toResponse(w, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WithdrawalResponse> listForTenant(Long tenantId) {
        List<WithdrawalRequest> requests = withdrawalRequestRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        Set<Long> userIds = requests.stream().map(WithdrawalRequest::getUserId).collect(java.util.stream.Collectors.toSet());
        Map<Long, IdentityServiceClient.UserContact> users = identityServiceClient.findUsersByIds(userIds);
        return requests.stream().map(w -> toResponse(w, users.get(w.getUserId()))).toList();
    }

    @Transactional
    public WithdrawalResponse approve(Long tenantId, Long id) {
        WithdrawalRequest w = getOwnedPendingOrThrow(tenantId, id);
        w.setStatus(WithdrawalStatus.APPROVED);
        w.setProcessedAt(Clocks.utcNow());
        withdrawalRequestRepository.save(w);

        notificationPublisher.publish(
                "WITHDRAWAL_APPROVED",
                w.getUserId(),
                tenantId,
                "Yêu cầu rút điểm đã được duyệt",
                "Yêu cầu rút " + w.getAmount() + " điểm của bạn đã được duyệt",
                Map.of("withdrawalId", String.valueOf(w.getId()))
        );
        return toResponse(w, null);
    }

    @Transactional
    public WithdrawalResponse reject(Long tenantId, Long id) {
        WithdrawalRequest w = getOwnedPendingOrThrow(tenantId, id);
        w.setStatus(WithdrawalStatus.REJECTED);
        w.setProcessedAt(Clocks.utcNow());
        withdrawalRequestRepository.save(w);

        notificationPublisher.publish(
                "WITHDRAWAL_REJECTED",
                w.getUserId(),
                tenantId,
                "Yêu cầu rút điểm bị từ chối",
                "Yêu cầu rút " + w.getAmount() + " điểm của bạn đã bị từ chối",
                Map.of("withdrawalId", String.valueOf(w.getId()))
        );
        return toResponse(w, null);
    }

    private WithdrawalRequest getOwnedPendingOrThrow(Long tenantId, Long id) {
        WithdrawalRequest w = withdrawalRequestRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy yêu cầu rút điểm với ID: " + id));
        if (w.getStatus() != WithdrawalStatus.PENDING) {
            throw new IllegalStateException("Yêu cầu này đã được xử lý");
        }
        return w;
    }

    /** Cùng cách gọi tên với thông báo điểm danh: họ tên, không có thì tên đăng nhập. */
    private String memberLabel(Long userId) {
        IdentityServiceClient.UserContact user = identityServiceClient.findUser(userId);
        if (user != null) {
            if (user.fullName() != null && !user.fullName().isBlank()) {
                return user.fullName().trim();
            }
            if (user.username() != null && !user.username().isBlank()) {
                return user.username().trim();
            }
        }
        return "Thành viên";
    }

    private String formatStatus(WithdrawalStatus status) {
        return switch (status) {
            case PENDING -> "Chờ duyệt";
            case APPROVED -> "Đã duyệt";
            case REJECTED -> "Đã từ chối";
        };
    }

    private WithdrawalResponse toResponse(WithdrawalRequest w, IdentityServiceClient.UserContact user) {
        return WithdrawalResponse.builder()
                .id(w.getId())
                .userId(w.getUserId())
                .userFullName(user != null ? user.fullName() : null)
                .bankName(user != null ? user.bankName() : null)
                .bankAccountNumber(user != null ? user.bankAccountNumber() : null)
                .bankAccountHolder(user != null ? user.bankAccountHolder() : null)
                .amount(w.getAmount())
                .status(w.getStatus().name())
                .statusDisplayName(formatStatus(w.getStatus()))
                .note(w.getNote())
                .createdAt(w.getCreatedAt())
                .processedAt(w.getProcessedAt())
                .build();
    }
}
