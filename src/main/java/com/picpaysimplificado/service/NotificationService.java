package com.picpaysimplificado.service;

import com.picpaysimplificado.dto.NotificationPayload;
import com.picpaysimplificado.infra.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publishes a notification event to the RabbitMQ queue for async processing.
     * This decouples notification delivery from the transfer transaction.
     */
    public void sendNotification(NotificationPayload payload) {
        log.info("Publicando notificação na fila para o usuário: {} (transação: {})",
                payload.receiverId(), payload.transactionId());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.NOTIFICATION_EXCHANGE,
                RabbitMQConfig.NOTIFICATION_ROUTING_KEY,
                payload
        );

        log.info("Notificação publicada com sucesso na fila: {}", RabbitMQConfig.NOTIFICATION_QUEUE);
    }
}
