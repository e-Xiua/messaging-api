package com.iwellness.messaging.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDTO {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private Long receiverId;
    private String content;
    private Boolean isRead;
    private LocalDateTime readAt;
    private LocalDateTime sentAt;
}
