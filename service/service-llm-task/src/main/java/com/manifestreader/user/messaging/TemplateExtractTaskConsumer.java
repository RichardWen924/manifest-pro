package com.manifestreader.user.messaging;

import com.manifestreader.user.config.BillParseMessagingConfig;
import com.manifestreader.user.service.TemplateExtractTaskService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class TemplateExtractTaskConsumer {

    private final TemplateExtractTaskService taskService;

    public TemplateExtractTaskConsumer(TemplateExtractTaskService taskService) {
        this.taskService = taskService;
    }

    @RabbitListener(
            queues = BillParseMessagingConfig.TEMPLATE_EXTRACT_QUEUE,
            autoStartup = "${manifest.messaging.template-extract.listener-enabled:true}"
    )
    public void consume(TemplateExtractTaskMessage message) {
        taskService.processTask(message);
    }
}
