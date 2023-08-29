package at.tuwien.service.impl;

import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.api.user.*;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.gateway.KeycloakGateway;
import at.tuwien.mapper.UserMapper;
import at.tuwien.repository.mdb.UserRepository;
import at.tuwien.repository.sdb.UserIdxRepository;
import at.tuwien.service.UserService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Log4j2
@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final KeycloakGateway keycloakGateway;
    private final UserIdxRepository userIdxRepository;

    @Autowired
    public UserServiceImpl(UserMapper userMapper, UserRepository userRepository, KeycloakGateway keycloakGateway,
                           UserIdxRepository userIdxRepository) {
        this.userMapper = userMapper;
        this.userRepository = userRepository;
        this.keycloakGateway = keycloakGateway;
        this.userIdxRepository = userIdxRepository;
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public User findByUsername(String username) throws UserNotFoundException {
        final Optional<User> optional = userRepository.findByUsername(username);
        if (optional.isEmpty()) {
            log.error("Failed to find user with username {}: not present in metadata database", username);
            throw new UserNotFoundException("Failed to find user with username " + username + ": not present in metadata database");
        }
        return optional.get();
    }

    @Override
    public User find(UUID id) throws UserNotFoundException {
        final Optional<User> optional = userRepository.findById(id);
        if (optional.isEmpty()) {
            log.error("Failed to find user with id {}: not present in metadata database", id);
            throw new UserNotFoundException("Failed to find user with id " + id + ": not present in metadata database");
        }
        return optional.get();
    }

    @Override
    public User create(SignupRequestDto data) throws UserAlreadyExistsException, AccessDeniedException,
            KeycloakRemoteException, UserNotFoundException {
        /* create at authentication service */
        final User entity = User.builder()
                .username(data.getUsername())
                .email(data.getEmail())
                .themeDark(false)
                .mariadbPassword(getMariaDbPassword(data.getPassword()))
                .build();
        keycloakGateway.createUser(userMapper.signupRequestDtoToUserCreateDto(data));
        /* create at metadata database */
        entity.setId(keycloakGateway.findByUsername(data.getUsername()).getId());
        final User user = userRepository.save(entity);
        log.info("Created user with id {} in metadata database", user.getId());
        /* save in open search database */
        userIdxRepository.save(userMapper.userToUserDto(user));
        log.info("Created user with id {} in open search database", user.getId());
        return user;
    }

    @Override
    public User modify(UUID id, UserUpdateDto data) throws UserNotFoundException {
        final User entity = find(id);
        entity.setFirstname(data.getFirstname());
        entity.setLastname(data.getLastname());
        entity.setAffiliation(data.getAffiliation());
        entity.setOrcid(data.getOrcid());
        final User user = userRepository.save(entity);
        log.info("Updated user data for user with id {}", user.getId());
        return user;
    }

    @Override
    public void updatePassword(UUID id, UserPasswordDto data) throws KeycloakRemoteException, AccessDeniedException,
            UserNotFoundException {
        final User user = find(id);
        user.setMariadbPassword(getMariaDbPassword(data.getPassword()));
        userRepository.save(user);
        log.debug("updated password in metadata database");
        keycloakGateway.updateUserCredentials(id, data);
        log.debug("updated password in keycloak");
        log.info("Updated user password with id {}", id);
    }

    @Override
    public User toggleTheme(UUID id, UserThemeSetDto data) throws UserNotFoundException {
        final User entity = find(id);
        entity.setThemeDark(data.getThemeDark());
        final User user = userRepository.save(entity);
        log.info("Updated theme by updating attribute with id {}", id);
        return user;
    }

    @Override
    public void validateUsernameNotExists(String username) throws UserAlreadyExistsException {
        if (userRepository.existsByUsername(username)) {
            log.error("User with username {} already exists in metadata database", username);
            throw new UserAlreadyExistsException("User with username " + username + " already exists in metadata database");
        }
    }

    @Override
    public void validateEmailNotExists(String email) throws UserEmailAlreadyExistsException {
        if (userRepository.existsByEmail(email)) {
            log.error("User with email {} already exists in metadata database", email);
            throw new UserEmailAlreadyExistsException("User with email " + email + " already exists in metadata database");
        }
    }

    protected String getMariaDbPassword(String password) {
        final byte[] utf8 = password.getBytes(StandardCharsets.UTF_8);
        return "*" + DigestUtils.sha1Hex(DigestUtils.sha1(utf8)).toUpperCase();
    }
}
