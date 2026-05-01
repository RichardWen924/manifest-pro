package com.manifestreader.llmtask.config;

import com.manifestreader.user.cache.RedisBillParseTaskCacheService;
import com.manifestreader.user.config.BillParseMessagingConfig;
import com.manifestreader.user.config.DifyWorkflowConfig;
import com.manifestreader.user.config.UserMybatisMapperConfig;
import com.manifestreader.user.dify.DifyTemplateExportParser;
import com.manifestreader.user.dify.DifyTemplateMappingParser;
import com.manifestreader.user.dify.DifyWorkflowClientImpl;
import com.manifestreader.user.messaging.BillParseTaskConsumer;
import com.manifestreader.user.messaging.RabbitBillParseTaskPublisher;
import com.manifestreader.user.messaging.RabbitTemplateExportTaskPublisher;
import com.manifestreader.user.messaging.RabbitTemplateExtractTaskPublisher;
import com.manifestreader.user.messaging.RabbitTemplateSaveTaskPublisher;
import com.manifestreader.user.messaging.TemplateExportTaskConsumer;
import com.manifestreader.user.messaging.TemplateExtractTaskConsumer;
import com.manifestreader.user.messaging.TemplateSaveTaskConsumer;
import com.manifestreader.user.service.impl.BillParseTaskServiceImpl;
import com.manifestreader.user.service.impl.TemplateExportTaskServiceImpl;
import com.manifestreader.user.service.impl.TemplateExtractTaskServiceImpl;
import com.manifestreader.user.service.impl.TemplateSaveTaskServiceImpl;
import com.manifestreader.user.service.impl.UserTemplateServiceImpl;
import com.manifestreader.user.storage.LocalObjectStorageService;
import com.manifestreader.user.storage.MinioObjectStorageService;
import com.manifestreader.user.support.HeaderUserRequestContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({
        DifyWorkflowConfig.class,
        UserMybatisMapperConfig.class,
        BillParseMessagingConfig.class,
        DifyTemplateExportParser.class,
        DifyTemplateMappingParser.class,
        DifyWorkflowClientImpl.class,
        HeaderUserRequestContext.class,
        LocalObjectStorageService.class,
        MinioObjectStorageService.class,
        RedisBillParseTaskCacheService.class,
        RabbitBillParseTaskPublisher.class,
        RabbitTemplateExportTaskPublisher.class,
        RabbitTemplateExtractTaskPublisher.class,
        RabbitTemplateSaveTaskPublisher.class,
        BillParseTaskServiceImpl.class,
        TemplateExportTaskServiceImpl.class,
        TemplateExtractTaskServiceImpl.class,
        TemplateSaveTaskServiceImpl.class,
        UserTemplateServiceImpl.class,
        BillParseTaskConsumer.class,
        TemplateExportTaskConsumer.class,
        TemplateExtractTaskConsumer.class,
        TemplateSaveTaskConsumer.class
})
public class LlmTaskExecutionConfig {
}
