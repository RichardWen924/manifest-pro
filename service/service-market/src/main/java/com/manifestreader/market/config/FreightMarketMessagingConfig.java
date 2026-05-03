package com.manifestreader.market.config;

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
public class FreightMarketMessagingConfig {

    public static final String FREIGHT_DEMAND_ACCEPTED_QUEUE = "freight.demand.accepted.queue";
    public static final String FREIGHT_DEMAND_ACCEPTED_EXCHANGE = "freight.demand.accepted.exchange";
    public static final String FREIGHT_DEMAND_ACCEPTED_ROUTING_KEY = "freight.demand.accepted.route";

    @Bean
    public Queue freightDemandAcceptedQueue() {
        return new Queue(FREIGHT_DEMAND_ACCEPTED_QUEUE, true);
    }

    @Bean
    public DirectExchange freightDemandAcceptedExchange() {
        return new DirectExchange(FREIGHT_DEMAND_ACCEPTED_EXCHANGE, true, false);
    }

    @Bean
    public Binding freightDemandAcceptedBinding() {
        return BindingBuilder.bind(freightDemandAcceptedQueue())
                .to(freightDemandAcceptedExchange())
                .with(FREIGHT_DEMAND_ACCEPTED_ROUTING_KEY);
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
