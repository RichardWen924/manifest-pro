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
    public static final String TEMPLATE_EXPORT_QUEUE = "template.export.queue";
    public static final String TEMPLATE_EXPORT_EXCHANGE = "template.export.exchange";
    public static final String TEMPLATE_EXPORT_ROUTING_KEY = "template.export.route";
    public static final String TEMPLATE_EXTRACT_QUEUE = "template.extract.queue";
    public static final String TEMPLATE_EXTRACT_EXCHANGE = "template.extract.exchange";
    public static final String TEMPLATE_EXTRACT_ROUTING_KEY = "template.extract.route";

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
    public Queue templateExportQueue() {
        return new Queue(TEMPLATE_EXPORT_QUEUE, true);
    }

    @Bean
    public DirectExchange templateExportExchange() {
        return new DirectExchange(TEMPLATE_EXPORT_EXCHANGE, true, false);
    }

    @Bean
    public Binding templateExportBinding() {
        return BindingBuilder.bind(templateExportQueue()).to(templateExportExchange()).with(TEMPLATE_EXPORT_ROUTING_KEY);
    }

    @Bean
    public Queue templateExtractQueue() {
        return new Queue(TEMPLATE_EXTRACT_QUEUE, true);
    }

    @Bean
    public DirectExchange templateExtractExchange() {
        return new DirectExchange(TEMPLATE_EXTRACT_EXCHANGE, true, false);
    }

    @Bean
    public Binding templateExtractBinding() {
        return BindingBuilder.bind(templateExtractQueue()).to(templateExtractExchange()).with(TEMPLATE_EXTRACT_ROUTING_KEY);
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
