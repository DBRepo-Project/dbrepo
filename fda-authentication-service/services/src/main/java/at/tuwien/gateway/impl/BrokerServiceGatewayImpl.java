package at.tuwien.gateway.impl;

import at.tuwien.api.amqp.CreateUserDto;
import at.tuwien.api.amqp.GrantVirtualHostPermissionsDto;
import at.tuwien.api.auth.JwtResponseDto;
import at.tuwien.api.auth.LoginRequestDto;
import at.tuwien.api.user.UserModifyPasswordDto;
import at.tuwien.exception.AuthenticationInvalidException;
import at.tuwien.exception.BrokerUserCreationException;
import at.tuwien.exception.UserEmailNotVerifiedException;
import at.tuwien.exception.UserNotFoundException;
import at.tuwien.gateway.BrokerServiceGateway;
import at.tuwien.mapper.UserMapper;
import at.tuwien.service.AuthenticationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class BrokerServiceGatewayImpl implements BrokerServiceGateway {

    private final UserMapper userMapper;
    private final RestTemplate restTemplate;
    private final AuthenticationService authenticationService;

    @Autowired
    public BrokerServiceGatewayImpl(UserMapper userMapper, RestTemplate restTemplate,
                                    AuthenticationService authenticationService) {
        this.userMapper = userMapper;
        this.restTemplate = restTemplate;
        this.authenticationService = authenticationService;
    }

    @Override
    public void createUser(CreateUserDto data) throws BrokerUserCreationException {
        /* create user */
        final ResponseEntity<Void> createResponse = restTemplate.exchange("/api/broker/user", HttpMethod.POST,
                new HttpEntity<>(data), Void.class);
        if (!createResponse.getStatusCode().equals(HttpStatus.CREATED)) {
            log.error("Failed to create user at queue service: {}", createResponse.getStatusCode());
            throw new BrokerUserCreationException("Failed to create user at queue service");
        }
        log.info("Created user at queue service with username {}", data.getUsername());
        final GrantVirtualHostPermissionsDto grantDto = userMapper.signupRequestDtoToGrantComponentDto();
        final String url = "/api/broker/user/" + data.getUsername() + "/permission";
        final ResponseEntity<Void> grantResponse = restTemplate.exchange(url, HttpMethod.PUT,
                new HttpEntity<>(grantDto), Void.class);
        if (!grantResponse.getStatusCode().equals(HttpStatus.ACCEPTED)) {
            log.error("Failed to grant permissions at queue service: {}", createResponse.getStatusCode());
            throw new BrokerUserCreationException("Failed to grant permissions at queue service");
        }
        log.info("Granted user permissions at queue service for username {}", data.getUsername());
        log.debug("granted user permissions at queue service {}", grantDto);
    }

    @Override
    public void modifyUserPassword(UserModifyPasswordDto data) throws BrokerUserCreationException,
            UserNotFoundException, UserEmailNotVerifiedException {
        /* obtain token */
        final JwtResponseDto obtainResponse = authenticationService.authenticate(data);
        /* modify at broker service */
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + obtainResponse.getToken());
        log.debug("modify user at broker service {}", data);
        final String url = "/api/broker/user/" + data.getUsername() + "/password";
        final ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.PUT,
                new HttpEntity<>(data, headers), Void.class);
        if (!response.getStatusCode().equals(HttpStatus.ACCEPTED)) {
            log.error("Failed to update user password at queue service: {}", response.getStatusCode());
            throw new BrokerUserCreationException("Failed to update user password at queue service");
        }
        log.info("Updated user password at queue service for username {}", data.getUsername());
        log.debug("updated user password at queue service {}", data);
    }

}
