package com.iwellness.messaging.publisher;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.iwellness.messaging.config.AppProperties;
import com.iwellness.messaging.dto.ChatMessageDTO;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MessageEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final AppProperties appProperties;

    public MessageEventPublisher(RabbitTemplate rabbitTemplate, AppProperties appProperties) {
        this.rabbitTemplate = rabbitTemplate;
        this.appProperties = appProperties;
    }

    public void publishMessageSent(ChatMessageDTO message) {
        try {
            String exchangeName = appProperties.getRabbitmq().getExchange().getName();
            String routingKey = appProperties.getRabbitmq().getRoutingKeys().get("message-sent");
            
            rabbitTemplate.convertAndSend(exchangeName, routingKey, message);
            log.info("Evento publicado: MessageSent - Message ID: {}", message.getId());
        } catch (Exception e) {
            log.error("Error al publicar evento MessageSent: {}", e.getMessage(), e);
        }
    }

    public void publishMessageRead(Long messageId, Long userId) {
        try {
            String exchangeName = appProperties.getRabbitmq().getExchange().getName();
            String routingKey = appProperties.getRabbitmq().getRoutingKeys().get("message-read");

            rabbitTemplate.convertAndSend(exchangeName, routingKey, messageId);
            log.info("Evento publicado: MessageRead - Message ID: {}", messageId);
        } catch (Exception e) {
            log.error("Error al publicar evento MessageRead: {}", e.getMessage(), e);
        }
    }
}
