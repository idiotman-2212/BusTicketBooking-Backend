package com.ticketbooking.service;

import com.ticketbooking.model.Conversation;
import com.ticketbooking.model.User;

public interface ConversationService {
    Conversation getOrCreateConversation(User customer, User staff);
    Conversation findById(Long id);
}
