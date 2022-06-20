package at.tuwien.gateway.impl;

import at.tuwien.api.amqp.CreateUserDto;
import at.tuwien.api.user.UserModifyPasswordDto;
import at.tuwien.exception.BrokerUserCreationException;
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
    public void createUser(CreateUserDto data) throws BrokerUserCreationException {
        log.debug("create user at broker service {}", data);
        final ResponseEntity<Void> response = restTemplate.exchange("/api/broker/user", HttpMethod.POST,
                new HttpEntity<>(data), Void.class);
        if (!response.getStatusCode().equals(HttpStatus.CREATED)) {
            log.error("Failed to create RabbitMQ user: {}", response.getStatusCode());
            throw new BrokerUserCreationException("Failed to create RabbitMQ user");
        }
    }

    @Override
    public void modifyUserPassword(CreateUserDto data) throws BrokerUserCreationException {
        final UserModifyPasswordDto userModifyPasswordDto = UserModifyPasswordDto.builder()
                .username(data.getUsername())
                .password(data.getPassword())
                .build();
        log.debug("modify user at broker service {}", data);
        final ResponseEntity<Void> response = restTemplate.exchange("/api/broker/user/password", HttpMethod.PUT,
                new HttpEntity<>(userModifyPasswordDto), Void.class);
        if (!response.getStatusCode().equals(HttpStatus.ACCEPTED)) {
            log.error("Failed to update RabbitMQ user: {}", response.getStatusCode());
            throw new BrokerUserCreationException("Failed to update RabbitMQ user");
        }
    }

}
