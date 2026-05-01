package com.manifestreader.user.messaging;

import com.manifestreader.user.config.BillParseMessagingConfig;
import com.manifestreader.user.service.TemplateSaveTaskService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "manifest.messaging.template-save", name = "listener-enabled", havingValue = "true", matchIfMissing = true)
public class TemplateSaveTaskConsumer {

    private final TemplateSaveTaskService templateSaveTaskService;

    public TemplateSaveTaskConsumer(TemplateSaveTaskService templateSaveTaskService) {
        this.templateSaveTaskService = templateSaveTaskService;
    }

    @RabbitListener(
            queues = BillParseMessagingConfig.TEMPLATE_SAVE_QUEUE,
            autoStartup = "${spring.rabbitmq.listener.simple.auto-startup:true}"
    )
    public void consume(TemplateSaveTaskMessage message) {
        templateSaveTaskService.processTask(message);
    }
}
