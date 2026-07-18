package dev.pavle.media.transcoder;

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
  Declarables transcoderTopology() {
    TopicExchange commandExchange = new TopicExchange(COMMAND_EXCHANGE, true, false);
    Queue commandQueue = new Queue(COMMAND_QUEUE, true);
    TopicExchange eventExchange = new TopicExchange(EVENT_EXCHANGE, true, false);
    return new Declarables(
        commandExchange,
        commandQueue,
        BindingBuilder.bind(commandQueue).to(commandExchange).with(COMMAND_ROUTING_KEY),
        eventExchange);
  }
}
