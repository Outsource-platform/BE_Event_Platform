package org.example.eventplatform.customer.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.eventplatform.customer.client.IdentityServiceClient;
import org.example.eventplatform.customer.dto.CustomerLookupResponse;
import org.example.eventplatform.customer.dto.CustomerRequest;
import org.example.eventplatform.customer.dto.CustomerResponse;
import org.example.eventplatform.customer.dto.internal.CustomerSummaryResponse;
import org.example.eventplatform.customer.dto.internal.FindOrCreateCustomerRequest;
import org.example.eventplatform.customer.entity.Customer;
import org.example.eventplatform.customer.entity.CustomerType;
import org.example.eventplatform.customer.repository.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final IdentityServiceClient identityServiceClient;

    /** Người đang gọi: thành viên chỉ thấy và làm việc với khách mình phụ trách, trưởng đoàn thấy cả đoàn. */
    public record Caller(Long userId, Long tenantId, boolean admin) {
    }

    @Transactional
    public CustomerResponse createCustomer(CustomerRequest request, Caller caller) {
        var existing = customerRepository.findByPhoneAndTenantId(request.getPhone(), caller.tenantId());
        if (existing.isPresent()) {
            throw new IllegalArgumentException(duplicateMessage(existing.get()));
        }

        // Thành viên tạo khách thì khách là của người đó; trưởng đoàn tạo thì để trống (khách của đoàn) hoặc chọn người phụ trách.
        Long owner = caller.admin() ? request.getAssignedToUserId() : caller.userId();
        if (owner != null) {
            requireTroupeMember(owner, caller.tenantId());
        }

        Customer customer = Customer.builder()
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .address(request.getAddress())
                .type(request.getType())
                .note(request.getNote())
                .assignedToUserId(owner)
                .tenantId(caller.tenantId())
                .active(true)
                .build();

        return toResponse(customerRepository.save(customer), names(List.of(customer)));
    }

    private String duplicateMessage(Customer existing) {
        String owner = existing.getAssignedToUserId() == null ? null
                : identityServiceClient.findUsers(List.of(existing.getAssignedToUserId())).values().stream()
                        .findFirst().map(IdentityServiceClient.UserInfo::displayName).orElse(null);
        return owner == null
                ? "Số điện thoại này đã có trong danh sách khách của đoàn"
                : "Khách này đã có người phụ trách là " + owner + ". Hãy tra theo số điện thoại để tạo show cho khách đó";
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> getCustomers(Caller caller, String keyword, Pageable pageable) {
        Page<Customer> page = customerRepository.searchCustomers(caller.tenantId(), caller.admin() ? null : caller.userId(), keyword, pageable);
        Map<Long, String> names = names(page.getContent());
        return page.map(c -> toResponse(c, names));
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Long id, Caller caller) {
        Customer customer = getVisible(id, caller);
        return toResponse(customer, names(List.of(customer)));
    }

    @Transactional
    public CustomerResponse updateCustomer(Long id, Long tenantId, CustomerRequest request) {
        Customer existing = getOrThrow(id, tenantId);

        if (!existing.getPhone().equals(request.getPhone())
                && customerRepository.existsByPhoneAndTenantId(request.getPhone(), tenantId)) {
            throw new IllegalArgumentException("Số điện thoại mới đã bị trùng trong hệ thống");
        }

        existing.setFullName(request.getFullName());
        existing.setPhone(request.getPhone());
        existing.setEmail(request.getEmail());
        existing.setAddress(request.getAddress());
        existing.setType(request.getType());
        existing.setNote(request.getNote());
        // Người phụ trách đổi qua assign(), không bị xoá nhầm khi sửa thông tin liên hệ.

        Customer saved = customerRepository.save(existing);
        return toResponse(saved, names(List.of(saved)));
    }

    /** Trưởng đoàn đổi người phụ trách bất kỳ lúc nào; người đang phụ trách chuyển cho đồng đội khác. */
    @Transactional
    public CustomerResponse assign(Long id, Caller caller, Long newOwner) {
        Customer customer = getVisible(id, caller);
        if (newOwner == null && !caller.admin()) {
            throw new IllegalArgumentException("Chỉ trưởng đoàn mới bỏ người phụ trách của khách");
        }
        if (newOwner != null) {
            requireTroupeMember(newOwner, caller.tenantId());
        }
        customer.setAssignedToUserId(newOwner);
        Customer saved = customerRepository.save(customer);
        return toResponse(saved, names(List.of(saved)));
    }

    /**
     * Tra khách theo số điện thoại đúng từng chữ số, để thành viên tạo show hộ khách của đồng đội
     * mà không phải xem cả danh sách khách của người khác. Chỉ trả tên và người phụ trách.
     */
    @Transactional(readOnly = true)
    public CustomerLookupResponse lookup(String phone, Caller caller) {
        Customer customer = customerRepository.findByPhoneAndTenantId(phone == null ? "" : phone.trim(), caller.tenantId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy khách có số điện thoại này"));
        Map<Long, String> names = names(List.of(customer));
        return CustomerLookupResponse.builder()
                .id(customer.getId())
                .fullName(customer.getFullName())
                .assignedToUserId(customer.getAssignedToUserId())
                .assignedToName(names.get(customer.getAssignedToUserId()))
                .mine(caller.userId().equals(customer.getAssignedToUserId()))
                .build();
    }

    private void requireTroupeMember(Long userId, Long tenantId) {
        var user = identityServiceClient.findUsers(List.of(userId)).get(userId);
        if (user == null || !tenantId.equals(user.tenantId())) {
            throw new IllegalArgumentException("Người phụ trách phải là thành viên của đoàn");
        }
    }

    private Customer getVisible(Long id, Caller caller) {
        Customer customer = getOrThrow(id, caller.tenantId());
        if (!caller.admin() && !caller.userId().equals(customer.getAssignedToUserId())) {
            throw new EntityNotFoundException("Không tìm thấy khách hàng");
        }
        return customer;
    }

    private Map<Long, String> names(List<Customer> customers) {
        java.util.Set<Long> ids = customers.stream().map(Customer::getAssignedToUserId).filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        Map<Long, String> out = new java.util.HashMap<>();
        identityServiceClient.findUsers(ids).forEach((id, u) -> out.put(id, u.displayName()));
        return out;
    }

    @Transactional
    public void deleteCustomer(Long id, Long tenantId) {
        Customer customer = getOrThrow(id, tenantId);
        customerRepository.delete(customer);
    }

    /**
     * Marketplace booking path: reuse the CRM row for (tenant, phone) if it
     * already exists, otherwise create a new INDIVIDUAL customer and link it
     * to the CUSTOMER app account via {@code userId}.
     */
    @Transactional
    public CustomerSummaryResponse findOrCreate(FindOrCreateCustomerRequest request) {
        String phone = request.getPhone().trim();
        return customerRepository.findByPhoneAndTenantId(phone, request.getTenantId())
                .map(existing -> {
                    if (existing.getUserId() == null && request.getUserId() != null) {
                        existing.setUserId(request.getUserId());
                        return toSummary(customerRepository.save(existing));
                    }
                    return toSummary(existing);
                })
                .orElseGet(() -> {
                    String name = request.getFullName() != null && !request.getFullName().isBlank()
                            ? request.getFullName().trim()
                            : phone;
                    Customer created = Customer.builder()
                            .fullName(name)
                            .phone(phone)
                            .email(request.getEmail())
                            .type(CustomerType.INDIVIDUAL)
                            .userId(request.getUserId())
                            .tenantId(request.getTenantId())
                            .active(true)
                            .build();
                    return toSummary(customerRepository.save(created));
                });
    }

    /** Tra nhiều khách một lần để danh sách show không phải gọi từng người (tránh N+1 giữa các service). */
    @Transactional(readOnly = true)
    public List<CustomerSummaryResponse> findByIds(java.util.Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return customerRepository.findAllById(ids).stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public List<CustomerSummaryResponse> findByUserId(Long userId) {
        return customerRepository.findByUserId(userId).stream().map(this::toSummary).toList();
    }

    private CustomerSummaryResponse toSummary(Customer customer) {
        return CustomerSummaryResponse.builder()
                .id(customer.getId())
                .tenantId(customer.getTenantId())
                .fullName(customer.getFullName())
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .userId(customer.getUserId())
                .assignedToUserId(customer.getAssignedToUserId())
                .build();
    }

    private Customer getOrThrow(Long id, Long tenantId) {
        return customerRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy khách hàng"));
    }

    private CustomerResponse toResponse(Customer customer, Map<Long, String> names) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .fullName(customer.getFullName())
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .address(customer.getAddress())
                .type(customer.getType())
                .note(customer.getNote())
                .active(customer.isActive())
                .assignedToUserId(customer.getAssignedToUserId())
                .assignedToName(customer.getAssignedToUserId() == null ? null : names.get(customer.getAssignedToUserId()))
                .tenantId(customer.getTenantId())
                .createdAt(customer.getCreatedAt())
                .build();
    }

    /**
     * Khi khách xoá tài khoản: các bản ghi CRM của họ ở từng đơn vị được ẩn danh (giữ lại để lịch sử show,
     * đối soát của đơn vị không vỡ) và ngắt liên kết với tài khoản.
     */
    @Transactional
    public int anonymizeByUserId(Long userId) {
        java.util.List<Customer> rows = customerRepository.findByUserId(userId);
        for (Customer c : rows) {
            c.setFullName("Khách đã xóa tài khoản");
            c.setPhone("deleted-" + c.getId()); // cột bắt buộc và duy nhất theo từng đơn vị
            c.setEmail(null);
            c.setAddress(null);
            c.setNote(null);
            c.setUserId(null);
            c.setActive(false);
        }
        customerRepository.saveAll(rows);
        return rows.size();
    }
}
