package at.tuwien.gateway;

import at.tuwien.api.amqp.ConsumerDto;

import java.util.List;

public interface BrokerServiceGateway {

    List<ConsumerDto> findAllConsumers();
}
