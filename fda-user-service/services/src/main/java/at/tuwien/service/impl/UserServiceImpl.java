package at.tuwien.service.impl;

import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.entities.user.User;
import at.tuwien.exception.UserNotFoundException;
import at.tuwien.mapper.UserMapper;
import at.tuwien.repository.jpa.UserRepository;
import at.tuwien.service.UserService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Log4j2
@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserRepository userRepository;

    @Autowired
    public UserServiceImpl(UserMapper userMapper, UserRepository userRepository) {
        this.userMapper = userMapper;
        this.userRepository = userRepository;
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
    public User create(SignupRequestDto data) {
        final User user = userMapper.signupRequestDtoToUser(data);
        user.setRealmId("82c39861-d877-4667-a0f3-4daa2ee230e0");
        user.setEmailVerified(false);
        user.setId(UUID.randomUUID().toString());
        final User entity = userRepository.save(user);
        log.info("Created user with id {}", entity.getId());
        return entity;
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
