package org.example.eventplatform.event.service;

import org.example.eventplatform.shared.time.Clocks;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.eventplatform.event.client.CatalogServiceClient;
import org.example.eventplatform.event.client.CustomerServiceClient;
import org.example.eventplatform.event.client.IdentityServiceClient;
import org.example.eventplatform.event.dto.*;
import org.example.eventplatform.event.entity.AssignStatus;
import org.example.eventplatform.event.entity.CrewRole;
import org.example.eventplatform.event.entity.Event;
import org.example.eventplatform.event.entity.EventStatus;
import org.example.eventplatform.event.entity.ShowPackage;
import org.example.eventplatform.event.entity.UserEvent;
import org.example.eventplatform.event.entity.UserEventPayrollItem;
import org.example.eventplatform.event.repository.CrewRoleRepository;
import org.example.eventplatform.event.repository.EventRepository;
import org.example.eventplatform.event.repository.ShowPackageRepository;
import org.example.eventplatform.event.repository.UserEventRepository;
import org.example.eventplatform.event.util.GeoUtils;
import org.example.eventplatform.shared.security.JwtPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService {

    private static final DateTimeFormatter HOUR_MINUTE = DateTimeFormatter.ofPattern("HH:mm");

    private static final int DEFAULT_CHECKIN_RADIUS_METERS = 100;

    private final EventRepository eventRepository;
    private final UserEventRepository userEventRepository;
    private final ShowPackageRepository showPackageRepository;
    private final CrewRoleRepository crewRoleRepository;
    private final NotificationPublisher notificationPublisher;
    private final PayoutService payoutService;
    private final ShowCodeService showCodeService;
    private final IdentityServiceClient identityServiceClient;
    private final CustomerServiceClient customerServiceClient;
    private final CatalogServiceClient catalogServiceClient;

    @Transactional
    public EventResponse createEvent(EventRequest request, JwtPrincipal principal) {
        boolean isTenantAdmin = principal != null && principal.authorities().contains("ROLE_ADMIN");
        boolean isTenantMember = principal != null && principal.authorities().contains("ROLE_TN_MEMBER");
        Long targetTenantId = (isTenantAdmin || isTenantMember) ? principal.tenantId() : request.getTenantId();

        CustomerServiceClient.CustomerSummary customer = customerServiceClient.requireCustomer(request.getCustomerId());
        if (targetTenantId != null && customer.tenantId() != null && !targetTenantId.equals(customer.tenantId())) {
            throw new IllegalArgumentException("Khách hàng không thuộc tenant được gán cho show này");
        }

        ShowPackage selectedPackage = null;
        if (request.getPackageId() != null) {
            if (targetTenantId == null) {
                throw new IllegalArgumentException("Chỉ có thể chọn gói show khi đã xác định tenant thực hiện");
            }
            selectedPackage = showPackageRepository.findByIdAndTenantId(request.getPackageId(), targetTenantId)
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy gói show với ID: " + request.getPackageId()));
        }

        Event event = Event.builder()
                .name(request.getName())
                .type(request.getType())
                .eventDate(request.getEventDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .location(request.getLocation())
                .customerId(request.getCustomerId())
                .totalAmount(request.getTotalAmount())
                .description(request.getDescription())
                .concentrateTime(request.getConcentrateTime())
                .concentrateLocation(request.getConcentrateLocation())
                .concentrateLat(request.getConcentrateLat())
                .concentrateLng(request.getConcentrateLng())
                .packageId(selectedPackage != null ? selectedPackage.getId() : null)
                .packageName(selectedPackage != null ? selectedPackage.getName() : null)
                .depositAmount(request.getDepositAmount())
                .vehicleInfo(request.getVehicleInfo())
                .venueLat(request.getVenueLat())
                .venueLng(request.getVenueLng())
                .checkinRadiusMeters(request.getCheckinRadiusMeters())
                .teamFundPercent(request.getTeamFundPercent())
                .status(EventStatus.SCHEDULED)
                .build();

        if (isTenantAdmin) {
            event.setTenantId(principal.tenantId());
            event.setPlatformFee(BigDecimal.ZERO);
            event.setCreatedBy(principal.username());
            event.setCreatedByUserId(principal.userId());
        } else if (isTenantMember) {
            event.setTenantId(principal.tenantId());
            event.setPlatformFee(BigDecimal.ZERO);
            event.setCreatedBy(principal.username());
            event.setCreatedByUserId(principal.userId());
            event.setStatus(EventStatus.PENDING_APPROVAL);
        } else {
            event.setTenantId(request.getTenantId());
            event.setPlatformFee(calculateDefaultFee(request.getTotalAmount()));
            event.setCreatedBy(principal != null ? principal.username() : "GUEST");
        }

        // Người thầu = người phụ trách khách (trưởng đoàn được chọn người khác). Show khách tự đặt qua sàn thì không có.
        if (isTenantAdmin || isTenantMember) {
            event.setSource("TROUPE");
            Long preferred = isTenantAdmin && request.getContractUserId() != null ? request.getContractUserId() : customer.assignedToUserId();
            applyContract(event, preferred, request.getContractUserId() != null && isTenantAdmin);
        }

        Event saved = saveWithShowCode(event);

        if (saved.getContractUserId() != null && !saved.getContractUserId().equals(principal.userId())) {
            notificationPublisher.publish(
                    "CONTRACT_ASSIGNED",
                    saved.getContractUserId(),
                    null,
                    "Có show mới từ khách của bạn",
                    memberLabel(principal.userId()) + " vừa tạo show \"" + saved.getName() + "\" cho khách bạn phụ trách — hoa hồng tính cho bạn",
                    Map.of("eventId", String.valueOf(saved.getId()))
            );
        }

        if (isTenantMember) {
            notificationPublisher.publish(
                    "EVENT_PENDING_APPROVAL",
                    null,
                    saved.getTenantId(),
                    "Có show mới chờ duyệt",
                    "Thành viên " + principal.username() + " vừa tạo show \"" + saved.getName() + "\" — vào duyệt để kích hoạt",
                    Map.of("eventId", String.valueOf(saved.getId()))
            );
        } else if (!isTenantAdmin && saved.getTenantId() != null) {
            notificationPublisher.publish(
                    "EVENT_PUSHED_TO_TENANT",
                    null,
                    saved.getTenantId(),
                    "Sàn vừa đẩy show mới về đơn vị bạn",
                    "Show \"" + saved.getName() + "\" đã được giao cho đơn vị bạn — vào xem và xác nhận",
                    Map.of("eventId", String.valueOf(saved.getId()))
            );
        }

        return toResponse(saved);
    }

    /**
     * Khách thuê đặt show từ sàn: tìm/tạo CRM theo SĐT, tạo Event PENDING_APPROVAL,
     * rồi đẩy thông báo BOOKING_REQUESTED tới admin đoàn.
     */
    @Transactional
    public EventResponse createCustomerBooking(CustomerBookingRequest request, JwtPrincipal principal) {
        if (principal == null || principal.userId() == null) {
            throw new AccessDeniedException("Cần đăng nhập tài khoản khách để đặt show");
        }

        IdentityServiceClient.UserContact customerUser = identityServiceClient.findUser(principal.userId());
        String phone = customerUser != null && customerUser.username() != null
                ? customerUser.username()
                : principal.username();
        String fullName = customerUser != null && customerUser.fullName() != null && !customerUser.fullName().isBlank()
                ? customerUser.fullName()
                : phone;
        String email = customerUser != null ? customerUser.email() : null;

        CustomerServiceClient.CustomerSummary customer = customerServiceClient.findOrCreate(
                request.getTenantId(), phone, principal.userId(), fullName, email);

        ShowPackage selectedPackage = showPackageRepository
                .findByIdAndTenantId(request.getPackageId(), request.getTenantId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy gói show với ID: " + request.getPackageId()));

        String eventName = "Đặt show · " + selectedPackage.getName();
        Event event = Event.builder()
                .name(eventName)
                .eventDate(request.getEventDate())
                .startTime(request.getStartTime())
                .location(request.getLocation())
                .customerId(customer.id())
                .tenantId(request.getTenantId())
                .packageId(selectedPackage.getId())
                .packageName(selectedPackage.getName())
                .totalAmount(selectedPackage.getPrice())
                .platformFee(calculateDefaultFee(selectedPackage.getPrice()))
                .description(request.getNote())
                .venueLat(request.getLat())
                .venueLng(request.getLng())
                .status(EventStatus.PENDING_APPROVAL)
                .createdByUserId(principal.userId())
                .source("MARKETPLACE")
                .build();
        event.setCreatedBy(principal.username());

        Event saved = saveWithShowCode(event);

        notificationPublisher.publish(
                "BOOKING_REQUESTED",
                null,
                saved.getTenantId(),
                "Có yêu cầu đặt show mới",
                "Khách " + fullName + " vừa gửi đơn đặt \"" + selectedPackage.getName() + "\" — vào duyệt để xác nhận",
                Map.of("eventId", String.valueOf(saved.getId()))
        );

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<EventResponse> listCustomerBookings(Long userId) {
        List<CustomerServiceClient.CustomerSummary> customers = customerServiceClient.findByUserId(userId);
        if (customers.isEmpty()) {
            return List.of();
        }
        List<Long> customerIds = customers.stream().map(CustomerServiceClient.CustomerSummary::id).toList();
        List<Event> events = eventRepository.findByCustomerIdInOrderByCreatedAtDesc(customerIds);
        Map<Long, String> names = customerNames(events);
        Map<Long, String> users = userNames(events);
        // Khách có thể đặt ở nhiều đơn vị: mỗi đơn vị chỉ tra thông tin một lần.
        Map<Long, TenantVendorContext> contexts = new java.util.HashMap<>();
        return events.stream()
                .map(e -> toResponse(e, contexts.computeIfAbsent(e.getTenantId(),
                        id -> TenantVendorContext.fetch(id, identityServiceClient, catalogServiceClient)), names, users))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EventResponse> listTenantEventsForCustomer(Long tenantId, Long customerId) {
        List<Event> events = eventRepository.findByTenantIdAndCustomerIdOrderByEventDateDesc(tenantId, customerId);
        TenantVendorContext ctx = TenantVendorContext.fetch(tenantId, identityServiceClient, catalogServiceClient);
        Map<Long, String> names = customerNames(events);
        Map<Long, String> users = userNames(events);
        return events.stream().map(event -> toResponse(event, ctx, names, users)).toList();
    }

    private Event saveWithShowCode(Event event) {
        showCodeService.assign(event);
        return eventRepository.save(event);
    }

    /**
     * Gán người thầu và chốt % hoa hồng của họ ngay lúc này. [strict] = trưởng đoàn chủ động chọn: người đó phải thuộc đoàn,
     * còn người phụ trách khách tự điền thì bỏ qua nếu đã rời đoàn.
     */
    private void applyContract(Event event, Long userId, boolean strict) {
        event.setContractUserId(null);
        event.setContractCommissionRate(null);
        event.setCreatorCommissionAmount(null);
        if (userId == null) {
            return;
        }
        IdentityServiceClient.UserContact user = identityServiceClient.findUser(userId);
        boolean valid = user != null && event.getTenantId() != null && event.getTenantId().equals(user.tenantId());
        if (!valid) {
            if (strict) {
                throw new IllegalArgumentException("Người thầu phải là thành viên của đoàn");
            }
            return;
        }
        BigDecimal rate = user.commissionRate();
        event.setContractUserId(userId);
        event.setContractCommissionRate(rate);
        if (rate != null && event.getTotalAmount() != null) {
            event.setCreatorCommissionAmount(
                    event.getTotalAmount().multiply(rate).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP));
        }
    }

    /** Trưởng đoàn đổi người thầu của show (chốt lại % theo người mới) cho tới khi show hoàn thành. */
    @Transactional
    public EventResponse updateContract(Long eventId, Long tenantId, Long contractUserId) {
        Event event = getEventOrThrow(eventId);
        if (event.getTenantId() == null || !event.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Show này không thuộc đơn vị của bạn");
        }
        if ("MARKETPLACE".equals(event.getSource())) {
            throw new IllegalArgumentException("Show khách đặt qua sàn: hoa hồng về sàn, không có người thầu");
        }
        if (event.getStatus() == EventStatus.COMPLETED) {
            throw new IllegalArgumentException("Show đã hoàn thành, không đổi người thầu được nữa");
        }
        applyContract(event, contractUserId, true);
        return toResponse(eventRepository.save(event));
    }

    private BigDecimal calculateDefaultFee(BigDecimal totalAmount) {
        return totalAmount != null ? totalAmount.multiply(new BigDecimal("0.1")) : BigDecimal.ZERO;
    }

    private BigDecimal computeDepositPercent(BigDecimal depositAmount, BigDecimal totalAmount) {
        if (depositAmount == null || totalAmount == null || totalAmount.signum() == 0) {
            return null;
        }
        return depositAmount.divide(totalAmount, 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    private BigDecimal computeTeamFundAmount(BigDecimal teamFundPercent, BigDecimal totalAmount) {
        if (teamFundPercent == null || totalAmount == null) {
            return null;
        }
        return totalAmount.multiply(teamFundPercent).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public EventResponse getEventDetail(Long id) {
        return toResponse(getEventOrThrow(id));
    }

    @Transactional(readOnly = true)
    public EventWithMembersResponse getEventDetailWithMembers(Long id) {
        Event event = getEventOrThrow(id);
        List<UserEvent> assignments = userEventRepository.findByEventId(id);
        Map<Long, IdentityServiceClient.UserContact> users = identityServiceClient.findUsersByIds(
                assignments.stream().map(UserEvent::getUserId).collect(Collectors.toSet())
        );
        Map<Long, CrewRole> crewRoles = fetchCrewRoles(assignments);
        List<AssignmentResponse> members = assignments.stream()
                .map(ue -> toAssignmentResponse(ue, users, crewRoles))
                .toList();
        return EventWithMembersResponse.builder().eventInfo(toResponse(event)).members(members).build();
    }

    private Map<Long, CrewRole> fetchCrewRoles(List<UserEvent> assignments) {
        Set<Long> crewRoleIds = assignments.stream()
                .flatMap(ue -> java.util.stream.Stream.concat(
                        java.util.stream.Stream.of(ue.getCrewRoleId()),
                        ue.getEvent().getAssignedMembers().stream().map(UserEvent::getCrewRoleId)))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (crewRoleIds.isEmpty()) {
            return Map.of();
        }
        return crewRoleRepository.findAllById(crewRoleIds).stream()
                .collect(Collectors.toMap(CrewRole::getId, r -> r));
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> getTenantEvents(Long tenantId, Pageable pageable) {
        TenantVendorContext ctx = TenantVendorContext.fetch(tenantId, identityServiceClient, catalogServiceClient);
        Page<Event> page = eventRepository.findByTenantId(tenantId, pageable);
        Map<Long, String> names = customerNames(page.getContent());
        Map<Long, String> users = userNames(page.getContent());
        return page.map(e -> toResponse(e, ctx, names, users));
    }

    @Transactional(readOnly = true)
    public Page<EventResponse> getTenantSchedule(Long tenantId, int month, int year, Pageable pageable) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        TenantVendorContext ctx = TenantVendorContext.fetch(tenantId, identityServiceClient, catalogServiceClient);
        Page<Event> page = eventRepository.findByTenantIdAndEventDateBetween(tenantId, start, end, pageable);
        Map<Long, String> names = customerNames(page.getContent());
        Map<Long, String> users = userNames(page.getContent());
        return page.map(e -> toResponse(e, ctx, names, users));
    }

    @Transactional(readOnly = true)
    public MonthlySummaryResponse getTenantMonthlySummary(Long tenantId, int month, int year) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        List<Event> events = eventRepository.findByTenantIdAndEventDateBetween(tenantId, start, end);

        BigDecimal revenue = events.stream()
                .filter(e -> e.getStatus() != EventStatus.CANCELLED)
                .map(Event::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long completed = events.stream().filter(e -> e.getStatus() == EventStatus.COMPLETED).count();
        double rate = events.isEmpty() ? 0 : (double) completed / events.size() * 100;

        return MonthlySummaryResponse.builder()
                .totalEvents(events.size())
                .estimatedRevenue(revenue)
                .completionRate(rate)
                .build();
    }

    @Transactional
    public void acceptEvent(Long eventId, Long tenantId) {
        Event event = getEventOrThrow(eventId);
        assertOwnership(event, tenantId);
        event.setStatus(EventStatus.CONFIRMED);
        eventRepository.save(event);
    }

    @Transactional
    public void rejectEvent(Long eventId, Long tenantId) {
        Event event = getEventOrThrow(eventId);
        assertOwnership(event, tenantId);
        event.setStatus(EventStatus.CANCELLED);
        eventRepository.save(event);
    }

    @Transactional
    public void assignMembers(Long eventId, Long tenantId, List<AssignMemberRequest> requests) {
        Event event = getEventOrThrow(eventId);
        assertOwnership(event, tenantId);

        Set<Long> userIds = requests.stream().map(AssignMemberRequest::getUserId).collect(Collectors.toSet());
        Map<Long, IdentityServiceClient.UserContact> users = identityServiceClient.findUsersByIds(userIds);
        for (AssignMemberRequest req : requests) {
            IdentityServiceClient.UserContact user = users.get(req.getUserId());
            if (user == null) {
                throw new IllegalArgumentException("Không tìm thấy thành viên với ID: " + req.getUserId());
            }
            if (user.tenantId() == null || !user.tenantId().equals(tenantId)) {
                throw new IllegalArgumentException("Thành viên " + req.getUserId() + " không thuộc tenant của show");
            }
        }

        Set<Long> crewRoleIds = requests.stream()
                .map(AssignMemberRequest::getCrewRoleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, CrewRole> crewRoles = crewRoleIds.isEmpty() ? Map.of() : crewRoleRepository.findAllById(crewRoleIds).stream()
                .collect(Collectors.toMap(CrewRole::getId, r -> r));
        for (Long crewRoleId : crewRoleIds) {
            CrewRole role = crewRoles.get(crewRoleId);
            if (role == null || !role.getTenantId().equals(tenantId)) {
                throw new IllegalArgumentException("Không tìm thấy vị trí biểu diễn với ID: " + crewRoleId);
            }
        }

        for (AssignMemberRequest req : requests) {
            CrewRole role = req.getCrewRoleId() != null ? crewRoles.get(req.getCrewRoleId()) : null;
            String position = (req.getPosition() != null && !req.getPosition().isBlank())
                    ? req.getPosition()
                    : (role != null ? role.getName() : req.getPosition());

            UserEvent userEvent = userEventRepository.findByEventIdAndUserId(eventId, req.getUserId())
                    .orElse(new UserEvent());
            userEvent.setEvent(event);
            userEvent.setUserId(req.getUserId());
            userEvent.setPosition(position);
            boolean fresh = userEvent.getId() == null;
            userEvent.setCrewRoleId(req.getCrewRoleId());
            userEvent.setStatus(AssignStatus.PENDING);
            // Gán vào vị trí có mức cát-xê thì tự có mức đó; sau này trưởng đoàn chỉ chỉnh xuống, không vượt mức trần.
            if (role != null && role.getCastFee() != null && role.getCastFee().signum() > 0
                    && (fresh || userEvent.getSalary() == null || userEvent.getSalary().signum() == 0)) {
                userEvent.getPayrollItems().clear();
                userEvent.getPayrollItems().add(UserEventPayrollItem.builder()
                        .userEvent(userEvent).label("Cát-xê vị trí " + role.getName()).amount(role.getCastFee()).build());
                userEvent.setSalary(role.getCastFee());
            }
            userEventRepository.save(userEvent);

            notificationPublisher.publish(
                    "MEMBER_ASSIGNED",
                    req.getUserId(),
                    tenantId,
                    "Bạn được gán vào show mới",
                    "Show \"" + event.getName() + "\" — vị trí " + position,
                    Map.of("eventId", String.valueOf(eventId), "position", String.valueOf(position))
            );
        }

        if (event.getStatus() == EventStatus.SCHEDULED) {
            event.setStatus(EventStatus.CONFIRMED);
            eventRepository.save(event);
        }
    }

    @Transactional
    public void respondToAssignment(Long userEventId, JwtPrincipal principal, AssignStatus status, String note) {
        UserEvent ue = getAssignmentOrThrow(userEventId);
        assertSelfOrAdmin(ue, principal);

        ue.setStatus(status);
        ue.setNote(note);
        ue.setRespondedAt(Clocks.utcNow());
        userEventRepository.save(ue);

        String type = status == AssignStatus.REJECTED ? "MEMBER_REJECTED" : "MEMBER_ACCEPTED";
        notificationPublisher.publish(
                type,
                null,
                ue.getEvent().getTenantId(),
                status == AssignStatus.REJECTED ? "Thành viên đã từ chối show" : "Thành viên đã nhận show",
                memberLabel(ue.getUserId()) + " đã phản hồi show \"" + ue.getEvent().getName() + "\"",
                Map.of("userEventId", String.valueOf(userEventId), "note", note == null ? "" : note)
        );
    }

    @Transactional
    public String concentrateCheckIn(Long userEventId, JwtPrincipal principal) {
        UserEvent ue = getAssignmentOrThrow(userEventId);
        assertSelfOrAdmin(ue, principal);

        // Lưu UTC; so với giờ quy định và viết vào câu chữ thì dùng giờ Việt Nam (giờ quy định là giờ tường do trưởng đoàn nhập).
        ue.setActualConcentrateAt(Clocks.utcNowTime());
        LocalTime now = Clocks.vnNowTime();
        ue.setStatus(AssignStatus.CHECKIN_CONCENTRATE);
        userEventRepository.save(ue);

        LocalTime scheduledTime = ue.getEvent().getConcentrateTime();
        String punctuality;
        String result;
        if (scheduledTime == null) {
            punctuality = "không có giờ quy định";
            result = "Xác nhận có mặt thành công lúc " + now.format(HOUR_MINUTE);
        } else if (!now.isAfter(scheduledTime)) {
            punctuality = "đúng giờ";
            result = "Xác nhận: có mặt ĐÚNG GIỜ (" + now.format(HOUR_MINUTE) + ").";
        } else {
            long minutesLate = java.time.Duration.between(scheduledTime, now).toMinutes();
            punctuality = "trễ " + minutesLate + " phút";
            result = "Xác nhận: có mặt MUỘN " + minutesLate + " phút (giờ quy định " + scheduledTime.format(HOUR_MINUTE) + ").";
        }

        notificationPublisher.publish(
                "MEMBER_CONCENTRATE_CHECKIN",
                null,
                ue.getEvent().getTenantId(),
                "Thành viên đã tập trung",
                memberLabel(ue.getUserId()) + " đã check-in tập trung lúc " + now.format(HOUR_MINUTE)
                        + " cho show \"" + ue.getEvent().getName() + "\" — " + punctuality + ".",
                Map.of("userEventId", String.valueOf(userEventId), "eventId", String.valueOf(ue.getEvent().getId()), "punctuality", punctuality)
        );

        return result;
    }

    @Transactional
    public void checkIn(Long userEventId, JwtPrincipal principal, String location, Double lat, Double lng) {
        UserEvent ue = getAssignmentOrThrow(userEventId);
        assertSelfOrAdmin(ue, principal);

        if (ue.getStatus() != AssignStatus.ACCEPTED && ue.getStatus() != AssignStatus.CHECKIN_CONCENTRATE) {
            throw new IllegalStateException("Bạn phải xác nhận tham gia hoặc check-in tập trung trước khi check-in điểm diễn");
        }

        Event event = ue.getEvent();
        if (lat != null && lng != null && event.getVenueLat() != null && event.getVenueLng() != null) {
            int radius = event.getCheckinRadiusMeters() != null ? event.getCheckinRadiusMeters() : DEFAULT_CHECKIN_RADIUS_METERS;
            double distance = GeoUtils.distanceMeters(lat, lng, event.getVenueLat(), event.getVenueLng());
            if (distance > radius) {
                throw new IllegalArgumentException(String.format(
                        "Bạn đang cách sân khấu %.0fm, ngoài phạm vi cho phép (%dm)", distance, radius));
            }
        }

        ue.setCheckinAt(Clocks.utcNowTime());
        ue.setCheckinLocation(location);
        ue.setCheckinLat(lat);
        ue.setCheckinLng(lng);
        ue.setStatus(AssignStatus.CHECKED_IN);
        userEventRepository.save(ue);

        if (event.getStatus() == EventStatus.CONFIRMED) {
            event.setStatus(EventStatus.IN_PROGRESS);
            eventRepository.save(event);
        }

        notificationPublisher.publish(
                "MEMBER_CHECKED_IN",
                null,
                event.getTenantId(),
                "Thành viên đã check-in",
                memberLabel(ue.getUserId()) + " đã check-in tại show \"" + event.getName() + "\"",
                Map.of("userEventId", String.valueOf(userEventId), "eventId", String.valueOf(event.getId()))
        );
    }

    @Transactional
    public String checkOut(Long userEventId, JwtPrincipal principal) {
        UserEvent ue = getAssignmentOrThrow(userEventId);
        assertSelfOrAdmin(ue, principal);

        if (ue.getStatus() != AssignStatus.CHECKED_IN) {
            throw new IllegalStateException("Bạn chưa check-in điểm diễn");
        }

        ue.setCheckoutAt(Clocks.utcNowTime());
        ue.setStatus(AssignStatus.COMPLETED);
        payoutService.settleAssignment(ue);
        userEventRepository.save(ue);

        autoCompleteEventIfFinished(ue.getEvent().getId());

        notificationPublisher.publish(
                "MEMBER_CHECKED_OUT",
                null,
                ue.getEvent().getTenantId(),
                "Thành viên đã check-out",
                memberLabel(ue.getUserId()) + " đã hoàn thành show \"" + ue.getEvent().getName() + "\"",
                Map.of("userEventId", String.valueOf(userEventId), "eventId", String.valueOf(ue.getEvent().getId()))
        );

        return "Đã hoàn thành show diễn!";
    }

    @Transactional
    public AssignmentResponse setPayrollItems(Long userEventId, Long tenantId, List<PayrollItemRequest> items) {
        UserEvent ue = getAssignmentOrThrow(userEventId);
        if (ue.getEvent().getTenantId() == null || !ue.getEvent().getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Show này không thuộc đơn vị của bạn");
        }

        ue.getPayrollItems().clear();
        BigDecimal total = BigDecimal.ZERO;
        for (PayrollItemRequest item : items) {
            ue.getPayrollItems().add(UserEventPayrollItem.builder()
                    .userEvent(ue)
                    .label(item.getLabel())
                    .amount(item.getAmount())
                    .build());
            total = total.add(item.getAmount());
        }
        // Vị trí có mức trần cát-xê thì tiền công không được vượt mức đó.
        CrewRole role = ue.getCrewRoleId() == null ? null : crewRoleRepository.findById(ue.getCrewRoleId()).orElse(null);
        if (role != null && role.getCastFee() != null && role.getCastFee().signum() > 0 && total.compareTo(role.getCastFee()) > 0) {
            throw new IllegalArgumentException("Tiền công vượt mức trần của vị trí \"" + role.getName() + "\" (" + role.getCastFee().toPlainString() + ")");
        }
        ue.setSalary(total);
        // Show đã hoàn thành mà sửa tiền công thì chốt lại phần trừ quỹ theo số mới.
        if (ue.getStatus() == AssignStatus.COMPLETED) {
            payoutService.settleAssignment(ue);
        }
        UserEvent saved = userEventRepository.save(ue);

        Map<Long, IdentityServiceClient.UserContact> users = identityServiceClient.findUsersByIds(Set.of(saved.getUserId()));
        Map<Long, CrewRole> crewRoles = saved.getCrewRoleId() == null ? Map.of()
                : crewRoleRepository.findById(saved.getCrewRoleId())
                        .map(r -> Map.of(r.getId(), r))
                        .orElse(Map.of());
        return toAssignmentResponse(saved, users, crewRoles);
    }

    private void autoCompleteEventIfFinished(Long eventId) {
        List<UserEvent> members = userEventRepository.findByEventId(eventId);
        boolean finished = members.stream()
                .filter(m -> m.getStatus() != AssignStatus.REJECTED)
                .allMatch(m -> m.getStatus() == AssignStatus.COMPLETED);

        if (finished) {
            Event event = getEventOrThrow(eventId);
            event.setStatus(EventStatus.COMPLETED);
            payoutService.settleCommission(event);
            eventRepository.save(event);

            notificationPublisher.publish(
                    "EVENT_AUTO_COMPLETED",
                    null,
                    event.getTenantId(),
                    "Show đã hoàn thành",
                    "Show \"" + event.getName() + "\" đã hoàn thành!",
                    Map.of("eventId", String.valueOf(eventId))
            );
        }
    }

    @Transactional
    public EventResponse updateConcentrateInfo(Long eventId, Long tenantId, UpdateConcentrateRequest request) {
        Event event = getEventOrThrow(eventId);
        assertOwnership(event, tenantId);

        event.setConcentrateTime(request.getConcentrateTime());
        event.setConcentrateLocation(request.getConcentrateLocation());
        Event saved = eventRepository.save(event);

        userEventRepository.findByEventId(eventId).stream()
                .filter(m -> m.getStatus() == AssignStatus.ACCEPTED || m.getStatus() == AssignStatus.PENDING)
                .forEach(m -> notificationPublisher.publish(
                        "CONCENTRATE_INFO_UPDATED",
                        m.getUserId(),
                        tenantId,
                        "Cập nhật giờ tập trung",
                        "Show \"" + saved.getName() + "\" — tập trung lúc " + request.getConcentrateTime()
                                + " tại " + request.getConcentrateLocation(),
                        Map.of("eventId", String.valueOf(eventId))
                ));

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> getMyAssignedEvents(Long userId) {
        List<UserEvent> mine = userEventRepository.findByUserId(userId);
        Set<Long> allUserIds = new HashSet<>();
        for (UserEvent ue : mine) {
            allUserIds.add(ue.getUserId());
            ue.getEvent().getAssignedMembers().forEach(m -> allUserIds.add(m.getUserId()));
        }
        Map<Long, IdentityServiceClient.UserContact> users = identityServiceClient.findUsersByIds(allUserIds);
        Map<Long, CrewRole> crewRoles = fetchCrewRoles(mine);
        return mine.stream().map(ue -> toAssignmentResponse(ue, users, crewRoles)).toList();
    }

    @Transactional(readOnly = true)
    public DashboardStatResponse getMemberDashboardStats(Long tenantId, Long userId) {
        long finished = userEventRepository.countFinishedShows(tenantId, userId);
        long pending = userEventRepository.countPendingShows(tenantId, userId);
        BigDecimal earnings = userEventRepository.sumTotalEarnings(tenantId, userId);
        return new DashboardStatResponse(finished, pending, earnings != null ? earnings : BigDecimal.ZERO, rank(finished));
    }

    private String rank(long finished) {
        if (finished > 50) return "Kim Cương";
        if (finished > 20) return "Bạch Kim";
        return "NEWBIE";
    }

    private Event getEventOrThrow(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sự kiện với ID: " + id));
    }

    private UserEvent getAssignmentOrThrow(Long id) {
        return userEventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy bản ghi phân công"));
    }

    private void assertOwnership(Event event, Long tenantId) {
        if (event.getTenantId() == null || !event.getTenantId().equals(tenantId)) {
            throw new AccessDeniedException("Show này không thuộc đơn vị của bạn");
        }
    }

    private void assertSelfOrAdmin(UserEvent ue, JwtPrincipal principal) {
        boolean isAdmin = principal.authorities().contains("ROLE_ADMIN") || principal.authorities().contains("ROLE_SUPER_ADMIN");
        if (!isAdmin && !ue.getUserId().equals(principal.userId())) {
            throw new AccessDeniedException("Bạn không có quyền thao tác trên bản ghi phân công này");
        }
    }

    /**
     * Single-item reads fetch tenant/vendor themselves. List views (getTenantEvents,
     * getTenantSchedule) share one tenantId across every row, so they fetch the
     * tenant/vendor once via {@link #toResponse(Event, TenantVendorContext)} instead
     * of repeating the same two HTTP calls per row.
     */
    private EventResponse toResponse(Event event) {
        TenantVendorContext ctx = TenantVendorContext.fetch(event.getTenantId(), identityServiceClient, catalogServiceClient);
        return toResponse(event, ctx);
    }

    private EventResponse toResponse(Event event, TenantVendorContext ctx) {
        return toResponse(event, ctx, null);
    }

    /** Tên khách của các show trong một danh sách, tra một lần thay vì mỗi show một lần gọi HTTP. */
    private Map<Long, String> customerNames(java.util.Collection<Event> events) {
        java.util.Set<Long> ids = events.stream().map(Event::getCustomerId).filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        return customerServiceClient.findNamesByIds(ids);
    }

    /** {@code customerNames} null nghĩa là chưa tra trước: tự tra từng khách (dùng cho một show đơn lẻ). */
    private EventResponse toResponse(Event event, TenantVendorContext ctx, Map<Long, String> customerNames) {
        return toResponse(event, ctx, customerNames, null);
    }

    /** Tên người trong danh sách show: tra một lần theo lô; null nghĩa là tra riêng từng người (một show đơn lẻ). */
    private Map<Long, String> userNames(java.util.Collection<Event> events) {
        java.util.Set<Long> ids = new java.util.HashSet<>();
        for (Event e : events) {
            if (e.getCreatedByUserId() != null && !"MARKETPLACE".equals(e.getSource())) ids.add(e.getCreatedByUserId());
            if (e.getContractUserId() != null) ids.add(e.getContractUserId());
        }
        Map<Long, String> out = new java.util.HashMap<>();
        identityServiceClient.findUsersByIds(ids).forEach((id, u) -> out.put(id, displayName(u)));
        return out;
    }

    private String userName(Long id, Map<Long, String> userNames) {
        if (userNames != null) {
            return userNames.get(id);
        }
        IdentityServiceClient.UserContact u = identityServiceClient.findUser(id);
        return u == null ? null : displayName(u);
    }

    private static String displayName(IdentityServiceClient.UserContact u) {
        return u.fullName() != null && !u.fullName().isBlank() ? u.fullName() : u.username();
    }

    private EventResponse toResponse(Event event, TenantVendorContext ctx, Map<Long, String> customerNames, Map<Long, String> userNames) {
        EventResponse response = EventResponse.builder()
                .id(event.getId())
                .showCode(event.getShowCode())
                .name(event.getName())
                .type(event.getType())
                .typeDisplayName(event.getType() != null ? event.getType().getDisplayName() : null)
                .status(event.getStatus())
                .statusDisplayName(formatEventStatus(event.getStatus()))
                .eventDate(event.getEventDate())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .location(event.getLocation())
                .customerId(event.getCustomerId())
                .tenantId(event.getTenantId())
                .concentrateTime(event.getConcentrateTime())
                .concentrateLocation(event.getConcentrateLocation())
                .concentrateLat(event.getConcentrateLat())
                .concentrateLng(event.getConcentrateLng())
                .totalAmount(event.getTotalAmount())
                .platformFee(event.getPlatformFee())
                .packageId(event.getPackageId())
                .packageName(event.getPackageName())
                .depositAmount(event.getDepositAmount())
                .depositPercent(computeDepositPercent(event.getDepositAmount(), event.getTotalAmount()))
                .vehicleInfo(event.getVehicleInfo())
                .description(event.getDescription())
                .venueLat(event.getVenueLat())
                .venueLng(event.getVenueLng())
                .checkinRadiusMeters(event.getCheckinRadiusMeters())
                .teamFundPercent(event.getTeamFundPercent())
                .teamFundAmount(computeTeamFundAmount(event.getTeamFundPercent(), event.getTotalAmount()))
                .createdByUserId(event.getCreatedByUserId())
                .source(event.getSource())
                .contractUserId(event.getContractUserId())
                .contractCommissionRate(event.getContractCommissionRate())
                .creatorCommissionAmount(event.getCreatorCommissionAmount())
                .createdAt(event.getCreatedAt())
                .build();

        if (response.getCustomerId() != null) {
            if (customerNames != null) {
                response.setCustomerName(customerNames.get(response.getCustomerId()));
            } else {
                CustomerServiceClient.CustomerSummary customer = customerServiceClient.findCustomer(response.getCustomerId());
                if (customer != null) {
                    response.setCustomerName(customer.fullName());
                }
            }
        }
        if (!"MARKETPLACE".equals(event.getSource()) && event.getCreatedByUserId() != null) {
            response.setCreatedByName(userName(event.getCreatedByUserId(), userNames));
        }
        if (event.getContractUserId() != null) {
            response.setContractUserName(userName(event.getContractUserId(), userNames));
        }
        if (ctx.tenant() != null) {
            response.setTenantName(ctx.tenant().name());
        }
        if (ctx.vendor() != null) {
            response.setVendorBusinessName(ctx.vendor().businessName());
            response.setServiceCategoryName(ctx.vendor().serviceCategoryName());
        }
        return response;
    }

    private record TenantVendorContext(IdentityServiceClient.TenantSummary tenant, CatalogServiceClient.VendorProfileSummary vendor) {
        static TenantVendorContext fetch(Long tenantId, IdentityServiceClient identityServiceClient, CatalogServiceClient catalogServiceClient) {
            if (tenantId == null) {
                return new TenantVendorContext(null, null);
            }
            return new TenantVendorContext(identityServiceClient.findTenant(tenantId), catalogServiceClient.findVendorByTenant(tenantId));
        }
    }

    private AssignmentResponse toAssignmentResponse(UserEvent ue, Map<Long, IdentityServiceClient.UserContact> users,
                                                      Map<Long, CrewRole> crewRoles) {
        Event event = ue.getEvent();
        IdentityServiceClient.UserContact self = users.get(ue.getUserId());
        CrewRole ownRole = ue.getCrewRoleId() != null ? crewRoles.get(ue.getCrewRoleId()) : null;
        List<AssignmentResponse.Teammate> teammates = event.getAssignedMembers().stream()
                .filter(m -> !m.getUserId().equals(ue.getUserId()))
                .map(m -> {
                    IdentityServiceClient.UserContact contact = users.get(m.getUserId());
                    CrewRole role = m.getCrewRoleId() != null ? crewRoles.get(m.getCrewRoleId()) : null;
                    return AssignmentResponse.Teammate.builder()
                            .userId(m.getUserId())
                            .fullName(contact != null ? contact.fullName() : null)
                            .position(m.getPosition())
                            .crewRoleId(m.getCrewRoleId())
                            .crewRoleDepartment(role != null ? role.getDepartment() : null)
                            .crewRoleName(role != null ? role.getName() : null)
                            .status(m.getStatus())
                            .statusDisplayName(formatAssignStatus(m.getStatus()))
                            .build();
                })
                .toList();

        List<PayrollItemResponse> payrollItems = ue.getPayrollItems().stream()
                .map(item -> PayrollItemResponse.builder().label(item.getLabel()).amount(item.getAmount()).build())
                .toList();

        return AssignmentResponse.builder()
                .id(ue.getId())
                .eventId(event.getId())
                .eventName(event.getName())
                .eventDate(event.getEventDate())
                .location(event.getLocation())
                .userId(ue.getUserId())
                .userFullName(self != null ? self.fullName() : null)
                .position(ue.getPosition())
                .crewRoleId(ue.getCrewRoleId())
                .roleCastFee(ownRole != null ? ownRole.getCastFee() : null)
                .fundPercent(ue.getFundPercent())
                .fundAmount(ue.getFundAmount())
                .netAmount(ue.getNetAmount())
                .crewRoleDepartment(ownRole != null ? ownRole.getDepartment() : null)
                .crewRoleName(ownRole != null ? ownRole.getName() : null)
                .status(ue.getStatus())
                .statusDisplayName(formatAssignStatus(ue.getStatus()))
                .note(ue.getNote())
                .actualConcentrateAt(ue.getActualConcentrateAt())
                .checkinAt(ue.getCheckinAt())
                .checkoutAt(ue.getCheckoutAt())
                .checkinLat(ue.getCheckinLat())
                .checkinLng(ue.getCheckinLng())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .concentrateTime(event.getConcentrateTime())
                .concentrateLocation(event.getConcentrateLocation())
                .payrollItems(payrollItems)
                .totalPayroll(ue.getSalary())
                .teammates(teammates)
                .build();
    }

    /** Trưởng đoàn cần nhận ra người, không hiện "User #id". Họ tên trước, không có thì tên đăng nhập. */
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

    private String formatEventStatus(EventStatus status) {
        if (status == null) return "";
        return switch (status) {
            case PENDING_APPROVAL -> "Chờ duyệt tạo show";
            case SCHEDULED -> "Chờ duyệt";
            case CONFIRMED -> "Đã chốt show";
            case IN_PROGRESS -> "Đang diễn";
            case COMPLETED -> "Hoàn thành";
            case CANCELLED -> "Đã hủy";
        };
    }

    private String formatAssignStatus(AssignStatus status) {
        if (status == null) return "Chờ xác nhận";
        return switch (status) {
            case PENDING -> "Đang mời";
            case ACCEPTED -> "Sẵn sàng";
            case REJECTED -> "Từ chối";
            case COMPLETED -> "Đã diễn xong";
            case CHECKED_IN -> "Đã checkin diễn";
            case CHECKED_OUT -> "Đã checkout diễn";
            case CHECKIN_CONCENTRATE -> "Đã checkin tập trung";
        };
    }
}
