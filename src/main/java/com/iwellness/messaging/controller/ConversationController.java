package com.iwellness.messaging.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.iwellness.messaging.clientes.UserApiClient;
import com.iwellness.messaging.dto.ChatMessageDTO;
import com.iwellness.messaging.dto.ConversationDetailDTO;
import com.iwellness.messaging.dto.ConversationSummaryDTO;
import com.iwellness.messaging.dto.UsuarioDTO;
import com.iwellness.messaging.service.MessagingService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api") // Cambiamos el mapping base
public class ConversationController {

    private final MessagingService messagingService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserApiClient userApiClient;

    public ConversationController(MessagingService messagingService, SimpMessagingTemplate messagingTemplate, UserApiClient userApiClient) {

        this.messagingService = messagingService;
        this.messagingTemplate = messagingTemplate;
        this.userApiClient = userApiClient;
    }

    /**
     * Endpoint REST para enviar un nuevo mensaje.
     * Persiste el mensaje y luego lo difunde al destinatario a través de WebSocket.
     *
     * @param messageDTO El cuerpo del mensaje a enviar.
     * @param authenticatedUserId El ID del usuario que envía, para validación.
     * @return El DTO del mensaje guardado.
     */
    @PostMapping("/messages")
    public ResponseEntity<ChatMessageDTO> sendMessage(
            @RequestBody ChatMessageDTO messageDTO,
            @RequestHeader("X-User-Id") Long authenticatedUserId) {

                // --- CAPA DE SEGURIDAD ---
        if (!authenticatedUserId.equals(messageDTO.getSenderId())) {
            log.warn("Security alert: User {} tried to send a message as user {}", authenticatedUserId, messageDTO.getSenderId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only send messages as yourself.");
        }

        log.info("REST request to send message from user {} to {}", messageDTO.getSenderId(), messageDTO.getReceiverId());
        ChatMessageDTO savedMessage = messagingService.sendMessage(messageDTO);

        // --- DIFUSIÓN EN TIEMPO REAL ---
        // Envía el mensaje guardado a la cola privada del destinatario.
        // El cliente (frontend) debe estar suscrito a /user/queue/messages.
        log.debug("Broadcasting message {} to user {}", savedMessage.getId(), savedMessage.getReceiverId());
        messagingTemplate.convertAndSendToUser(
                            String.valueOf(savedMessage.getReceiverId()), // El ID del usuario destinatario
                "/queue/messages",                             // El destino privado
                savedMessage                                   // El objeto a enviar
        );

        return new ResponseEntity<>(savedMessage, HttpStatus.CREATED);
    }

    /**
     * Obtiene una lista de resúmenes de todas las conversaciones de un usuario.
     *
     * @param userId El ID del usuario del cual se solicitan las conversaciones.
     * @param authenticatedUserId El ID del usuario autenticado (inyectado desde el header).
     * @return Una lista de resúmenes de conversación.
     */
    @GetMapping("/users/{userId}/conversations")
    public ResponseEntity<List<ConversationSummaryDTO>> getConversationSummaries(
            @PathVariable Long userId,
            @RequestHeader("X-User-Id") Long authenticatedUserId) {

        // --- CAPA DE SEGURIDAD ---
        if (!userId.equals(authenticatedUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to access these resources."); 
        }

        log.info("Request received for conversation summaries for user ID: {}", userId);
        List<ConversationSummaryDTO> summaries = messagingService.getConversationSummaries(userId);
        return ResponseEntity.ok(summaries);
    }

    /**
     * Obtiene el detalle completo de una conversación específica.
     *
     * @param conversationId El ID de la conversación a obtener.
     * @param authenticatedUserId El ID del usuario autenticado, para validar permisos.
     * @return El detalle completo de la conversación.
     */
    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<ConversationDetailDTO> getConversationDetails(
            @PathVariable Long conversationId,
            @RequestHeader("X-User-Id") Long authenticatedUserId) {
        log.info("Request received for conversation details for ID: {}", conversationId);
        // La validación de seguridad ya está dentro del service, pero se podría duplicar aquí por claridad.
        ConversationDetailDTO details = messagingService.getConversationDetails(conversationId, authenticatedUserId);
        return ResponseEntity.ok(details);
    }

        /**
     * Obtiene la lista de contactos con los que un usuario puede hablar.
     * Delega la llamada al microservicio de usuarios a través de Feign.
     */
    @GetMapping("/users/{userId}/contacts")
    public ResponseEntity<List<UsuarioDTO>> getUserContacts(
            @PathVariable Long userId,
            
            @RequestHeader("X-User-Id") Long authenticatedUserId) {
        
        // --- CAPA DE SEGURIDAD ---
        if (!userId.equals(authenticatedUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only view your own contacts.");
        }

        log.info("Fetching contacts for user ID: {}", userId);
        // Llama al método del cliente Feign
        List<UsuarioDTO> contacts = userApiClient.getContactsForUser(userId);
        return ResponseEntity.ok(contacts);
    }

    /**
     * Crea una nueva conversación o recupera una existente entre dos usuarios.
     * Este endpoint es útil cuando un usuario quiere iniciar un chat con otro usuario
     * desde la lista de contactos.
     *
     * @param senderId El ID del usuario que inicia la conversación (del body).
     * @param receiverId El ID del usuario con quien se quiere hablar (del body).
     * @param authenticatedUserId El ID del usuario autenticado, para validar permisos.
     * @return El resumen de la conversación creada o encontrada.
     */
    @PostMapping("/conversations")
    public ResponseEntity<ConversationSummaryDTO> createOrGetConversation(
            @RequestBody CreateConversationRequest request,
            @RequestHeader("X-User-Id") Long authenticatedUserId) {
        
        // --- CAPA DE SEGURIDAD ---
        if (!authenticatedUserId.equals(request.getSenderId())) {
            log.warn("Security alert: User {} tried to create a conversation as user {}", 
                    authenticatedUserId, request.getSenderId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                    "You can only create conversations as yourself.");
        }

        log.info("Request to create/get conversation between user {} and user {}", 
                request.getSenderId(), request.getReceiverId());
        
        ConversationSummaryDTO conversationSummary = messagingService.createOrGetConversation(
                request.getSenderId(), request.getReceiverId());
        
        return ResponseEntity.ok(conversationSummary);
    }

    // Clase interna para el request body
    public static class CreateConversationRequest {
        private Long senderId;
        private Long receiverId;

        public Long getSenderId() {
            return senderId;
        }

        public void setSenderId(Long senderId) {
            this.senderId = senderId;
        }

        public Long getReceiverId() {
            return receiverId;
        }

        public void setReceiverId(Long receiverId) {
            this.receiverId = receiverId;
        }
    }
}
