package com.iwellness.messaging.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.security.Principal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.iwellness.messaging.dto.ChatMessageDTO;
import com.iwellness.messaging.service.MessagingService;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests para ChatController")
class ChatControllerTest {

    @Mock
    private MessagingService messagingService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private SimpMessageHeaderAccessor headerAccessor;

    @Mock
    private Principal principal;

    @InjectMocks
    private ChatController chatController;

    private ChatMessageDTO chatMessageDTO;

    @BeforeEach
    void setUp() {
        chatMessageDTO = ChatMessageDTO.builder()
                .id(1L)
                .conversationId(1L)
                .senderId(100L)
                .receiverId(200L)
                .content("WebSocket test message")
                .sentAt(LocalDateTime.now())
                .isRead(false)
                .build();
        
        // No es necesario configurar principal en setUp si no se usa en todos los tests
    }

    @Test
    @DisplayName("sendMessage - Enviar mensaje por WebSocket exitosamente")
    void testSendMessage_Success() {
        // Given
        when(messagingService.sendMessage(any(ChatMessageDTO.class))).thenReturn(chatMessageDTO);

        // When
        chatController.sendMessage(chatMessageDTO, headerAccessor, principal);

        // Then
        verify(messagingService, times(1)).sendMessage(chatMessageDTO);
        verify(messagingTemplate, times(1)).convertAndSendToUser(
                eq("200"),
                eq("/queue/messages"),
                eq(chatMessageDTO)
        );
    }

    @Test
    @DisplayName("sendMessage - Mensaje con contenido vacío")
    void testSendMessage_EmptyContent() {
        // Given
        ChatMessageDTO emptyMessage = ChatMessageDTO.builder()
                .senderId(100L)
                .receiverId(200L)
                .content("")
                .build();

        when(messagingService.sendMessage(any(ChatMessageDTO.class))).thenReturn(emptyMessage);

        // When
        chatController.sendMessage(emptyMessage, headerAccessor, principal);

        // Then
        verify(messagingService, times(1)).sendMessage(emptyMessage);
        verify(messagingTemplate, times(1)).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("sendMessage - Verificar envío a usuario correcto")
    void testSendMessage_CorrectRecipient() {
        // Given
        Long receiverId = 200L;
        chatMessageDTO.setReceiverId(receiverId);
        when(messagingService.sendMessage(any(ChatMessageDTO.class))).thenReturn(chatMessageDTO);

        // When
        chatController.sendMessage(chatMessageDTO, headerAccessor, principal);

        // Then
        verify(messagingTemplate, times(1)).convertAndSendToUser(
                eq(receiverId.toString()),
                eq("/queue/messages"),
                eq(chatMessageDTO)
        );
    }

    @Test
    @DisplayName("sendMessage - Mensaje sin ID (nuevo mensaje)")
    void testSendMessage_NewMessage() {
        // Given
        ChatMessageDTO newMessage = ChatMessageDTO.builder()
                .senderId(100L)
                .receiverId(200L)
                .content("New message")
                .build();

        ChatMessageDTO savedMessage = ChatMessageDTO.builder()
                .id(5L)
                .senderId(100L)
                .receiverId(200L)
                .content("New message")
                .sentAt(LocalDateTime.now())
                .isRead(false)
                .build();

        when(messagingService.sendMessage(newMessage)).thenReturn(savedMessage);

        // When
        chatController.sendMessage(newMessage, headerAccessor, principal);

        // Then
        verify(messagingService, times(1)).sendMessage(newMessage);
    }

    @Test
    @DisplayName("sendMessage - Llamada exitosa verifica mocks")
    void testSendMessage_VerifyMocks() {
        // Given
        when(messagingService.sendMessage(any(ChatMessageDTO.class))).thenReturn(chatMessageDTO);

        // When
        chatController.sendMessage(chatMessageDTO, headerAccessor, principal);

        // Then
        verify(messagingService, times(1)).sendMessage(chatMessageDTO);
        verify(messagingTemplate, times(1)).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("sendMessage - Múltiples mensajes consecutivos")
    void testSendMessage_MultipleMessages() {
        // Given
        ChatMessageDTO message1 = ChatMessageDTO.builder()
                .senderId(100L).receiverId(200L).content("Message 1").build();
        ChatMessageDTO message2 = ChatMessageDTO.builder()
                .senderId(100L).receiverId(200L).content("Message 2").build();

        when(messagingService.sendMessage(any(ChatMessageDTO.class)))
                .thenReturn(message1)
                .thenReturn(message2);

        // When
        chatController.sendMessage(message1, headerAccessor, principal);
        chatController.sendMessage(message2, headerAccessor, principal);

        // Then
        verify(messagingService, times(2)).sendMessage(any(ChatMessageDTO.class));
        verify(messagingTemplate, times(2)).convertAndSendToUser(anyString(), anyString(), any());
    }
}
