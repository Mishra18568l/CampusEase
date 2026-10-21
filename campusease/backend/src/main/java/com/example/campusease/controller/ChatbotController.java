package com.example.campusease.controller;

import com.example.campusease.dto.ChatbotRequest;
import com.example.campusease.dto.ChatbotResponse;
import com.example.campusease.service.ChatbotService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping("/ask")
    public ResponseEntity<ChatbotResponse> ask(@Valid @RequestBody ChatbotRequest request) {
        return ResponseEntity.ok(new ChatbotResponse(chatbotService.answer(request.getMessage())));
    }
}
