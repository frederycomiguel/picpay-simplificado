package com.picpaysimplificado.infra.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String NOTIFICATION_QUEUE = "notification.queue";
    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";
    public static final String NOTIFICATION_ROUTING_KEY = "notification.routing.key";

    // Dead letter queue for failed notifications
    public static final String NOTIFICATION_DLQ = "notification.dlq";
    public static final String NOTIFICATION_DLX = "notification.dlx";

    /**
     * [PT-BR] Cria e configura a fila principal de notificações com suporte a Dead Letter Exchange (DLX).
     * [EN]    Creates and configures the primary notification queue with Dead Letter Exchange (DLX) support.
     *
     * @return Instância da Queue / Queue instance
     */
    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", NOTIFICATION_DLX)
                .withArgument("x-dead-letter-routing-key", NOTIFICATION_DLQ)
                .build();
    }

    /**
     * [PT-BR] Cria a Direct Exchange para roteamento das mensagens de notificação.
     * [EN]    Creates the Direct Exchange for routing notification messages.
     *
     * @return Instância da DirectExchange / DirectExchange instance
     */
    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange(NOTIFICATION_EXCHANGE);
    }

    /**
     * [PT-BR] Vincula a fila de notificação à exchange utilizando a chave de roteamento (routing key).
     * [EN]    Binds the notification queue to the exchange using the routing key.
     *
     * @param notificationQueue Fila principal / Main queue
     * @param notificationExchange Exchange de notificação / Notification exchange
     * @return Binding configurado / Configured binding
     */
    @Bean
    public Binding notificationBinding(Queue notificationQueue, DirectExchange notificationExchange) {
        return BindingBuilder.bind(notificationQueue)
                .to(notificationExchange)
                .with(NOTIFICATION_ROUTING_KEY);
    }

    /**
     * [PT-BR] Cria a Dead Letter Queue (DLQ) para armazenar mensagens que excederam as tentativas de reprocessamento.
     * [EN]    Creates the Dead Letter Queue (DLQ) to store messages that exceeded retry attempts.
     *
     * @return Instância da Queue DLQ / DLQ Queue instance
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(NOTIFICATION_DLQ).build();
    }

    /**
     * [PT-BR] Cria a Dead Letter Exchange (DLX) para mensagens rejeitadas.
     * [EN]    Creates the Dead Letter Exchange (DLX) for rejected messages.
     *
     * @return Instância da DirectExchange DLX / DLX DirectExchange instance
     */
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(NOTIFICATION_DLX);
    }

    /**
     * [PT-BR] Vincula a DLQ à Dead Letter Exchange.
     * [EN]    Binds the DLQ to the Dead Letter Exchange.
     *
     * @param deadLetterQueue Fila DLQ / DLQ Queue
     * @param deadLetterExchange Exchange DLX / DLX Exchange
     * @return Binding da DLQ / DLQ binding
     */
    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue)
                .to(deadLetterExchange)
                .with(NOTIFICATION_DLQ);
    }

    /**
     * [PT-BR] Configura o conversor de mensagens para serialização/deserialização automática em JSON usando Jackson.
     * [EN]    Configures the message converter for automatic JSON serialization/deserialization using Jackson.
     *
     * @return Conversor Jackson2JsonMessageConverter / Jackson2JsonMessageConverter instance
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * [PT-BR] Configura o RabbitTemplate com suporte nativo à conversão de objetos Java para JSON.
     * [EN]    Configures RabbitTemplate with native support for converting Java objects to JSON.
     *
     * @param connectionFactory Fábrica de conexões do RabbitMQ / RabbitMQ connection factory
     * @param jsonMessageConverter Conversor JSON / JSON converter
     * @return Instância configurada do RabbitTemplate / Configured RabbitTemplate instance
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter jsonMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter);
        return rabbitTemplate;
    }
}
