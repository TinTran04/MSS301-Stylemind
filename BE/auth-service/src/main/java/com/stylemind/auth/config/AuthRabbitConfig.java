package com.stylemind.auth.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthRabbitConfig {

    @Bean
    DirectExchange stylemindEventsExchange(
            @Value("${app.rabbitmq.exchange:stylemind.events}") String exchangeName) {
        return new DirectExchange(exchangeName, true, false);
    }

    @Bean
    DirectExchange stylemindEventsDeadLetterExchange(
            @Value("${app.rabbitmq.dlx:stylemind.events.dlx}") String exchangeName) {
        return new DirectExchange(exchangeName, true, false);
    }

    @Bean
    Queue userRegisteredQueue(
            @Value("${app.rabbitmq.user-registered.queue:user-profile.user-registered}") String queueName,
            @Value("${app.rabbitmq.dlx:stylemind.events.dlx}") String deadLetterExchange,
            @Value("${app.rabbitmq.user-registered.dlq-routing-key:user.registered.dlq}") String deadLetterRoutingKey) {
        return QueueBuilder.durable(queueName)
                .withArgument("x-dead-letter-exchange", deadLetterExchange)
                .withArgument("x-dead-letter-routing-key", deadLetterRoutingKey)
                .build();
    }

    @Bean
    Queue userRegisteredDeadLetterQueue(
            @Value("${app.rabbitmq.user-registered.dlq:user-profile.user-registered.dlq}") String queueName) {
        return QueueBuilder.durable(queueName).build();
    }

    @Bean
    Binding userRegisteredBinding(
            Queue userRegisteredQueue,
            DirectExchange stylemindEventsExchange,
            @Value("${app.rabbitmq.user-registered.routing-key:user.registered}") String routingKey) {
        return BindingBuilder.bind(userRegisteredQueue).to(stylemindEventsExchange).with(routingKey);
    }

    @Bean
    Binding userRegisteredDeadLetterBinding(
            Queue userRegisteredDeadLetterQueue,
            DirectExchange stylemindEventsDeadLetterExchange,
            @Value("${app.rabbitmq.user-registered.dlq-routing-key:user.registered.dlq}") String routingKey) {
        return BindingBuilder.bind(userRegisteredDeadLetterQueue).to(stylemindEventsDeadLetterExchange).with(routingKey);
    }
}
