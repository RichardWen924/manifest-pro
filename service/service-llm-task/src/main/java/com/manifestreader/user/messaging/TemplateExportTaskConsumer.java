package com.manifestreader.user.messaging;

import com.manifestreader.user.config.BillParseMessagingConfig;
import com.manifestreader.user.service.TemplateExportTaskService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class TemplateExportTaskConsumer {

    private final TemplateExportTaskService taskService;

    public TemplateExportTaskConsumer(TemplateExportTaskService taskService) {
        this.taskService = taskService;
    }

    @RabbitListener(
            queues = BillParseMessagingConfig.TEMPLATE_EXPORT_QUEUE,
            autoStartup = "${manifest.messaging.template-export.listener-enabled:true}"
    )
    public void consume(TemplateExportTaskMessage message) {
        taskService.processTask(message);
    }
}
