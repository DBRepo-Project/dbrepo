package at.tuwien.gateway;


import at.tuwien.api.amqp.CreateVirtualHostDto;
import at.tuwien.api.amqp.GrantComponentDto;
import at.tuwien.exception.BrokerVirtualHostCreationException;

public interface BrokerServiceGateway {

    void createVirtualHost(CreateVirtualHostDto data) throws BrokerVirtualHostCreationException;

    void grantPermission(GrantComponentDto data) throws BrokerVirtualHostCreationException;
}
