package com.iwellness.messaging.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ConversationSummaryDTO {
    private Long id;
    private LocalDateTime lastMessageAt;
    private UsuarioDTO otherParticipant;
    private ChatMessageDTO lastMessage;
    private long unreadCount;
}
