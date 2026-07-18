package dev.pavle.media.mediaservice.config;

import static dev.pavle.media.contracts.MessagingTopology.*;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfiguration {
  @Bean
  MessageConverter messageConverter() {
    return new JacksonJsonMessageConverter();
  }

  @Bean
  Declarables mediaTopology() {
    TopicExchange commandExchange = new TopicExchange(COMMAND_EXCHANGE, true, false);
    Queue commandQueue = new Queue(COMMAND_QUEUE, true);
    Binding commandBinding =
        BindingBuilder.bind(commandQueue).to(commandExchange).with(COMMAND_ROUTING_KEY);

    TopicExchange eventExchange = new TopicExchange(EVENT_EXCHANGE, true, false);
    Queue eventQueue = new Queue(EVENT_QUEUE, true);
    Binding eventBinding =
        BindingBuilder.bind(eventQueue).to(eventExchange).with("rendition.event.*");

    TopicExchange notificationExchange = new TopicExchange(NOTIFICATION_EXCHANGE, true, false);
    Queue notificationQueue = new Queue(NOTIFICATION_QUEUE, true);
    Binding notificationBinding =
        BindingBuilder.bind(notificationQueue)
            .to(notificationExchange)
            .with(NOTIFICATION_ROUTING_KEY);

    return new Declarables(
        commandExchange,
        commandQueue,
        commandBinding,
        eventExchange,
        eventQueue,
        eventBinding,
        notificationExchange,
        notificationQueue,
        notificationBinding);
  }
}
