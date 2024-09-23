package com.ticketbooking.controller;

import com.ticketbooking.dto.MessageRequest;
import com.ticketbooking.model.Conversation;
import com.ticketbooking.model.Message;
import com.ticketbooking.model.User;
import com.ticketbooking.service.ConversationService;
import com.ticketbooking.service.MessageService;
import com.ticketbooking.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ConversationService conversationService;
    private final MessageService messageService;
    private final UserService userService;

    @PostMapping("/start")
    public Conversation startConversation(@RequestParam String customerUsername, @RequestParam String staffUsername) {
        User customer = userService.findByUsername(customerUsername);
        User staff = userService.findByUsername(staffUsername);
        return conversationService.getOrCreateConversation(customer, staff);
    }

    @GetMapping("/{conversationId}/messages")
    public List<Message> getMessages(@PathVariable Long conversationId) {
        Conversation conversation = conversationService.findById(conversationId);
        return messageService.getMessagesByConversation(conversation);
    }

    @PostMapping("/{conversationId}/send")
    public Message sendMessage(@PathVariable Long conversationId, @RequestBody MessageRequest messageRequest) {
        Conversation conversation = conversationService.findById(conversationId);
        return messageService.saveMessage(conversation, messageRequest.getSender(), messageRequest.getContent());
    }
}
