package com.manifestreader.user.messaging;

import com.manifestreader.user.config.BillParseMessagingConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class RabbitTemplateExtractTaskPublisher implements TemplateExtractTaskPublisher {

    private final RabbitTemplate rabbitTemplate;

    public RabbitTemplateExtractTaskPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publish(TemplateExtractTaskMessage message) {
        rabbitTemplate.convertAndSend(
                BillParseMessagingConfig.TEMPLATE_EXTRACT_EXCHANGE,
                BillParseMessagingConfig.TEMPLATE_EXTRACT_ROUTING_KEY,
                message
        );
    }
}
