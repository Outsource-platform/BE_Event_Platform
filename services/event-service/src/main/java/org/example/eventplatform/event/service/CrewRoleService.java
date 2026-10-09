package org.example.eventplatform.event.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.eventplatform.event.dto.CrewRoleRequest;
import org.example.eventplatform.event.dto.CrewRoleResponse;
import org.example.eventplatform.event.entity.CrewRole;
import org.example.eventplatform.event.repository.CrewRoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CrewRoleService {

    private final CrewRoleRepository crewRoleRepository;

    @Transactional(readOnly = true)
    public List<CrewRoleResponse> listForTenant(Long tenantId) {
        return crewRoleRepository.findByTenantId(tenantId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public CrewRoleResponse create(Long tenantId, CrewRoleRequest request) {
        CrewRole role = CrewRole.builder()
                .tenantId(tenantId)
                .department(request.getDepartment())
                .name(request.getName())
                .description(request.getDescription())
                .castFee(request.getCastFee())
                .build();
        return toResponse(crewRoleRepository.save(role));
    }

    @Transactional
    public CrewRoleResponse update(Long tenantId, Long id, CrewRoleRequest request) {
        CrewRole role = getOwnedOrThrow(tenantId, id);
        role.setDepartment(request.getDepartment());
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        role.setCastFee(request.getCastFee());
        return toResponse(crewRoleRepository.save(role));
    }

    @Transactional
    public void delete(Long tenantId, Long id) {
        crewRoleRepository.delete(getOwnedOrThrow(tenantId, id));
    }

    CrewRole getOwnedOrThrow(Long tenantId, Long id) {
        return crewRoleRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy vị trí biểu diễn với ID: " + id));
    }

    private CrewRoleResponse toResponse(CrewRole role) {
        return CrewRoleResponse.builder()
                .id(role.getId())
                .department(role.getDepartment())
                .name(role.getName())
                .description(role.getDescription())
                .castFee(role.getCastFee())
                .build();
    }
}
