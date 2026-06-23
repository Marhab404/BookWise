package com.bookwise.chatbot.controller;

import com.bookwise.chatbot.service.ChatbotService;
import com.bookwise.chatbot.service.ChatbotService.ChatResponse;
import com.bookwise.chatbot.service.ChatbotService.PendingAction;
import com.bookwise.security.AppUserPrincipal;
import com.bookwise.user.entity.UserRole;
import jakarta.servlet.http.HttpSession;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chatbot")
public class ChatbotController {

    private static final Logger logger = LoggerFactory.getLogger(ChatbotController.class);

    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping("/messages")
    public ResponseEntity<?> handleMessage(
            @RequestBody MessageRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal,
            HttpSession session
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "User not authenticated"));
        }
        if (request.message() == null || request.message().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Message cannot be empty"));
        }

        try {
            ChatResponse chatResponse = chatbotService.handleMessage(
                    request.message().trim(),
                    request.previousInteractionId(),
                    principal,
                    session
            );
            return ResponseEntity.ok(chatResponse);
        } catch (Exception e) {
            logger.error("Error in chatbot handling message", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "An error occurred while processing your request. Please try again."));
        }
    }

    @PostMapping("/actions/confirm")
    public ResponseEntity<?> confirmAction(
            @RequestBody ConfirmRequest request,
            @AuthenticationPrincipal AppUserPrincipal principal,
            HttpSession session
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "User not authenticated"));
        }
        if (principal.role() != UserRole.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Only admins can perform mutations"));
        }
        if (request.actionId() == null || request.actionId().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Missing actionId"));
        }

        String sessionKey = "PENDING_ACTION_" + request.actionId();
        PendingAction pendingAction = (PendingAction) session.getAttribute(sessionKey);

        if (pendingAction == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Proposed action not found or expired. Please ask the chatbot to propose it again."));
        }

        if (pendingAction.expiresAt().isBefore(Instant.now())) {
            session.removeAttribute(sessionKey);
            return ResponseEntity.badRequest().body(Map.of("message", "Proposed action has expired. Please ask the chatbot to propose it again."));
        }

        try {
            chatbotService.executeAction(pendingAction);
            session.removeAttribute(sessionKey);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Action executed successfully."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error executing pending action: {}", pendingAction.actionId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", "Failed to execute action: " + e.getMessage()));
        }
    }

    public record MessageRequest(
            String message,
            String previousInteractionId
    ) {}

    public record ConfirmRequest(
            String actionId
    ) {}
}
