package at.tuwien.service.impl;

import at.tuwien.entities.container.Container;
import at.tuwien.entities.container.image.ContainerImageEnvironmentItem;
import at.tuwien.entities.container.image.ContainerImageEnvironmentItemType;
import at.tuwien.entities.user.User;
import at.tuwien.exception.UserNotFoundException;
import at.tuwien.repository.jpa.UserRepository;
import at.tuwien.service.UserService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Log4j2
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserServiceImpl(UserRepository userRepository) {
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
    @Transactional(readOnly = true)
    public User findByPrincipalOrAnonymous(Principal principal, Container container) throws UserNotFoundException {
        log.trace("find user or anonymous, principal={}, container={}", principal, container);
        if (principal == null) {
            final String username = container.getImage()
                    .getEnvironment()
                    .stream()
                    .filter(e -> e.getType().equals(ContainerImageEnvironmentItemType.PRIVILEGED_USERNAME))
                    .map(ContainerImageEnvironmentItem::getValue)
                    .collect(Collectors.toList())
                    .get(0);
            final String password = container.getImage()
                    .getEnvironment()
                    .stream()
                    .filter(e -> e.getType().equals(ContainerImageEnvironmentItemType.PRIVILEGED_PASSWORD))
                    .map(ContainerImageEnvironmentItem::getValue)
                    .collect(Collectors.toList())
                    .get(0);
            final User user = User.builder()
                    .username(username)
                    .databasePassword(password)
                    .build();
            log.trace("mapped anonymous user {}", user);
            return user;
        }
        return findByUsername(principal.getName());
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
