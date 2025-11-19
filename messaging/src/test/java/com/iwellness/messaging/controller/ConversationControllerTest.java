package com.iwellness.messaging.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iwellness.messaging.clientes.UserApiClient;
import com.iwellness.messaging.dto.ChatMessageDTO;
import com.iwellness.messaging.dto.ConversationDetailDTO;
import com.iwellness.messaging.dto.ConversationSummaryDTO;
import com.iwellness.messaging.dto.UsuarioDTO;
import com.iwellness.messaging.service.MessagingService;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests para ConversationController")
class ConversationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MessagingService messagingService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private UserApiClient userApiClient;

    @InjectMocks
    private ConversationController conversationController;

    private ObjectMapper objectMapper;
    private ChatMessageDTO chatMessageDTO;
    private ConversationSummaryDTO conversationSummaryDTO;
    private ConversationDetailDTO conversationDetailDTO;
    private UsuarioDTO usuarioDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(conversationController).build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        // Setup DTOs
        usuarioDTO = new UsuarioDTO();
        usuarioDTO.setId(200L);
        usuarioDTO.setNombre("Usuario Test");

        chatMessageDTO = ChatMessageDTO.builder()
                .id(1L)
                .conversationId(1L)
                .senderId(100L)
                .receiverId(200L)
                .content("Test message")
                .sentAt(LocalDateTime.now())
                .isRead(false)
                .build();

        conversationSummaryDTO = ConversationSummaryDTO.builder()
                .id(1L)
                .lastMessageAt(LocalDateTime.now())
                .otherParticipant(usuarioDTO)
                .lastMessage(chatMessageDTO)
                .unreadCount(3L)
                .build();

        conversationDetailDTO = ConversationDetailDTO.builder()
                .id(1L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .participant1(usuarioDTO)
                .participant2(usuarioDTO)
                .messages(Arrays.asList(chatMessageDTO))
                .build();
    }

    @Test
    @DisplayName("sendMessage - Enviar mensaje exitosamente")
    void testSendMessage_Success() throws Exception {
        // Given
        Long authenticatedUserId = 100L;
        when(messagingService.sendMessage(any(ChatMessageDTO.class))).thenReturn(chatMessageDTO);

        // When & Then
        mockMvc.perform(post("/api/messages")
                .header("X-User-Id", authenticatedUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(chatMessageDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.senderId").value(100))
                .andExpect(jsonPath("$.receiverId").value(200))
                .andExpect(jsonPath("$.content").value("Test message"));

        verify(messagingService, times(1)).sendMessage(any(ChatMessageDTO.class));
        verify(messagingTemplate, times(1)).convertAndSendToUser(anyString(), anyString(), any(ChatMessageDTO.class));
    }

    @Test
    @DisplayName("sendMessage - Error cuando senderId no coincide")
    void testSendMessage_UnauthorizedSender() throws Exception {
        // Given
        Long authenticatedUserId = 999L; // Diferente al senderId del mensaje
        chatMessageDTO.setSenderId(100L);

        // When & Then
        mockMvc.perform(post("/api/messages")
                .header("X-User-Id", authenticatedUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(chatMessageDTO)))
                .andExpect(status().isForbidden());

        verify(messagingService, never()).sendMessage(any());
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("getUserConversations - Obtener conversaciones del usuario")
    void testGetUserConversations_Success() throws Exception {
        // Given
        Long userId = 100L;
        List<ConversationSummaryDTO> summaries = Arrays.asList(conversationSummaryDTO);
        when(messagingService.getConversationSummaries(userId)).thenReturn(summaries);

        // When & Then
        mockMvc.perform(get("/api/users/{userId}/conversations", userId)
                .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].unreadCount").value(3));

        verify(messagingService, times(1)).getConversationSummaries(userId);
    }

    @Test
    @DisplayName("getUserConversations - Usuario no autorizado")
    void testGetUserConversations_Unauthorized() throws Exception {
        // Given
        Long userId = 100L;
        Long authenticatedUserId = 999L;

        // When & Then
        mockMvc.perform(get("/api/users/{userId}/conversations", userId)
                .header("X-User-Id", authenticatedUserId))
                .andExpect(status().isForbidden());

        verify(messagingService, never()).getConversationSummaries(anyLong());
    }

    @Test
    @DisplayName("getConversationDetails - Obtener detalles de conversación")
    void testGetConversationDetails_Success() throws Exception {
        // Given
        Long conversationId = 1L;
        Long authenticatedUserId = 100L;
        when(messagingService.getConversationDetails(conversationId, authenticatedUserId))
                .thenReturn(conversationDetailDTO);

        // When & Then
        mockMvc.perform(get("/api/conversations/{conversationId}", conversationId)
                .header("X-User-Id", authenticatedUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.messages[0].content").value("Test message"));

        verify(messagingService, times(1)).getConversationDetails(conversationId, authenticatedUserId);
    }

    @Test
    @DisplayName("getUserContacts - Obtener lista de contactos")
    void testGetUserContacts_Success() throws Exception {
        // Given
        Long userId = 100L;
        List<UsuarioDTO> contacts = Arrays.asList(usuarioDTO);
        when(userApiClient.getContactsForUser(userId)).thenReturn(contacts);

        // When & Then
        mockMvc.perform(get("/api/users/{userId}/contacts", userId)
                .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(200))
                .andExpect(jsonPath("$[0].nombre").value("Usuario Test"));

        verify(userApiClient, times(1)).getContactsForUser(userId);
    }

    @Test
    @DisplayName("getUserContacts - Usuario no autorizado")
    void testGetUserContacts_Unauthorized() throws Exception {
        // Given
        Long userId = 100L;
        Long authenticatedUserId = 999L;

        // When & Then
        mockMvc.perform(get("/api/users/{userId}/contacts", userId)
                .header("X-User-Id", authenticatedUserId))
                .andExpect(status().isForbidden());

        verify(userApiClient, never()).getContactsForUser(anyLong());
    }

    @Test
    @DisplayName("createOrGetConversation - Crear nueva conversación")
    void testCreateOrGetConversation_Success() throws Exception {
        // Given
        Long authenticatedUserId = 100L;
        when(messagingService.createOrGetConversation(100L, 200L)).thenReturn(conversationSummaryDTO);

        String requestBody = "{\"senderId\": 100, \"receiverId\": 200}";

        // When & Then
        mockMvc.perform(post("/api/conversations")
                .header("X-User-Id", authenticatedUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.unreadCount").value(3));

        verify(messagingService, times(1)).createOrGetConversation(100L, 200L);
    }

    @Test
    @DisplayName("createOrGetConversation - Sender no autorizado")
    void testCreateOrGetConversation_UnauthorizedSender() throws Exception {
        // Given
        Long authenticatedUserId = 999L;
        String requestBody = "{\"senderId\": 100, \"receiverId\": 200}";

        // When & Then
        mockMvc.perform(post("/api/conversations")
                .header("X-User-Id", authenticatedUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isForbidden());

        verify(messagingService, never()).createOrGetConversation(anyLong(), anyLong());
    }

    @Test
    @DisplayName("getUserConversations - Lista vacía")
    void testGetUserConversations_EmptyList() throws Exception {
        // Given
        Long userId = 100L;
        when(messagingService.getConversationSummaries(userId)).thenReturn(Arrays.asList());

        // When & Then
        mockMvc.perform(get("/api/users/{userId}/conversations", userId)
                .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(messagingService, times(1)).getConversationSummaries(userId);
    }

    @Test
    @DisplayName("sendMessage - Contenido vacío")
    void testSendMessage_EmptyContent() throws Exception {
        // Given
        Long authenticatedUserId = 100L;
        ChatMessageDTO emptyMessage = ChatMessageDTO.builder()
                .senderId(100L)
                .receiverId(200L)
                .content("")
                .build();

        when(messagingService.sendMessage(any(ChatMessageDTO.class))).thenReturn(emptyMessage);

        // When & Then
        mockMvc.perform(post("/api/messages")
                .header("X-User-Id", authenticatedUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyMessage)))
                .andExpect(status().isCreated());

        verify(messagingService, times(1)).sendMessage(any(ChatMessageDTO.class));
    }

    @Test
    @DisplayName("createOrGetConversation - Mismo senderId y receiverId")
    void testCreateOrGetConversation_SameSenderAndReceiver() throws Exception {
        // Given
        Long authenticatedUserId = 100L;
        String requestBody = "{\"senderId\": 100, \"receiverId\": 100}";

        when(messagingService.createOrGetConversation(100L, 100L)).thenReturn(conversationSummaryDTO);

        // When & Then
        mockMvc.perform(post("/api/conversations")
                .header("X-User-Id", authenticatedUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk());

        verify(messagingService, times(1)).createOrGetConversation(100L, 100L);
    }
}
