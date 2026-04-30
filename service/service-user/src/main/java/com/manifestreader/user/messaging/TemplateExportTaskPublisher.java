package com.manifestreader.user.messaging;

public interface TemplateExportTaskPublisher {

    void publish(TemplateExportTaskMessage message);
}
