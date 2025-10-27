package com.iwellness.messaging.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.iwellness.messaging.clientes.UserApiClient;
import com.iwellness.messaging.dto.ChatMessageDTO;
import com.iwellness.messaging.dto.ConversationDetailDTO;
import com.iwellness.messaging.dto.ConversationSummaryDTO;
import com.iwellness.messaging.dto.UsuarioDTO;
import com.iwellness.messaging.entity.Conversation;
import com.iwellness.messaging.entity.Message;
import com.iwellness.messaging.repository.ConversationRepository;
import com.iwellness.messaging.repository.MessageRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MessagingService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserApiClient userApiClient;

    public MessagingService(ConversationRepository conversationRepository,
                            MessageRepository messageRepository,
                            UserApiClient userApiClient) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userApiClient = userApiClient;
    }

    /**
     * Procesa y guarda un nuevo mensaje.
     * Busca una conversación existente entre el emisor y el receptor.
     * Si no existe, crea una nueva.
     *
     * @param messageDTO DTO con la información del mensaje a enviar.
     * @return El DTO del mensaje guardado.
     */
    @Transactional
    public ChatMessageDTO sendMessage(ChatMessageDTO messageDTO) {
        log.info("Sending message from user {} to user {}", messageDTO.getSenderId(), messageDTO.getReceiverId());

        // Usa el método findByUsers para encontrar o crear la conversación
        Conversation conversation = conversationRepository
                .findByUsers(messageDTO.getSenderId(), messageDTO.getReceiverId())
                .orElseGet(() -> {
                    log.info("No existing conversation found. Creating a new one.");
                    Conversation newConversation = new Conversation();
                    newConversation.setUser1Id(messageDTO.getSenderId());
                    newConversation.setUser2Id(messageDTO.getReceiverId());
                    return conversationRepository.save(newConversation);
                });

        // Actualiza la fecha de la última actividad en la conversación
        conversation.setUpdatedAt(LocalDateTime.now());
        conversationRepository.save(conversation);

        // Crea y guarda la nueva entidad de mensaje
        Message message = new Message();
        message.setConversation(conversation);
        message.setSenderId(messageDTO.getSenderId());
        message.setReceiverId(messageDTO.getReceiverId());
        message.setContent(messageDTO.getContent());
        message.setIsRead(false); // Un nuevo mensaje nunca está leído

        Message savedMessage = messageRepository.save(message);
        log.info("Message saved with ID: {}", savedMessage.getId());

        return mapToChatMessageDTO(savedMessage);
    }


    @Transactional(readOnly = true)
    public List<ConversationSummaryDTO> getConversationSummaries(Long userId) {
        log.info("Fetching conversation summaries for user ID: {}", userId);
        // ✅ CORRECCIÓN: Usando el método findByParticipant del repositorio
        List<Conversation> conversations = conversationRepository.findByParticipant(userId);

        return conversations.stream()
                .map(conversation -> mapToSummaryDTO(conversation, userId))
                .collect(Collectors.toList());
    }

    /**
     * Crea una nueva conversación entre dos usuarios o recupera una existente.
     * Útil cuando un usuario quiere iniciar un chat desde la lista de contactos.
     *
     * @param senderId El ID del usuario que inicia la conversación.
     * @param receiverId El ID del usuario con quien se quiere hablar.
     * @return El resumen de la conversación creada o encontrada.
     */
    @Transactional
    public ConversationSummaryDTO createOrGetConversation(Long senderId, Long receiverId) {
        log.info("Creating or getting conversation between user {} and user {}", senderId, receiverId);

        // Busca una conversación existente entre los dos usuarios
        Conversation conversation = conversationRepository
                .findByUsers(senderId, receiverId)
                .orElseGet(() -> {
                    log.info("No existing conversation found. Creating a new one.");
                    Conversation newConversation = new Conversation();
                    newConversation.setUser1Id(senderId);
                    newConversation.setUser2Id(receiverId);
                    newConversation.setUpdatedAt(LocalDateTime.now());
                    newConversation.setCreatedAt(LocalDateTime.now());
                    newConversation.setMessages(List.of());
                    return conversationRepository.save(newConversation);
                });

        // Si la conversación ya existía, actualizamos su timestamp
        if (conversation.getId() != null) {
            conversation.setUpdatedAt(LocalDateTime.now());
            conversationRepository.save(conversation);
        }

        // Retorna el resumen de la conversación para que el frontend pueda mostrarla
        return mapToSummaryDTO(conversation, senderId);
    }

    @Transactional(readOnly = true)
    public ConversationDetailDTO getConversationDetails(Long conversationId, Long userId) {
        log.info("Fetching details for conversation ID: {} for user ID: {}", conversationId, userId);
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found with id: " + conversationId));

        if (!conversation.getUser1Id().equals(userId) && !conversation.getUser2Id().equals(userId)) {
            throw new SecurityException("User is not a participant of this conversation.");
        }

        log.debug("Fetching participant info from admin-users-service");
        UsuarioDTO participant1 = userApiClient.findById(conversation.getUser1Id());
        UsuarioDTO participant2 = userApiClient.findById(conversation.getUser2Id());

        List<Message> messages = messageRepository.findByConversationIdOrderBySentAtAsc(conversationId);
        List<ChatMessageDTO> messageDTOs = messages.stream()
                .map(this::mapToChatMessageDTO)
                .collect(Collectors.toList());

        return ConversationDetailDTO.builder()
                .id(conversation.getId())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .participant1(participant1)
                .participant2(participant2)
                .messages(messageDTOs)
                .build();
    }

        /**
     * Marca un mensaje específico como leído por un usuario.
     *
     * @param messageId El ID del mensaje a marcar.
     * @param userId El ID del usuario que está leyendo el mensaje (debe ser el destinatario).
     * @return El DTO del mensaje actualizado.
     */
        @Transactional
    public ChatMessageDTO markMessageAsRead(Long messageId, Long userId) {
        log.info("Attempting to mark message {} as read by user {}", messageId, userId);
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found with id: " + messageId));

        // --- CAPA DE SEGURIDAD ---
        // Solo el destinatario del mensaje puede marcarlo como leído.
        if (!message.getReceiverId().equals(userId)) {
            log.warn("Security alert: User {} tried to mark a message not addressed to them.", userId);
            throw new SecurityException("You can only mark messages addressed to you as read.");
        }

                // Si ya está leído, no hacemos nada para evitar escrituras innecesarias en la BD.
        if (message.getIsRead()) {
            log.info("Message {} was already marked as read.", messageId);
            return mapToChatMessageDTO(message);
        }

        message.setIsRead(true);
        message.setReadAt(LocalDateTime.now());
        Message updatedMessage = messageRepository.save(message);

        log.info("Message {} successfully marked as read.", messageId);
        return mapToChatMessageDTO(updatedMessage);
    }

        /**
     * Obtiene un único mensaje por su ID.
     *
     * @param messageId El ID del mensaje.
     * @return El DTO del mensaje.
     */
    @Transactional(readOnly = true)
    public ChatMessageDTO getMessageById(Long messageId) {
        return messageRepository.findById(messageId)
                .map(this::mapToChatMessageDTO)
                .orElseThrow(() -> new RuntimeException("Message not found with id: " + messageId));
    }

    // --- Métodos de Ayuda (Helpers) ---

    private ConversationSummaryDTO mapToSummaryDTO(Conversation conversation, Long currentUserId) {
        Long otherParticipantId = conversation.getUser1Id().equals(currentUserId)
                ? conversation.getUser2Id()
                : conversation.getUser1Id();

        UsuarioDTO otherParticipantInfo = userApiClient.findById(otherParticipantId);

        Message lastMessage = messageRepository.findTopByConversationIdOrderBySentAtDesc(conversation.getId())
                .orElse(null);

        long unreadCount = messageRepository.countByConversationIdAndReceiverIdAndIsReadIsFalse(conversation.getId(), currentUserId);

        return ConversationSummaryDTO.builder()
                .id(conversation.getId())
                .lastMessageAt(lastMessage != null ? lastMessage.getSentAt() : conversation.getUpdatedAt())
                .otherParticipant(otherParticipantInfo)
                .lastMessage(lastMessage != null ? mapToChatMessageDTO(lastMessage) : null)
                .unreadCount(unreadCount)
                .build();
    }

    private ChatMessageDTO mapToChatMessageDTO(Message message) {
        return ChatMessageDTO.builder()
                .id(message.getId())
                .conversationId(message.getConversation().getId())
                .senderId(message.getSenderId())
                .receiverId(message.getReceiverId())
                .content(message.getContent())
                .isRead(message.getIsRead())
                .readAt(message.getReadAt())
                .sentAt(message.getSentAt())
                .build();
    }
}


