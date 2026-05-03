package com.manifestreader.market.messaging;

import com.manifestreader.market.config.FreightMarketMessagingConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class RabbitFreightDemandAcceptedPublisher implements FreightDemandAcceptedPublisher {

    private final RabbitTemplate rabbitTemplate;

    public RabbitFreightDemandAcceptedPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publish(FreightDemandAcceptedMessage message) {
        rabbitTemplate.convertAndSend(
                FreightMarketMessagingConfig.FREIGHT_DEMAND_ACCEPTED_EXCHANGE,
                FreightMarketMessagingConfig.FREIGHT_DEMAND_ACCEPTED_ROUTING_KEY,
                message
        );
    }
}
