package at.tuwien.service.impl;

import at.tuwien.api.amqp.CreateUserDto;
import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.api.user.UserPasswordDto;
import at.tuwien.entities.user.User;
import at.tuwien.exception.BrokerUserCreationException;
import at.tuwien.gateway.BrokerServiceGateway;
import at.tuwien.mapper.UserMapper;
import at.tuwien.service.QueueService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class QueueServiceImpl implements QueueService {

    private final UserMapper userMapper;
    private final BrokerServiceGateway brokerServiceGateway;

    @Autowired
    public QueueServiceImpl(UserMapper userMapper, BrokerServiceGateway brokerServiceGateway) {
        this.userMapper = userMapper;
        this.brokerServiceGateway = brokerServiceGateway;
    }

    @Override
    public void createUser(SignupRequestDto data) throws BrokerUserCreationException {
        final CreateUserDto userDto = userMapper.signupRequestDtoToCreateUserDto(data);
        brokerServiceGateway.createUser(userDto);
    }

    @Override
    public void modifyUserPassword(User user, UserPasswordDto data) throws BrokerUserCreationException {
        final CreateUserDto dto = CreateUserDto.builder()
                .username(user.getUsername())
                .password(data.getPassword())
                .build();
        brokerServiceGateway.modifyUserPassword(dto);
    }

}
