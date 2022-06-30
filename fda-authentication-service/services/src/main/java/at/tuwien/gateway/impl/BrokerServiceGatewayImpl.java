package at.tuwien.gateway.impl;

import at.tuwien.api.amqp.CreateUserDto;
import at.tuwien.api.amqp.GrantVirtualHostPermissionsDto;
import at.tuwien.api.user.UserModifyPasswordDto;
import at.tuwien.exception.BrokerUserCreationException;
import at.tuwien.gateway.BrokerServiceGateway;
import at.tuwien.mapper.UserMapper;
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
    private final UserMapper userMapper;

    @Autowired
    public BrokerServiceGatewayImpl(RestTemplate restTemplate, UserMapper userMapper) {
        this.restTemplate = restTemplate;
        this.userMapper = userMapper;
    }

    @Override
    public void createUser(CreateUserDto data) throws BrokerUserCreationException {
        final GrantVirtualHostPermissionsDto grantDto = userMapper.signupRequestDtoToGrantComponentDto();
        final ResponseEntity<Void> createResponse = restTemplate.exchange("/api/broker/user", HttpMethod.POST,
                new HttpEntity<>(data), Void.class);
        if (!createResponse.getStatusCode().equals(HttpStatus.CREATED)) {
            log.error("Failed to create user at queue service: {}", createResponse.getStatusCode());
            throw new BrokerUserCreationException("Failed to create user at queue service");
        }
        log.info("Created user at queue service with username {}", data.getUsername());
        final ResponseEntity<Void> grantResponse = restTemplate.exchange(
                "/api/broker/user/" + data.getUsername() + "/permission", HttpMethod.PUT, new HttpEntity<>(grantDto),
                Void.class);
        if (!grantResponse.getStatusCode().equals(HttpStatus.ACCEPTED)) {
            log.error("Failed to grant permissions at queue service: {}", createResponse.getStatusCode());
            throw new BrokerUserCreationException("Failed to grant permissions at queue service");
        }
        log.info("Granted user permissions at queue service for username {}", data.getUsername());
        log.debug("granted user permissions at queue service {}", grantDto);
    }

    @Override
    public void modifyUserPassword(String username, UserModifyPasswordDto data) throws BrokerUserCreationException {
        log.debug("modify user at broker service {}", data);
        final ResponseEntity<Void> response = restTemplate.exchange("/api/broker/user/" + username + "/password",
                HttpMethod.PUT, new HttpEntity<>(data), Void.class);
        if (!response.getStatusCode().equals(HttpStatus.ACCEPTED)) {
            log.error("Failed to update user password at queue service: {}", response.getStatusCode());
            throw new BrokerUserCreationException("Failed to update user password at queue service");
        }
        log.info("Updated user password at queue service for username {}", username);
        log.debug("updated user password at queue service {}", data);
    }

}
