package com.manifestreader.user.messaging;

import com.manifestreader.user.config.BillParseMessagingConfig;
import com.manifestreader.user.service.BillParseTaskService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class BillParseTaskConsumer {

    private final BillParseTaskService billParseTaskService;

    public BillParseTaskConsumer(BillParseTaskService billParseTaskService) {
        this.billParseTaskService = billParseTaskService;
    }

    @RabbitListener(
            queues = BillParseMessagingConfig.BILL_PARSE_QUEUE,
            autoStartup = "${manifest.messaging.bill-parse.listener-enabled:true}"
    )
    public void consume(BillParseTaskMessage message) {
        billParseTaskService.processTask(message);
    }
}
