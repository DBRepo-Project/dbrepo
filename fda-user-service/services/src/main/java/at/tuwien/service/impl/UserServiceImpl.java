package at.tuwien.service.impl;

import at.tuwien.api.auth.CreateUserDto;
import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.api.auth.TokenDto;
import at.tuwien.entities.user.User;
import at.tuwien.exception.RemoteUnavailableException;
import at.tuwien.exception.UserNotFoundException;
import at.tuwien.gateway.GatewayServiceGateway;
import at.tuwien.mapper.UserMapper;
import at.tuwien.repository.jpa.UserRepository;
import at.tuwien.service.UserService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Log4j2
@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final GatewayServiceGateway authenticationServiceGateway;

    @Autowired
    public UserServiceImpl(UserMapper userMapper, UserRepository userRepository,
                           GatewayServiceGateway authenticationServiceGateway) {
        this.userMapper = userMapper;
        this.userRepository = userRepository;
        this.authenticationServiceGateway = authenticationServiceGateway;
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public User findByUsername(String username) throws UserNotFoundException {
        final Optional<User> optional = userRepository.findByUsername(username);
        if (optional.isEmpty()) {
            log.error("Failed to retrieve user with username {}", username);
            throw new UserNotFoundException("Failed to retrieve user");
        }
        return optional.get();
    }

    @Override
    public User create(SignupRequestDto data) throws RemoteUnavailableException, UserNotFoundException {
        final TokenDto dto = authenticationServiceGateway.getToken();
        log.debug("obtained authentication token");
        final CreateUserDto userDto = userMapper.signupRequestDtoToCreateUserDto(data);
        authenticationServiceGateway.createUser(dto.getAccessToken(), userDto);
        final Optional<User> optional = userRepository.findByUsername(data.getUsername());
        if (optional.isEmpty()) {
            /* should never occur */
            throw new UserNotFoundException("User not found with username '" + data.getUsername() + "'");
        }
        final User user = optional.get();
        log.info("Created user with id {}", user.getId());
        return user;
    }

    @Override
    public User find(String id) throws UserNotFoundException {
        final Optional<User> optional = userRepository.findById(id);
        if (optional.isEmpty()) {
            log.error("Failed to retrieve user with id {}", id);
            throw new UserNotFoundException("Failed to retrieve user");
        }
        return optional.get();
    }

}
