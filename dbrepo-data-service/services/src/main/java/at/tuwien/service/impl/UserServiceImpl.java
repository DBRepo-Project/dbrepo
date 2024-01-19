package at.tuwien.service.impl;

import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.repository.mdb.UserRepository;
import at.tuwien.service.UserService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Log4j2
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public User findByUsername(String username) throws UserNotFoundException {
        final Optional<User> optional = userRepository.findByUsername(username);
        if (optional.isEmpty()) {
            log.error("Failed to find user with username {}: not present in metadata database", username);
            throw new UserNotFoundException("Failed to find user with username " + username + ": not present in metadata database");
        }
        return optional.get();
    }
}
