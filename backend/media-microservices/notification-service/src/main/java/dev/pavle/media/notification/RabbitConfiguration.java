package dev.pavle.media.notification;

import static dev.pavle.media.contracts.MessagingTopology.*;

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
  Declarables notificationTopology() {
    TopicExchange exchange = new TopicExchange(NOTIFICATION_EXCHANGE, true, false);
    Queue queue = new Queue(NOTIFICATION_QUEUE, true);
    return new Declarables(
        exchange, queue, BindingBuilder.bind(queue).to(exchange).with(NOTIFICATION_ROUTING_KEY));
  }
}
