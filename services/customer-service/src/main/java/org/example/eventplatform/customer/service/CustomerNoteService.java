package org.example.eventplatform.customer.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.eventplatform.customer.dto.CustomerNoteRequest;
import org.example.eventplatform.customer.dto.CustomerNoteResponse;
import org.example.eventplatform.customer.entity.CustomerNote;
import org.example.eventplatform.customer.repository.CustomerNoteRepository;
import org.example.eventplatform.customer.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerNoteService {

    private final CustomerNoteRepository customerNoteRepository;
    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public List<CustomerNoteResponse> list(Long tenantId, Long customerId) {
        requireCustomer(tenantId, customerId);
        return customerNoteRepository.findByCustomerIdAndTenantIdOrderByCreatedAtDesc(customerId, tenantId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CustomerNoteResponse create(Long tenantId, Long userId, String username, Long customerId, CustomerNoteRequest request) {
        requireCustomer(tenantId, customerId);
        CustomerNote note = CustomerNote.builder()
                .customerId(customerId)
                .tenantId(tenantId)
                .body(request.getBody().trim())
                .authorUserId(userId)
                .authorName(username)
                .build();
        return toResponse(customerNoteRepository.save(note));
    }

    @Transactional
    public void delete(Long tenantId, Long customerId, Long noteId) {
        CustomerNote note = customerNoteRepository.findByIdAndCustomerIdAndTenantId(noteId, customerId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy ghi chú"));
        customerNoteRepository.delete(note);
    }

    private void requireCustomer(Long tenantId, Long customerId) {
        if (customerRepository.findByIdAndTenantId(customerId, tenantId).isEmpty()) {
            throw new EntityNotFoundException("Không tìm thấy khách hàng");
        }
    }

    private CustomerNoteResponse toResponse(CustomerNote note) {
        return CustomerNoteResponse.builder()
                .id(note.getId())
                .body(note.getBody())
                .authorName(note.getAuthorName())
                .createdAt(note.getCreatedAt())
                .build();
    }
}
