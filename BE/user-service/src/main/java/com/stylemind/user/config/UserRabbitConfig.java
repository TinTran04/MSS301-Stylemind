package com.stylemind.user.config;

import org.aopalliance.aop.Advice;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserRabbitConfig {

    @Bean
    MessageConverter rabbitJsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

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

    @Bean
    Advice userRegisteredRetryAdvice(
            RabbitTemplate rabbitTemplate,
            @Value("${app.rabbitmq.dlx:stylemind.events.dlx}") String deadLetterExchange,
            @Value("${app.rabbitmq.user-registered.dlq-routing-key:user.registered.dlq}") String deadLetterRoutingKey,
            @Value("${user-profile.events.consumer.retry-attempts:3}") int maxAttempts,
            @Value("${user-profile.events.consumer.retry-initial-interval-ms:1000}") long initialInterval,
            @Value("${user-profile.events.consumer.retry-multiplier:2.0}") double multiplier,
            @Value("${user-profile.events.consumer.retry-max-interval-ms:10000}") long maxInterval) {
        return RetryInterceptorBuilder.stateless()
                .maxAttempts(maxAttempts)
                .backOffOptions(initialInterval, multiplier, maxInterval)
                .recoverer(new RepublishMessageRecoverer(rabbitTemplate, deadLetterExchange, deadLetterRoutingKey))
                .build();
    }

    @Bean
    SimpleRabbitListenerContainerFactory userRegisteredRabbitListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory,
            Advice userRegisteredRetryAdvice) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setAdviceChain(userRegisteredRetryAdvice);
        return factory;
    }
}
