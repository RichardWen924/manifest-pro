package com.manifestreader.user.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BillParseMessagingConfig {

    public static final String BILL_PARSE_QUEUE = "bill.parse.queue";
    public static final String BILL_PARSE_EXCHANGE = "bill.parse.exchange";
    public static final String BILL_PARSE_ROUTING_KEY = "bill.parse.route";

    @Bean
    public Queue billParseQueue() {
        return new Queue(BILL_PARSE_QUEUE, true);
    }

    @Bean
    public DirectExchange billParseExchange() {
        return new DirectExchange(BILL_PARSE_EXCHANGE, true, false);
    }

    @Bean
    public Binding billParseBinding() {
        return BindingBuilder.bind(billParseQueue()).to(billParseExchange()).with(BILL_PARSE_ROUTING_KEY);
    }

    @Bean
    public MessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }
}
