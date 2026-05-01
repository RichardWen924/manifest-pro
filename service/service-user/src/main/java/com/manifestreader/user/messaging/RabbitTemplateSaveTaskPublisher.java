package com.manifestreader.user.messaging;

import com.manifestreader.user.config.BillParseMessagingConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class RabbitTemplateSaveTaskPublisher implements TemplateSaveTaskPublisher {

    private final RabbitTemplate rabbitTemplate;

    public RabbitTemplateSaveTaskPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publish(TemplateSaveTaskMessage message) {
        rabbitTemplate.convertAndSend(
                BillParseMessagingConfig.TEMPLATE_SAVE_EXCHANGE,
                BillParseMessagingConfig.TEMPLATE_SAVE_ROUTING_KEY,
                message
        );
    }
}
