package com.manifestreader.user.messaging;

import com.manifestreader.user.config.BillParseMessagingConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class RabbitTemplateExportTaskPublisher implements TemplateExportTaskPublisher {

    private final RabbitTemplate rabbitTemplate;

    public RabbitTemplateExportTaskPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publish(TemplateExportTaskMessage message) {
        rabbitTemplate.convertAndSend(
                BillParseMessagingConfig.TEMPLATE_EXPORT_EXCHANGE,
                BillParseMessagingConfig.TEMPLATE_EXPORT_ROUTING_KEY,
                message
        );
    }
}
