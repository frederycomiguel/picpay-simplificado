package com.picpaysimplificado.infra.consumer;

import com.picpaysimplificado.dto.NotificationPayload;
import com.picpaysimplificado.infra.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@Slf4j
public class NotificationConsumer {

    private final RestClient restClient;
    private final String notificationUrl;

    /**
     * [PT-BR] Construtor que injeta o RestClient e a URL do serviço de notificação mockado.
     * [EN]    Constructor injecting the RestClient and the mocked notification service URL.
     *
     * @param restClientBuilder Builder do Spring RestClient / Spring RestClient builder
     * @param notificationUrl URL do serviço de notificação / Notification service URL
     */
    public NotificationConsumer(
            RestClient.Builder restClientBuilder,
            @Value("${picpay.services.notification-url}") String notificationUrl) {
        this.restClient = restClientBuilder.build();
        this.notificationUrl = notificationUrl;
    }

    /**
     * [PT-BR] Consome mensagens da fila RabbitMQ 'notification.queue' e faz requisição HTTP POST para notificar o recebedor.
     *         Em caso de falhas temporárias na API externa, o RabbitMQ reprocessa via mecanismo de retry.
     * [EN]    Consumes messages from RabbitMQ 'notification.queue' and performs an HTTP POST request to notify the receiver.
     *         In case of temporary external API failures, RabbitMQ reprocesses the message via retry mechanism.
     *
     * @param payload Payload da notificação recebido da fila / Notification payload received from queue
     */
    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void consumeNotification(NotificationPayload payload) {
        log.info("Processando notificação da fila - Transação: {}, Destinatário: {}",
                payload.transactionId(), payload.receiverEmail());

        try {
            var response = restClient.post()
                    .uri(notificationUrl)
                    .body(Map.of(
                            "email", payload.receiverEmail(),
                            "message", String.format(
                                    "Você recebeu uma transferência de R$ %s de %s",
                                    payload.amount(), payload.senderName())
                    ))
                    .retrieve()
                    .toEntity(Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Notificação enviada com sucesso para: {}", payload.receiverEmail());
            } else {
                log.warn("Serviço de notificação retornou status: {}", response.getStatusCode());
                throw new RuntimeException("Falha ao enviar notificação - Status: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Erro ao enviar notificação para {}: {}", payload.receiverEmail(), e.getMessage());
            throw new RuntimeException("Falha ao processar notificação", e);
        }
    }
}
