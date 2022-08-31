package at.tuwien.service.impl;

import at.tuwien.api.amqp.CreateUserDto;
import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.api.user.UserPasswordDto;
import at.tuwien.entities.user.User;
import at.tuwien.exception.BrokerUserCreationException;
import at.tuwien.gateway.BrokerServiceGateway;
import at.tuwien.service.QueueService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class QueueServiceImpl implements QueueService {

    private final BrokerServiceGateway brokerServiceGateway;

    @Autowired
    public QueueServiceImpl(BrokerServiceGateway brokerServiceGateway) {
        this.brokerServiceGateway = brokerServiceGateway;
    }

    @Override
    public void createUser(String username, SignupRequestDto data) throws BrokerUserCreationException {
        final CreateUserDto userDto = CreateUserDto.builder()
                .password(data.getPassword())
                .tags("")
                .build();
        brokerServiceGateway.createUser(username, userDto);
        brokerServiceGateway.grantUserHost(username);
    }

    @Override
    public void modifyUserPassword(User user, UserPasswordDto data) throws BrokerUserCreationException {
        final CreateUserDto userDto = CreateUserDto.builder()
                .password(data.getPassword())
                .tags("")
                .build();
        brokerServiceGateway.modifyUserPassword(user.getUsername(), userDto);
    }

}
