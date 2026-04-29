package com.manifestreader.user.messaging;

public interface BillParseTaskPublisher {

    void publish(BillParseTaskMessage message);
}
