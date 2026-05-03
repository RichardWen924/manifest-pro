package com.manifestreader.market.messaging;

public interface FreightDemandAcceptedPublisher {

    void publish(FreightDemandAcceptedMessage message);
}
