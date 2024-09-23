package com.ticketbooking.service;

import com.ticketbooking.model.Conversation;
import com.ticketbooking.model.Message;

import java.util.List;

public interface MessageService {
    Message saveMessage(Conversation conversation, String sender, String content);
    List<Message> getMessagesByConversation(Conversation conversation);
}
