package at.tuwien.service.impl;

import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.api.user.UserEmailDto;
import at.tuwien.api.user.UserPasswordDto;
import at.tuwien.api.user.UserRolesDto;
import at.tuwien.api.user.UserUpdateDto;
import at.tuwien.entities.user.RoleType;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.mapper.UserMapper;
import at.tuwien.repositories.UserRepository;
import at.tuwien.service.UserService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Log4j2
@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserServiceImpl(UserMapper userMapper, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        final List<User> users = userRepository.findAll();
        log.info("Found {} users", users.size());
        return users;
    }

    @Override
    @Transactional(readOnly = true)
    public User find(Long id) throws UserNotFoundException {
        /* check */
        final Optional<User> user = userRepository.findById(id);
        if (user.isEmpty()) {
            log.error("User not found with id {}", id);
            throw new UserNotFoundException("User not found");
        }
        return user.get();
    }

    @Override
    @Transactional
    public User create(SignupRequestDto data) throws UserEmailExistsException, UserNameExistsException {
        /* check */
        final Optional<User> email = userRepository.findByEmail(data.getEmail());
        if (email.isPresent()) {
            log.error("Email address is already present in the database");
            throw new UserEmailExistsException("Email taken");
        }
        final Optional<User> username = userRepository.findByUsername(data.getEmail());
        if (username.isPresent()) {
            log.error("Username is already present in the database");
            throw new UserNameExistsException("Username taken");
        }
        /* get role */
        /* save */
        final User user = userMapper.signupRequestDtoToUser(data);
        user.setEmailVerified(false);
        user.setRoles(List.of(RoleType.ROLE_RESEARCHER));
        user.setPassword(passwordEncoder.encode(data.getPassword()));
        final User entity = userRepository.save(user);
        log.info("Created user with id {}", entity.getId());
        log.debug("created user {}", entity);
        return entity;
    }

    @Override
    @Transactional
    public User update(Long id, UserUpdateDto data) throws UserNotFoundException {
        /* check */
        final User user = find(id);
        /* save */
        user.setTitlesBefore(data.getTitlesBefore());
        user.setTitlesAfter(data.getTitlesAfter());
        user.setFirstname(data.getFirstname());
        user.setLastname(data.getLastname());
        user.setUsername(user.getUsername());
        log.debug("mapped data {} to new user {}", data, user);
        final User entity = userRepository.save(user);
        log.info("Updated user with id {}", entity.getId());
        log.debug("updated user {}", entity);
        return entity;
    }

    @Override
    @Transactional
    public User updateRoles(Long id, UserRolesDto data)
            throws UserNotFoundException, RoleNotFoundException, RoleUniqueException {
        /* check */
        final User user = find(id);
        /* save */
        try {
            user.setRoles(data.getRoles()
                    .stream()
                    .map(RoleType::valueOf)
                    .collect(Collectors.toList()));
        } catch (IllegalArgumentException e) {
            log.error("Failed to map roles {}", data.getRoles());
            throw new RoleNotFoundException("Failed to map roles");
        }
        log.debug("mapped roles {} to updated user {}", data, user);
        final User entity;
        try {
            entity = userRepository.save(user);
        } catch (DuplicateKeyException e) {
            log.error("Failed to assign roles, must be unique");
            throw new RoleUniqueException("Failed to assign roles", e);
        }
        log.info("Updated user with id {}", entity.getId());
        log.debug("updated user {}", entity);
        return entity;
    }

    @Override
    @Transactional
    public User updatePassword(Long id, UserPasswordDto data) throws UserNotFoundException {
        /* check */
        final User user = find(id);
        /* save */
        final String passwd = passwordEncoder.encode(data.getPassword());
        user.setPassword(passwd);
        log.debug("mapped password {} to updated user {}", passwd, user);
        final User entity = userRepository.save(user);
        log.info("Updated user with id {}", entity.getId());
        log.debug("updated user {}", entity);
        return entity;
    }

    @Override
    @Transactional
    public User updateEmail(Long id, UserEmailDto data) throws UserNotFoundException {
        /* check */
        final User user = find(id);
        /* save */
        user.setEmail(data.getEmail());
        log.debug("mapped email {} to updated user {}", data, user);
        final User entity = userRepository.save(user);
        log.info("Updated user with id {}", entity.getId());
        log.debug("updated user {}", entity);
        return entity;
    }

}
