package com.ticketbooking.service.impl;

import com.ticketbooking.model.Conversation;
import com.ticketbooking.model.Message;
import com.ticketbooking.repo.MessageRepo;
import com.ticketbooking.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageRepo messageRepo;

    @Override
    public Message saveMessage(Conversation conversation, String sender, String content) {
        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(content)
                .timestamp(LocalDateTime.now())
                .build();
        return messageRepo.save(message);
    }

    @Override
    public List<Message> getMessagesByConversation(Conversation conversation) {
        return messageRepo.findByConversationOrderByTimestampAsc(conversation);
    }
}
