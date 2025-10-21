package com.iwellness.messaging.repository;

import com.iwellness.messaging.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * Busca todos los mensajes de una conversación específica, ordenados por fecha de envío ascendente.
     * Utilizado en `getConversationDetails` para obtener el historial completo del chat.
     *
     * @param conversationId El ID de la conversación.
     * @return Una lista de mensajes ordenados.
     */
    List<Message> findByConversationIdOrderBySentAtAsc(Long conversationId);

    /**
     * Busca el último mensaje enviado en una conversación.
     * Utilizado en `mapToSummaryDTO` para mostrar un avance del último mensaje en la lista de chats.
     *
     * @param conversationId El ID de la conversación.
     * @return Un Optional que contiene el último mensaje, o vacío si no hay mensajes.
     */
    Optional<Message> findTopByConversationIdOrderBySentAtDesc(Long conversationId);

    /**
     * Cuenta el número de mensajes no leídos para un destinatario específico en una conversación.
     * Utilizado en `mapToSummaryDTO` para mostrar la "burbuja" de notificaciones.
     *
     * @param conversationId El ID de la conversación.
     * @param receiverId El ID del usuario que es el destinatario.
     * @return El número de mensajes no leídos.
     */
    long countByConversationIdAndReceiverIdAndIsReadIsFalse(Long conversationId, Long receiverId);

}
