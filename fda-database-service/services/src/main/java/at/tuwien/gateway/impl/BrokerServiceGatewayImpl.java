package at.tuwien.gateway.impl;

import at.tuwien.api.amqp.CreateVirtualHostDto;
import at.tuwien.api.amqp.GrantVirtualHostPermissionsDto;
import at.tuwien.exception.BrokerVirtualHostCreationException;
import at.tuwien.gateway.BrokerServiceGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class BrokerServiceGatewayImpl implements BrokerServiceGateway {

    private final RestTemplate restTemplate;

    @Autowired
    public BrokerServiceGatewayImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public void createVirtualHost(CreateVirtualHostDto data) throws BrokerVirtualHostCreationException {
        log.debug("create virtual host at broker service {}", data);
        final ResponseEntity<Void> response = restTemplate.exchange("/api/broker/vhost", HttpMethod.POST,
                new HttpEntity<>(data), Void.class);
        if (!response.getStatusCode().equals(HttpStatus.ACCEPTED)) {
            log.error("Failed to create virtual host: {}", response.getStatusCode());
            throw new BrokerVirtualHostCreationException("Failed to create virtual host");
        }
    }

    @Override
    public void grantPermission(String username, GrantVirtualHostPermissionsDto data)
            throws BrokerVirtualHostCreationException {
        final ResponseEntity<Void> response = restTemplate.exchange("/api/broker/user/" + username + "/permission",
                HttpMethod.PUT, new HttpEntity<>(data), Void.class);
        if (!response.getStatusCode().equals(HttpStatus.ACCEPTED)) {
            log.error("Failed to grant virtual host: {}", response.getStatusCode());
            throw new BrokerVirtualHostCreationException("Failed to grant virtual host");
        }
    }

}
