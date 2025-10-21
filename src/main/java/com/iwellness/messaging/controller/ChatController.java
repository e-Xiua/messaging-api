package com.iwellness.messaging.controller;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.iwellness.messaging.dto.ChatMessageDTO;
import com.iwellness.messaging.service.MessagingService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
public class ChatController {

    private final MessagingService messagingService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatController(MessagingService messagingService, SimpMessagingTemplate messagingTemplate) {
        this.messagingService = messagingService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Maneja el envío de mensajes a través de WebSocket
     * Endpoint: /app/chat.send
     */ 
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessageDTO messageDTO, 
                           SimpMessageHeaderAccessor headerAccessor,
                           Principal principal) {
        try {
            // Obtener userId de los atributos de sesión WebSocket
            Long senderId = (Long) headerAccessor.getSessionAttributes().get("userId");
            
            log.info("Mensaje recibido de usuario {} para usuario {}", senderId, messageDTO.getReceiverId());

            // Guardar mensaje en la base de datos
            ChatMessageDTO savedMessage = messagingService.sendMessage(messageDTO);

            // Enviar mensaje al destinatario en tiempo real
            messagingTemplate.convertAndSendToUser(
                messageDTO.getReceiverId().toString(),
                "/queue/messages",
                savedMessage
            );

            // Confirmar al remitente
            messagingTemplate.convertAndSendToUser(
                senderId.toString(),
                "/queue/messages",
                savedMessage
            );

            log.info("Mensaje {} enviado exitosamente", savedMessage.getId());

        } catch (Exception e) {
            log.error("Error al procesar mensaje: {}", e.getMessage(), e);
        }
    } 

    /**
     * Marca un mensaje como leído
     * Endpoint: /app/chat.read
     */ 
    @MessageMapping("/chat.read")
    public void markAsRead(@Payload Long messageId, SimpMessageHeaderAccessor headerAccessor) {
        try {
            Long userId = (Long) headerAccessor.getSessionAttributes().get("userId");
            
            log.info("Usuario {} marcando mensaje {} como leído", userId, messageId);
            
            messagingService.markMessageAsRead(messageId, userId);
            
            // Notificar al remitente que su mensaje fue leído
            ChatMessageDTO message = messagingService.getMessageById(messageId);
            messagingTemplate.convertAndSendToUser(
                message.getSenderId().toString(),
                "/queue/read-receipts",
                messageId
            );

        } catch (Exception e) {
            log.error("Error al marcar mensaje como leído: {}", e.getMessage(), e);
        }
    } 

    /**
     * Notifica que un usuario está escribiendo
     * Endpoint: /app/chat.typing
     */
    @MessageMapping("/chat.typing")
    public void userTyping(@Payload Long receiverId, SimpMessageHeaderAccessor headerAccessor) {
        try {
            Long senderId = (Long) headerAccessor.getSessionAttributes().get("userId");
            String username = (String) headerAccessor.getSessionAttributes().get("username");
            
            log.debug("Usuario {} está escribiendo para {}", username, receiverId);
            
            // Notificar al destinatario
            messagingTemplate.convertAndSendToUser(
                receiverId.toString(),
                "/queue/typing",
                username
            );

        } catch (Exception e) {
            log.error("Error al notificar typing: {}", e.getMessage());
        }
    }
}  
