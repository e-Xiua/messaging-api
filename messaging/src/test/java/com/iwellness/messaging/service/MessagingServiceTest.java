package com.iwellness.messaging.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.iwellness.messaging.clientes.UserApiClient;
import com.iwellness.messaging.dto.ChatMessageDTO;
import com.iwellness.messaging.dto.ConversationDetailDTO;
import com.iwellness.messaging.dto.ConversationSummaryDTO;
import com.iwellness.messaging.dto.UsuarioDTO;
import com.iwellness.messaging.entity.Conversation;
import com.iwellness.messaging.entity.Message;
import com.iwellness.messaging.repository.ConversationRepository;
import com.iwellness.messaging.repository.MessageRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests para MessagingService")
class MessagingServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserApiClient userApiClient;

    @InjectMocks
    private MessagingService messagingService;

    private Conversation conversation;
    private Message message;
    private ChatMessageDTO chatMessageDTO;
    private UsuarioDTO usuarioDTO1;
    private UsuarioDTO usuarioDTO2;

    @BeforeEach
    void setUp() {
        // Setup de conversación
        conversation = new Conversation();
        conversation.setId(1L);
        conversation.setUser1Id(100L);
        conversation.setUser2Id(200L);
        conversation.setCreatedAt(LocalDateTime.now());
        conversation.setUpdatedAt(LocalDateTime.now());

        // Setup de mensaje
        message = new Message();
        message.setId(1L);
        message.setConversation(conversation);
        message.setSenderId(100L);
        message.setReceiverId(200L);
        message.setContent("Test message");
        message.setSentAt(LocalDateTime.now());
        message.setIsRead(false);

        // Setup de DTO de mensaje
        chatMessageDTO = ChatMessageDTO.builder()
                .id(1L)
                .conversationId(1L)
                .senderId(100L)
                .receiverId(200L)
                .content("Test message")
                .sentAt(LocalDateTime.now())
                .isRead(false)
                .build();

        // Setup de usuarios
        usuarioDTO1 = new UsuarioDTO();
        usuarioDTO1.setId(100L);
        usuarioDTO1.setNombre("Usuario 1");

        usuarioDTO2 = new UsuarioDTO();
        usuarioDTO2.setId(200L);
        usuarioDTO2.setNombre("Usuario 2");
    }

    @Test
    @DisplayName("sendMessage - Crear nuevo mensaje en conversación existente")
    void testSendMessage_ExistingConversation() {
        // Given
        when(conversationRepository.findByUsers(100L, 200L)).thenReturn(Optional.of(conversation));
        when(messageRepository.save(any(Message.class))).thenReturn(message);

        ChatMessageDTO inputDTO = ChatMessageDTO.builder()
                .senderId(100L)
                .receiverId(200L)
                .content("Test message")
                .build();

        // When
        ChatMessageDTO result = messagingService.sendMessage(inputDTO);

        // Then
        assertNotNull(result);
        assertEquals(100L, result.getSenderId());
        assertEquals(200L, result.getReceiverId());
        assertEquals("Test message", result.getContent());
        assertEquals(false, result.getIsRead());
        verify(conversationRepository, times(1)).findByUsers(100L, 200L);
        verify(conversationRepository, times(1)).save(conversation);
        verify(messageRepository, times(1)).save(any(Message.class));
    }

    @Test
    @DisplayName("sendMessage - Crear nuevo mensaje y nueva conversación")
    void testSendMessage_NewConversation() {
        // Given
        when(conversationRepository.findByUsers(100L, 200L)).thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
        when(messageRepository.save(any(Message.class))).thenReturn(message);

        ChatMessageDTO inputDTO = ChatMessageDTO.builder()
                .senderId(100L)
                .receiverId(200L)
                .content("First message")
                .build();

        // When
        ChatMessageDTO result = messagingService.sendMessage(inputDTO);

        // Then
        assertNotNull(result);
        assertEquals(100L, result.getSenderId());
        assertEquals(200L, result.getReceiverId());
        verify(conversationRepository, times(1)).findByUsers(100L, 200L);
        verify(conversationRepository, times(2)).save(any(Conversation.class)); // Una vez para crear, otra para actualizar
        verify(messageRepository, times(1)).save(any(Message.class));
    }

    @Test
    @DisplayName("getConversationSummaries - Obtener resúmenes de conversaciones")
    void testGetConversationSummaries() {
        // Given
        Long userId = 100L;
        List<Conversation> conversations = Arrays.asList(conversation);
        
        when(conversationRepository.findByParticipant(userId)).thenReturn(conversations);
        when(userApiClient.findById(200L)).thenReturn(usuarioDTO2);
        when(messageRepository.findTopByConversationIdOrderBySentAtDesc(1L)).thenReturn(Optional.of(message));
        when(messageRepository.countByConversationIdAndReceiverIdAndIsReadIsFalse(1L, userId)).thenReturn(3L);

        // When
        List<ConversationSummaryDTO> result = messagingService.getConversationSummaries(userId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        ConversationSummaryDTO summary = result.get(0);
        assertEquals(1L, summary.getId());
        assertEquals(3L, summary.getUnreadCount());
        assertNotNull(summary.getOtherParticipant());
        assertEquals(200L, summary.getOtherParticipant().getId());
        verify(conversationRepository, times(1)).findByParticipant(userId);
        verify(userApiClient, times(1)).findById(200L);
    }

    @Test
    @DisplayName("getConversationSummaries - Lista vacía cuando no hay conversaciones")
    void testGetConversationSummaries_Empty() {
        // Given
        Long userId = 100L;
        when(conversationRepository.findByParticipant(userId)).thenReturn(Arrays.asList());

        // When
        List<ConversationSummaryDTO> result = messagingService.getConversationSummaries(userId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(conversationRepository, times(1)).findByParticipant(userId);
    }

    @Test
    @DisplayName("createOrGetConversation - Crear nueva conversación")
    void testCreateOrGetConversation_New() {
        // Given
        Long senderId = 100L;
        Long receiverId = 200L;
        
        when(conversationRepository.findByUsers(senderId, receiverId)).thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conversation);
        when(userApiClient.findById(receiverId)).thenReturn(usuarioDTO2);
        when(messageRepository.findTopByConversationIdOrderBySentAtDesc(1L)).thenReturn(Optional.empty());
        when(messageRepository.countByConversationIdAndReceiverIdAndIsReadIsFalse(1L, senderId)).thenReturn(0L);

        // When
        ConversationSummaryDTO result = messagingService.createOrGetConversation(senderId, receiverId);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(conversationRepository, times(1)).findByUsers(senderId, receiverId);
        verify(conversationRepository, times(2)).save(any(Conversation.class)); // Una vez en orElseGet, otra al actualizar timestamp
    }

    @Test
    @DisplayName("createOrGetConversation - Obtener conversación existente")
    void testCreateOrGetConversation_Existing() {
        // Given
        Long senderId = 100L;
        Long receiverId = 200L;
        
        when(conversationRepository.findByUsers(senderId, receiverId)).thenReturn(Optional.of(conversation));
        when(conversationRepository.save(conversation)).thenReturn(conversation);
        when(userApiClient.findById(receiverId)).thenReturn(usuarioDTO2);
        when(messageRepository.findTopByConversationIdOrderBySentAtDesc(1L)).thenReturn(Optional.of(message));
        when(messageRepository.countByConversationIdAndReceiverIdAndIsReadIsFalse(1L, senderId)).thenReturn(2L);

        // When
        ConversationSummaryDTO result = messagingService.createOrGetConversation(senderId, receiverId);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(2L, result.getUnreadCount());
        verify(conversationRepository, times(1)).findByUsers(senderId, receiverId);
        verify(conversationRepository, times(1)).save(conversation);
    }

    @Test
    @DisplayName("getConversationDetails - Obtener detalles de conversación")
    void testGetConversationDetails_Success() {
        // Given
        Long conversationId = 1L;
        Long userId = 100L;
        List<Message> messages = Arrays.asList(message);
        
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(userApiClient.findById(100L)).thenReturn(usuarioDTO1);
        when(userApiClient.findById(200L)).thenReturn(usuarioDTO2);
        when(messageRepository.findByConversationIdOrderBySentAtAsc(conversationId)).thenReturn(messages);

        // When
        ConversationDetailDTO result = messagingService.getConversationDetails(conversationId, userId);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertNotNull(result.getParticipant1());
        assertNotNull(result.getParticipant2());
        assertEquals(1, result.getMessages().size());
        verify(conversationRepository, times(1)).findById(conversationId);
        verify(userApiClient, times(2)).findById(anyLong());
        verify(messageRepository, times(1)).findByConversationIdOrderBySentAtAsc(conversationId);
    }

    @Test
    @DisplayName("getConversationDetails - Excepción cuando conversación no existe")
    void testGetConversationDetails_NotFound() {
        // Given
        Long conversationId = 999L;
        Long userId = 100L;
        
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            messagingService.getConversationDetails(conversationId, userId);
        });
        verify(conversationRepository, times(1)).findById(conversationId);
    }

    @Test
    @DisplayName("getConversationDetails - Excepción cuando usuario no es participante")
    void testGetConversationDetails_SecurityException() {
        // Given
        Long conversationId = 1L;
        Long userId = 999L; // Usuario que no es participante
        
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));

        // When & Then
        assertThrows(SecurityException.class, () -> {
            messagingService.getConversationDetails(conversationId, userId);
        });
        verify(conversationRepository, times(1)).findById(conversationId);
    }

    @Test
    @DisplayName("markMessageAsRead - Marcar mensaje como leído")
    void testMarkMessageAsRead_Success() {
        // Given
        Long messageId = 1L;
        Long userId = 200L; // Receiver
        message.setIsRead(false);
        
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));
        when(messageRepository.save(message)).thenReturn(message);

        // When
        ChatMessageDTO result = messagingService.markMessageAsRead(messageId, userId);

        // Then
        assertNotNull(result);
        assertTrue(message.getIsRead());
        verify(messageRepository, times(1)).findById(messageId);
        verify(messageRepository, times(1)).save(message);
    }

    @Test
    @DisplayName("markMessageAsRead - Excepción cuando mensaje no existe")
    void testMarkMessageAsRead_NotFound() {
        // Given
        Long messageId = 999L;
        Long userId = 200L;
        
        when(messageRepository.findById(messageId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            messagingService.markMessageAsRead(messageId, userId);
        });
        verify(messageRepository, times(1)).findById(messageId);
        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("markMessageAsRead - Excepción cuando usuario no es el receptor")
    void testMarkMessageAsRead_SecurityException() {
        // Given
        Long messageId = 1L;
        Long userId = 999L; // No es el receptor
        
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));

        // When & Then
        assertThrows(SecurityException.class, () -> {
            messagingService.markMessageAsRead(messageId, userId);
        });
        verify(messageRepository, times(1)).findById(messageId);
        verify(messageRepository, never()).save(any());
    }

    @Test
    @DisplayName("getMessageById - Obtener mensaje por ID")
    void testGetMessageById_Success() {
        // Given
        Long messageId = 1L;
        when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));

        // When
        ChatMessageDTO result = messagingService.getMessageById(messageId);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(100L, result.getSenderId());
        assertEquals(200L, result.getReceiverId());
        assertEquals("Test message", result.getContent());
        verify(messageRepository, times(1)).findById(messageId);
    }

    @Test
    @DisplayName("getMessageById - Excepción cuando mensaje no existe")
    void testGetMessageById_NotFound() {
        // Given
        Long messageId = 999L;
        when(messageRepository.findById(messageId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            messagingService.getMessageById(messageId);
        });
        verify(messageRepository, times(1)).findById(messageId);
    }

    @Test
    @DisplayName("sendMessage - Contenido vacío")
    void testSendMessage_EmptyContent() {
        // Given
        when(conversationRepository.findByUsers(100L, 200L)).thenReturn(Optional.of(conversation));
        when(messageRepository.save(any(Message.class))).thenReturn(message);

        ChatMessageDTO inputDTO = ChatMessageDTO.builder()
                .senderId(100L)
                .receiverId(200L)
                .content("")
                .build();

        // When
        ChatMessageDTO result = messagingService.sendMessage(inputDTO);

        // Then
        assertNotNull(result);
        verify(messageRepository, times(1)).save(any(Message.class));
    }

    @Test
    @DisplayName("getConversationSummaries - Usuario con múltiples conversaciones")
    void testGetConversationSummaries_Multiple() {
        // Given
        Long userId = 100L;
        
        Conversation conv2 = new Conversation();
        conv2.setId(2L);
        conv2.setUser1Id(100L);
        conv2.setUser2Id(300L);
        conv2.setCreatedAt(LocalDateTime.now());
        conv2.setUpdatedAt(LocalDateTime.now());
        
        UsuarioDTO usuario3 = new UsuarioDTO();
        usuario3.setId(300L);
        usuario3.setNombre("Usuario 3");
        
        List<Conversation> conversations = Arrays.asList(conversation, conv2);
        
        when(conversationRepository.findByParticipant(userId)).thenReturn(conversations);
        when(userApiClient.findById(200L)).thenReturn(usuarioDTO2);
        when(userApiClient.findById(300L)).thenReturn(usuario3);
        when(messageRepository.findTopByConversationIdOrderBySentAtDesc(1L)).thenReturn(Optional.of(message));
        when(messageRepository.findTopByConversationIdOrderBySentAtDesc(2L)).thenReturn(Optional.empty());
        when(messageRepository.countByConversationIdAndReceiverIdAndIsReadIsFalse(anyLong(), eq(userId))).thenReturn(0L);

        // When
        List<ConversationSummaryDTO> result = messagingService.getConversationSummaries(userId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(conversationRepository, times(1)).findByParticipant(userId);
        verify(userApiClient, times(2)).findById(anyLong());
    }
}
