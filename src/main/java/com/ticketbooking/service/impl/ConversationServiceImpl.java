package com.ticketbooking.service.impl;

import com.ticketbooking.model.Conversation;
import com.ticketbooking.model.User;
import com.ticketbooking.repo.ConversationRepo;
import com.ticketbooking.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepo conversationRepo;

    @Override
    public Conversation getOrCreateConversation(User customer, User staff) {
        Optional<Conversation> conversation = conversationRepo.findByCustomerAndStaff(customer, staff);
        if (conversation.isPresent()) {
            return conversation.get();
        }
        Conversation newConversation = Conversation.builder()
                .customer(customer)
                .staff(staff)
                .build();
        return conversationRepo.save(newConversation);
    }

    @Override
    public Conversation findById(Long id) {
        return conversationRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
    }
}
