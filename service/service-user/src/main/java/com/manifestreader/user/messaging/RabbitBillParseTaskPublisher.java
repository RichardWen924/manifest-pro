package com.manifestreader.user.messaging;

import com.manifestreader.user.config.BillParseMessagingConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class RabbitBillParseTaskPublisher implements BillParseTaskPublisher {

    private final RabbitTemplate rabbitTemplate;

    public RabbitBillParseTaskPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publish(BillParseTaskMessage message) {
        rabbitTemplate.convertAndSend(
                BillParseMessagingConfig.BILL_PARSE_EXCHANGE,
                BillParseMessagingConfig.BILL_PARSE_ROUTING_KEY,
                message
        );
    }
}
