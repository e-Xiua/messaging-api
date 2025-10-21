package com.iwellness.messaging.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @NotNull(message = "El ID del remitente es obligatorio")
    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @NotNull(message = "El ID del destinatario es obligatorio")
    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @NotNull(message = "El contenido del mensaje es obligatorio")
    @Size(max = 5000, message = "El mensaje no puede exceder 5000 caracteres")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "sent_at", nullable = false, updatable = false)
    private LocalDateTime sentAt;

    @PrePersist
    protected void onCreate() {
        sentAt = LocalDateTime.now();
    }

    /**
     * Marca el mensaje como leído
     */
    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
    }
}
