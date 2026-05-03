package com.manifestreader.market.messaging;

import com.manifestreader.market.config.FreightMarketMessagingConfig;
import com.manifestreader.market.mapper.FreightOrderTimelineMapper;
import com.manifestreader.model.entity.FreightOrderTimelineEntity;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "manifest.messaging.market-demand-accepted",
        name = "listener-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class FreightDemandAcceptedConsumer {

    private final FreightOrderTimelineMapper timelineMapper;

    public FreightDemandAcceptedConsumer(FreightOrderTimelineMapper timelineMapper) {
        this.timelineMapper = timelineMapper;
    }

    @RabbitListener(
            queues = FreightMarketMessagingConfig.FREIGHT_DEMAND_ACCEPTED_QUEUE,
            autoStartup = "${spring.rabbitmq.listener.simple.auto-startup:true}"
    )
    public void consume(FreightDemandAcceptedMessage message) {
        FreightOrderTimelineEntity entity = new FreightOrderTimelineEntity();
        entity.setOrderId(message.getOrderId());
        entity.setEventType("ORDER_CREATED");
        entity.setEventMessage("货运订单已创建，等待代理履约");
        entity.setOperatorUserId(message.getOperatorUserId());
        timelineMapper.insert(entity);
    }
}
