package com.ticketbooking.repo;

import com.ticketbooking.model.Conversation;
import com.ticketbooking.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepo extends JpaRepository<Message, Long> {
    List<Message> findByConversationOrderByTimestampAsc(Conversation conversation);
}
