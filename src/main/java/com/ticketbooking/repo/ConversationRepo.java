package com.ticketbooking.repo;

import com.ticketbooking.model.Conversation;
import com.ticketbooking.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConversationRepo extends JpaRepository<Conversation, Long> {
    Optional<Conversation> findByCustomerAndStaff(User customer, User staff);
}
