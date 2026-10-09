package org.example.eventplatform.customer.controller;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.eventplatform.customer.dto.internal.CustomerIdsRequest;
import org.example.eventplatform.customer.dto.internal.CustomerSummaryResponse;
import org.example.eventplatform.customer.dto.internal.FindOrCreateCustomerRequest;
import org.example.eventplatform.customer.entity.Customer;
import org.example.eventplatform.customer.repository.CustomerRepository;
import org.example.eventplatform.customer.service.CustomerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Service-to-service only — guarded by {@code X-Internal-Token}.
 */
@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalController {

    private final CustomerRepository customerRepository;
    private final CustomerService customerService;

    @GetMapping("/customers/{id}")
    public ResponseEntity<CustomerSummaryResponse> getCustomer(@PathVariable Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy khách hàng với ID: " + id));
        return ResponseEntity.ok(CustomerSummaryResponse.builder()
                .id(customer.getId())
                .tenantId(customer.getTenantId())
                .fullName(customer.getFullName())
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .userId(customer.getUserId())
                .assignedToUserId(customer.getAssignedToUserId())
                .build());
    }

    @PostMapping("/customers/find-or-create")
    public ResponseEntity<CustomerSummaryResponse> findOrCreate(@Valid @RequestBody FindOrCreateCustomerRequest request) {
        return ResponseEntity.ok(customerService.findOrCreate(request));
    }

    @PostMapping("/customers/anonymize")
    public ResponseEntity<Void> anonymize(@RequestParam("userId") Long userId) {
        customerService.anonymizeByUserId(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/customers/by-ids")
    public ResponseEntity<List<CustomerSummaryResponse>> byIds(@RequestBody CustomerIdsRequest request) {
        return ResponseEntity.ok(customerService.findByIds(request.ids()));
    }

    @GetMapping("/customers")
    public ResponseEntity<List<CustomerSummaryResponse>> listByUserId(@RequestParam("userId") Long userId) {
        return ResponseEntity.ok(customerService.findByUserId(userId));
    }
}
