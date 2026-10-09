package org.example.eventplatform.identity.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.eventplatform.identity.dto.user.CreateMemberRequest;
import org.example.eventplatform.identity.dto.user.MemberResponse;
import org.example.eventplatform.identity.entity.Role;
import org.example.eventplatform.identity.entity.User;
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

    @Transactional(readOnly = true)
    public Page<MemberResponse> listTenantMembers(Long tenantId, Pageable pageable) {
        return userRepository.findByTenantId(tenantId, pageable).map(this::toResponse);
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
