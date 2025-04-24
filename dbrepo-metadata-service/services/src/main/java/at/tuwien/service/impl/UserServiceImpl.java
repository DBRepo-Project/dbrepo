package at.tuwien.service.impl;

import at.tuwien.api.auth.CreateUserDto;
import at.tuwien.api.user.UserUpdateDto;
import at.tuwien.entities.user.User;
import at.tuwien.exception.AuthServiceException;
import at.tuwien.exception.UserNotFoundException;
import at.tuwien.gateway.KeycloakGateway;
import at.tuwien.repository.UserRepository;
import at.tuwien.service.UserService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Log4j2
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final KeycloakGateway keycloakGateway;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, KeycloakGateway keycloakGateway) {
        this.userRepository = userRepository;
        this.keycloakGateway = keycloakGateway;
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public User findByUsername(String username) throws UserNotFoundException {
        final Optional<User> optional = userRepository.findByUsername(username);
        if (optional.isEmpty()) {
            log.error("Failed to find user with username: {}", username);
            throw new UserNotFoundException("Failed to find user with username: " + username);
        }
        return optional.get();
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAllInternalUsers() {
        return userRepository.findAllInternal();
    }

    @Override
    @Transactional(readOnly = true)
    public User findById(UUID id) throws UserNotFoundException {
        final Optional<User> optional = userRepository.findById(id);
        if (optional.isEmpty()) {
            log.error("Failed to find user with id: {}", id);
            throw new UserNotFoundException("Failed to find user with id: " + id);
        }
        return optional.get();
    }

    @Override
    @Transactional
    public User create(CreateUserDto data) {
        /* create at authentication service */
        final User entity = User.builder()
                .id(data.getLdapId())
                .keycloakId(data.getId())
                .username(data.getUsername())
                .theme("light")
                .mariadbPassword(getMariaDbPassword(RandomStringUtils.randomAlphabetic(10)))
                .language("en")
                .firstname(data.getGivenName())
                .lastname(data.getFamilyName())
                .isInternal(false)
                .build();
        /* save in metadata database */
        final User user = userRepository.save(entity);
        log.info("Created user with id: {}", user.getId());
        return user;
    }

    @Override
    @Transactional
    public User modify(User user, UserUpdateDto data) throws UserNotFoundException, AuthServiceException {
        user.setFirstname(data.getFirstname());
        user.setLastname(data.getLastname());
        user.setAffiliation(data.getAffiliation());
        user.setOrcid(data.getOrcid());
        user.setTheme(data.getTheme());
        user.setLanguage(data.getLanguage());
        /* save in auth service */
        keycloakGateway.updateUser(user.getKeycloakId(), data);
        /* save in metadata database */
        user = userRepository.save(user);
        log.info("Modified user with id: {}", user.getId());
        return user;
    }

    @Override
    public String getMariaDbPassword(String password) {
        final byte[] utf8 = password.getBytes(StandardCharsets.UTF_8);
        return "*" + DigestUtils.sha1Hex(DigestUtils.sha1(utf8)).toUpperCase();
    }
}
