package org.example.eventplatform.customer.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional
    public CustomerResponse createCustomer(CustomerRequest request, Long tenantId) {
        if (customerRepository.existsByPhoneAndTenantId(request.getPhone(), tenantId)) {
            throw new RuntimeException("Số điện thoại này đã tồn tại trong hệ thống của bạn");
        }

        Customer customer = Customer.builder()
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .address(request.getAddress())
                .type(request.getType())
                .note(request.getNote())
                .assignedToUserId(request.getAssignedToUserId())
                .tenantId(tenantId)
                .active(true)
                .build();

        return toResponse(customerRepository.save(customer));
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> getCustomers(Long tenantId, String keyword, Pageable pageable) {
        return customerRepository.searchCustomers(tenantId, keyword, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Long id, Long tenantId) {
        return toResponse(getOrThrow(id, tenantId));
    }

    @Transactional
    public CustomerResponse updateCustomer(Long id, Long tenantId, CustomerRequest request) {
        Customer existing = getOrThrow(id, tenantId);

        if (!existing.getPhone().equals(request.getPhone())
                && customerRepository.existsByPhoneAndTenantId(request.getPhone(), tenantId)) {
            throw new RuntimeException("Số điện thoại mới đã bị trùng trong hệ thống");
        }

        existing.setFullName(request.getFullName());
        existing.setPhone(request.getPhone());
        existing.setEmail(request.getEmail());
        existing.setAddress(request.getAddress());
        existing.setType(request.getType());
        existing.setNote(request.getNote());
        existing.setAssignedToUserId(request.getAssignedToUserId());

        return toResponse(customerRepository.save(existing));
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
                .build();
    }

    private Customer getOrThrow(Long id, Long tenantId) {
        return customerRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy khách hàng"));
    }

    private CustomerResponse toResponse(Customer customer) {
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
