package com.manifestreader.user.messaging;

public interface TemplateSaveTaskPublisher {

    void publish(TemplateSaveTaskMessage message);
}
