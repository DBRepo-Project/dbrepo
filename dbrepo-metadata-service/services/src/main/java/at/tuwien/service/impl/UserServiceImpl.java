package at.tuwien.service.impl;

import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.api.user.*;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.repository.mdb.UserRepository;
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
            log.error("Failed to find user with username {} in metadata database", username);
            throw new UserNotFoundException("Failed to find user with username " + username + " in metadata database");
        }
        return optional.get();
    }

    @Override
    public User find(UUID id) throws UserNotFoundException {
        final Optional<User> optional = userRepository.findById(id);
        if (optional.isEmpty()) {
            log.error("Failed to find user with id {} in metadata database", id);
            throw new UserNotFoundException("Failed to find user with id " + id + " in metadata database");
        }
        return optional.get();
    }

    @Override
    public User create(SignupRequestDto data, UUID id) {
        /* create at authentication service */
        final User entity = User.builder()
                .id(id)
                .username(data.getUsername())
                .email(data.getEmail())
                .themeDark(false)
                .mariadbPassword(getMariaDbPassword(data.getPassword()))
                .build();
        /* create at metadata database */
        final User user = userRepository.save(entity);
        log.info("Created user with id {} in metadata database", user.getId());
        return user;
    }

    @Override
    public User modify(UUID id, UserUpdateDto data) throws UserNotFoundException {
        final User entity = find(id);
        entity.setFirstname(data.getFirstname());
        entity.setLastname(data.getLastname());
        entity.setAffiliation(data.getAffiliation());
        entity.setOrcid(data.getOrcid());
        /* create at metadata database */
        final User user = userRepository.save(entity);
        log.info("Modified user with id {} in metadata database", user.getId());
        return user;
    }

    @Override
    public void updatePassword(UUID id, UserPasswordDto data) throws UserNotFoundException {
        final User user = find(id);
        user.setMariadbPassword(getMariaDbPassword(data.getPassword()));
        userRepository.save(user);
        log.info("Updated password of user with id {} in metadata database", id);
    }

    @Override
    public User toggleTheme(UUID id, UserThemeSetDto data) throws UserNotFoundException {
        final User entity = find(id);
        entity.setThemeDark(data.getThemeDark());
        final User user = userRepository.save(entity);
        log.info("Updated theme of user with id {} in metadata database", id);
        return user;
    }

    @Override
    public void validateUsernameNotExists(String username) throws UserAlreadyExistsException {
        if (userRepository.existsByUsername(username)) {
            throw new UserAlreadyExistsException("User with username " + username + " already exists in metadata database");
        }
    }

    @Override
    public void validateEmailNotExists(String email) throws UserEmailAlreadyExistsException {
        if (userRepository.existsByEmail(email)) {
            throw new UserEmailAlreadyExistsException("User with email " + email + " already exists in metadata database");
        }
    }

    protected String getMariaDbPassword(String password) {
        final byte[] utf8 = password.getBytes(StandardCharsets.UTF_8);
        return "*" + DigestUtils.sha1Hex(DigestUtils.sha1(utf8)).toUpperCase();
    }
}
