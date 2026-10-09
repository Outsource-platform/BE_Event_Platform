package org.example.eventplatform.identity.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.eventplatform.identity.dto.user.CreateMemberRequest;
import org.example.eventplatform.identity.dto.user.MemberResponse;
import org.example.eventplatform.identity.dto.user.TeammateResponse;
import org.example.eventplatform.identity.dto.user.UpdateProfileRequest;
import org.example.eventplatform.identity.entity.Role;
import org.example.eventplatform.identity.entity.User;
import org.example.eventplatform.identity.client.AccountCleanupClient;
import org.example.eventplatform.identity.entity.UserStatus;
import org.example.eventplatform.identity.repository.RoleRepository;
import org.example.eventplatform.identity.repository.TenantRepository;
import org.example.eventplatform.identity.repository.UserRepository;
import org.example.eventplatform.identity.entity.AvailabilityStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tenant-scoped member roster: an admin creates/lists/manages the accounts
 * that belong to their own tenant (troupe members). Deliberately separate
 * from AuthService (login/refresh) and TenantService (tenant self-register).
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private static final String DEFAULT_MEMBER_ROLE = "TN_MEMBER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountCleanupClient accountCleanupClient;

    @Transactional(readOnly = true)
    public Page<MemberResponse> listTenantMembers(Long tenantId, Pageable pageable) {
        return userRepository.findByTenantIdAndStatusNot(tenantId, UserStatus.DELETED, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public java.util.List<TeammateResponse> listTeammates(Long tenantId) {
        return userRepository.findByTenantId(tenantId, org.springframework.data.domain.Pageable.unpaged()).stream()
                .filter(u -> u.getStatus() == null || u.getStatus() == org.example.eventplatform.identity.entity.UserStatus.ACTIVE)
                .map(u -> new TeammateResponse(u.getId(),
                        u.getFullName() != null && !u.getFullName().isBlank() ? u.getFullName() : u.getUsername(),
                        u.getRoles() != null ? u.getRoles().getName() : null))
                .sorted(java.util.Comparator.comparing(TeammateResponse::fullName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public MemberResponse getTenantMember(Long tenantId, Long userId) {
        User user = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy thành viên với ID: " + userId));
        return toResponse(user);
    }

    @Transactional
    public MemberResponse createTenantMember(Long tenantId, CreateMemberRequest request) {
        if (userRepository.existsByTenantIdAndUsername(tenantId, request.getUsername())) {
            throw new IllegalStateException("Username đã tồn tại trong đoàn của bạn");
        }

        String roleName = request.getRoleName() != null ? request.getRoleName() : DEFAULT_MEMBER_ROLE;
        if (!DEFAULT_MEMBER_ROLE.equals(roleName) && !"ADMIN".equals(roleName)) {
            throw new IllegalArgumentException("Chỉ được gán vai trò ADMIN hoặc TN_MEMBER cho thành viên trong đội");
        }
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy vai trò: " + roleName));

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .tenant(tenantRepository.getReferenceById(tenantId))
                .roles(role)
                .status(UserStatus.ACTIVE)
                .isActive(true)
                .isVerified(true)
                .availabilityStatus(AvailabilityStatus.ACTIVE)
                .build();

        return toResponse(userRepository.save(user));
    }

    @Transactional
    public MemberResponse updateAvailability(Long tenantId, Long userId, AvailabilityStatus status) {
        User user = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy thành viên với ID: " + userId));
        user.setAvailabilityStatus(status);
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public MemberResponse updateCommissionRate(Long tenantId, Long userId, java.math.BigDecimal commissionRate) {
        User user = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy thành viên với ID: " + userId));
        user.setCommissionRate(commissionRate);
        return toResponse(userRepository.save(user));
    }

    /**
     * Trưởng đoàn xoá thành viên khỏi đoàn (xoá mềm): không đăng nhập được nữa, mất khỏi danh sách; họ tên giữ lại để lịch sử show
     * còn đọc được, còn liên hệ và ngân hàng bị xoá. Chỉ xoá được khi không còn suất diễn dở, điểm chưa rút hay yêu cầu rút đang chờ.
     */
    @Transactional
    public void deleteMember(Long tenantId, Long actingUserId, Long userId) {
        if (userId.equals(actingUserId)) {
            throw new IllegalArgumentException("Bạn không thể tự xoá tài khoản đang đăng nhập");
        }
        User user = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy thành viên với ID: " + userId));
        if (user.getStatus() == UserStatus.DELETED) {
            throw new EntityNotFoundException("Không tìm thấy thành viên với ID: " + userId);
        }
        String role = user.getRoles() != null ? user.getRoles().getName() : "";
        if ("ADMIN".equals(role)) {
            throw new IllegalArgumentException("Không xoá trưởng đoàn ở đây; hãy liên hệ hỗ trợ để bàn giao đơn vị");
        }
        AccountCleanupClient.MemberDeleteCheck check;
        try {
            check = accountCleanupClient.memberDeleteCheck(userId, tenantId);
        } catch (Exception ex) {
            throw new IllegalStateException("Hệ thống đang bận, vui lòng thử xoá lại sau ít phút");
        }
        java.util.List<String> reasons = new java.util.ArrayList<>();
        if (check.openAssignments() > 0) reasons.add(check.openAssignments() + " suất diễn chưa xong");
        if (check.openContracts() > 0) reasons.add(check.openContracts() + " show đang thầu chưa hoàn thành");
        if (check.pendingWithdrawals() > 0) reasons.add("yêu cầu rút điểm đang chờ duyệt");
        if (check.balance() != null && check.balance().signum() > 0) reasons.add("còn " + check.balance().stripTrailingZeros().toPlainString() + " điểm chưa rút");
        if (!reasons.isEmpty()) {
            throw new IllegalStateException("Chưa xoá được: " + String.join(", ", reasons));
        }
        try {
            accountCleanupClient.deleteNotificationData(userId);
        } catch (Exception ex) {
            throw new IllegalStateException("Hệ thống đang bận, vui lòng thử xoá lại sau ít phút");
        }
        user.setUsername("deleted_" + user.getId());
        user.setEmail(null);
        user.setPhone(null);
        BankAccounts.clear(user);
        user.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
        user.setVerificationToken(null);
        user.setIsActive(false);
        user.setStatus(UserStatus.DELETED);
        userRepository.save(user);
    }

    @Transactional
    public MemberResponse updateProfile(Long tenantId, Long userId, UpdateProfileRequest request) {
        User user = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy thành viên với ID: " + userId));
        String email = request.getEmail() == null || request.getEmail().isBlank() ? null : request.getEmail().trim();
        if (email != null && !email.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmailAndTenantId(email, tenantId)) {
            throw new IllegalStateException("Email này đã được dùng trong đoàn của bạn");
        }
        user.setFullName(request.getFullName().trim());
        user.setPhone(request.getPhone() == null || request.getPhone().isBlank() ? null : request.getPhone().trim());
        user.setEmail(email);
        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }
        return toResponse(userRepository.save(user));
    }

    /** Trưởng đoàn chỉnh các thông tin dùng để gán tỉ lệ: ngày vào đoàn, số năm kinh nghiệm, % quỹ đoàn, % hoa hồng. */
    @Transactional
    public MemberResponse updateFinanceProfile(Long tenantId, Long userId, java.time.LocalDate joinedDate, Integer seniority,
                                               java.math.BigDecimal teamFundPercent, java.math.BigDecimal commissionRate) {
        User user = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy thành viên với ID: " + userId));
        if (joinedDate != null && joinedDate.isAfter(java.time.LocalDate.now(org.example.eventplatform.shared.time.Clocks.VN))) {
            throw new IllegalArgumentException("Ngày vào đoàn không được ở tương lai");
        }
        user.setJoinedDate(joinedDate);
        user.setSeniority(seniority);
        user.setTeamFundPercent(teamFundPercent);
        user.setCommissionRate(commissionRate);
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public MemberResponse updateBankAccount(Long tenantId, Long userId, String bankName, String accountNumber, String accountHolder) {
        User user = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy thành viên với ID: " + userId));
        BankAccounts.apply(user, bankName, accountNumber, accountHolder);
        return toResponse(userRepository.save(user));
    }

    private MemberResponse toResponse(User user) {
        return MemberResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .tenantId(user.getTenant().getId())
                .roleName(user.getRoles() != null ? user.getRoles().getName() : null)
                .isActive(user.getIsActive())
                .availabilityStatus(user.getAvailabilityStatus())
                .commissionRate(user.getCommissionRate())
                .teamFundPercent(user.getTeamFundPercent())
                .joinedDate(user.getJoinedDate())
                .seniority(user.getSeniority())
                .bankName(user.getBankName())
                .bankAccountNumber(user.getBankAccountNumber())
                .bankAccountHolder(user.getBankAccountHolder())
                .build();
    }
}
