package com.historyai.repository;

import com.historyai.entity.Message;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByConversationIdOrderByTimestampDesc(UUID conversationId, Pageable pageable);

    List<Message> findByConversationIdOrderByTimestampAsc(UUID conversationId);
}
