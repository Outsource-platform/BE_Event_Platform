package org.example.eventplatform.event.repository;

import org.example.eventplatform.event.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    void deleteByConversationId(Long conversationId);

    List<ChatMessage> findByConversationIdAndIdGreaterThanOrderByIdAsc(Long conversationId, Long afterId, Pageable pageable);
}
