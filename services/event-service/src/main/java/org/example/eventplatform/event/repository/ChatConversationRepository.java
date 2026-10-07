package org.example.eventplatform.event.repository;

import org.example.eventplatform.event.entity.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {

    Optional<ChatConversation> findFirstByTenantIdAndCustomerUserIdAndEventId(Long tenantId, Long customerUserId, Long eventId);

    Optional<ChatConversation> findFirstByTenantIdAndCustomerUserIdAndEventIdIsNull(Long tenantId, Long customerUserId);

    List<ChatConversation> findByTenantIdOrderByLastMessageAtDesc(Long tenantId);

    List<ChatConversation> findByCustomerUserIdOrderByLastMessageAtDesc(Long customerUserId);
}
