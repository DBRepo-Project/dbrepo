package at.tuwien.service.impl;

import at.tuwien.api.amqp.CreateUserDto;
import at.tuwien.api.amqp.UserDetailsDto;
import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.api.user.UserPasswordDto;
import at.tuwien.entities.user.User;
import at.tuwien.exception.BrokerUserCreationException;
import at.tuwien.gateway.BrokerServiceGateway;
import at.tuwien.mapper.AmqpMapper;
import at.tuwien.service.QueueService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class QueueServiceImpl implements QueueService {

    private final AmqpMapper amqpMapper;
    private final BrokerServiceGateway brokerServiceGateway;

    @Autowired
    public QueueServiceImpl(AmqpMapper amqpMapper, BrokerServiceGateway brokerServiceGateway) {
        this.amqpMapper = amqpMapper;
        this.brokerServiceGateway = brokerServiceGateway;
    }

    @Override
    public UserDetailsDto findUser(String username) throws BrokerUserCreationException {
        log.debug("broker service find user, username={}", username);
        return brokerServiceGateway.findUser(username);
    }

    @Override
    public void createUser(String username, SignupRequestDto data) throws BrokerUserCreationException {
        log.debug("broker service create user, username={}, data={}", username, data);
        final CreateUserDto userDto = CreateUserDto.builder()
                .password(data.getPassword())
                .tags("")
                .build();
        brokerServiceGateway.createUser(username, userDto);
        brokerServiceGateway.modifyHostPermissions(username, amqpMapper.defaultVirtualHostUserPermissions());
    }

    @Override
    public void modifyUserPassword(User user, CreateUserDto data) throws BrokerUserCreationException {
        log.debug("broker service create user, user={}, data={}", user, data);
        brokerServiceGateway.modifyUserPassword(user.getUsername(), data);
    }

}
