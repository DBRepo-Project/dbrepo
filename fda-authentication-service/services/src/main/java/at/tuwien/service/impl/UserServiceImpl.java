package at.tuwien.service.impl;

import at.tuwien.api.amqp.CreateUserDto;
import at.tuwien.api.amqp.UserDetailsDto;
import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.api.user.*;
import at.tuwien.auth.MariaDbPassword;
import at.tuwien.config.AuthenticationConfig;
import at.tuwien.entities.user.RoleType;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.exception.UserEmailExistsException;
import at.tuwien.exception.UserNameExistsException;
import at.tuwien.exception.UserNotFoundException;
import at.tuwien.mapper.UserMapper;
import at.tuwien.repositories.UserRepository;
import at.tuwien.service.QueueService;
import at.tuwien.service.UserService;
import joptsimple.internal.Strings;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.ConstraintViolationException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Log4j2
@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final QueueService queueService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationConfig authenticationConfig;

    @Autowired
    public UserServiceImpl(UserMapper userMapper, QueueService queueService, UserRepository userRepository,
                           PasswordEncoder passwordEncoder, AuthenticationConfig authenticationConfig) {
        this.userMapper = userMapper;
        this.queueService = queueService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationConfig = authenticationConfig;
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
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
    @Transactional(readOnly = true)
    public User findByUsernameOrEmail(String username, String email) throws UserNotFoundException {
        /* check */
        final Optional<User> user = userRepository.findByUsernameOrEmail(username, email);
        if (user.isEmpty()) {
            log.error("User not found with username {} or email {}", username, email);
            throw new UserNotFoundException("User not found");
        }
        return user.get();
    }

    @Override
    @Transactional(readOnly = true)
    public User findByUsername(String username) throws UserNotFoundException {
        /* check */
        final Optional<User> user = userRepository.findByUsername(username);
        if (user.isEmpty()) {
            log.error("User not found with username {}", username);
            throw new UserNotFoundException("User not found");
        }
        return user.get();
    }

    @Override
    @Transactional
    public User create(SignupRequestDto data) throws UserEmailExistsException, UserNameExistsException {
        /* duplicate */
        final Optional<User> email = userRepository.findByEmail(data.getEmail());
        if (email.isPresent()) {
            log.error("Email address {} is already present in the database", data.getEmail());
            throw new UserEmailExistsException("Email taken");
        }
        final Optional<User> username = userRepository.findByUsername(data.getUsername());
        if (username.isPresent()) {
            log.error("Username {} is already present in the database", data.getUsername());
            throw new UserNameExistsException("Username taken");
        }
        /* save */
        final User user = userMapper.signupRequestDtoToUser(data);
        user.setEmailVerified(false);
        final List<RoleType> roles = new LinkedList<>(Arrays.asList(authenticationConfig.getDefaultRoles()));
        final List<String> developers = Arrays.asList(authenticationConfig.getDeveloperUsernames());
        if (developers.contains(data.getUsername())) {
            log.info("Assign all privileges to user with username {}", data.getUsername());
            roles.addAll(List.of(RoleType.ROLE_DEVELOPER, RoleType.ROLE_RESEARCHER, RoleType.ROLE_DATA_STEWARD));
        }
        user.setRoles(roles);
        user.setThemeDark(false);
        user.setPassword(passwordEncoder.encode(data.getPassword()));
        user.setDatabasePassword(MariaDbPassword.encode(data.getPassword()));
        final User entity;
        try {
            entity = userRepository.save(user);
        } catch (ConstraintViolationException e) {
            log.error("Failed to create user");
            throw new UserNameExistsException("Failed to create user", e);
        }
        log.info("Created user with id {}", entity.getId());
        log.trace("created user {}", entity);
        return entity;
    }

    @Override
    @Transactional
    public User forgot(UserForgotDto data) throws UserNotFoundException {
        /* check */
        final User user = findByUsernameOrEmail(data.getUsername(), data.getEmail());
        /* save */
        log.info("Forgot user with id {}", user.getId());
        log.trace("forgot user {}", user);
        return user;
    }

    @Override
    @Transactional
    public User update(Long id, UserUpdateDto data) throws UserNotFoundException, OrcidMalformedException {
        /* check */
        final User user = find(id);
        /* check */
        if (data.getOrcid() != null && !validateOrcid(data.getOrcid())) {
            log.error("Checksum of the provided ORCID does not match");
            log.debug("checksum of the provided orcid {} does not match", data.getOrcid());
            throw new OrcidMalformedException(data.getOrcid());
        }
        /* save */
        user.setTitlesBefore(data.getTitlesBefore());
        user.setTitlesAfter(data.getTitlesAfter());
        user.setFirstname(data.getFirstname());
        user.setLastname(data.getLastname());
        user.setUsername(user.getUsername());
        user.setAffiliation(data.getAffiliation());
        user.setOrcid(userMapper.userUpdateDtoToCompressedOrcid(data));
        log.debug("mapped data {} to new user {}", data, user);
        final User entity = userRepository.save(user);
        log.info("Updated user with id {}", entity.getId());
        log.trace("updated user {}", entity);
        return entity;
    }

    /**
     * Validates a given ORCID checksum (ISO 7064 11,2)
     * Source: https://support.orcid.org/hc/en-us/articles/360006897674-Structure-of-the-ORCID-Identifier
     *
     * @param orcid The ORCID.
     * @return True if the ORCID provided is valid, false otherwise.
     */
    protected static Boolean validateOrcid(String orcid) {
        if (orcid == null) {
            return true;
        }
        if (orcid.length() != 19) {
            log.error("Provided ORCID has an invalid length");
            log.debug("provided orcid {} has an invalid length {}, is not 19", orcid, orcid.length());
            return false;
        }
        int total = 0;
        for (int i = 0; i < orcid.length() - 1; i++) {
            if (orcid.charAt(i) == '-') {
                continue;
            }
            int digit = Character.getNumericValue(orcid.charAt(i));
            total = (total + digit) * 2;
        }
        int remainder = total % 11;
        int result = (12 - remainder) % 11;
        final String check = result == 10 ? "X" : String.valueOf(result);
        log.trace("orcid checksum is '{}'", check);
        return orcid.substring(18).equals(check);
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
                    .map(userMapper::roleTypeDtoToRoleType)
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
        log.trace("updated user {}", entity);
        return entity;
    }

    @Override
    @Transactional
    public void updateTheme(Long id, UserThemeSetDto data) throws UserNotFoundException {
        /* check */
        final User user = find(id);
        /* save */
        user.setThemeDark(data.getThemeDark());
        final User entity = userRepository.save(user);
        log.info("Updated user with id {}", entity.getId());
        log.trace("updated user {}", entity);
    }

    @Override
    @Transactional
    public User updatePassword(Long id, UserPasswordDto data) throws UserNotFoundException,
            BrokerUserCreationException {
        /* check */
        final User user = find(id);
        /* modify */
        final UserDetailsDto details = queueService.findUser(user.getUsername());
        final CreateUserDto modifyDto = userMapper.userPasswordDtoToCreateUserDto(data);
        if (details.getTags().length > 0) {
            final String tags = Strings.join(details.getTags(), ",");
            log.debug("found tags, setting the tags={}", tags);
            modifyDto.setTags(tags);
        }
        queueService.modifyUserPassword(user, modifyDto);
        log.info("Updated broker service password for user with id {}", user.getId());
        /* save */
        final String passwd = passwordEncoder.encode(data.getPassword());
        log.trace("encoded updated user password {}", passwd);
        user.setPassword(passwd);
        user.setDatabasePassword(MariaDbPassword.encode(data.getPassword()));
        log.trace("mapped password {} to updated user {}", passwd, user);
        final User entity = userRepository.save(user);
        log.info("Updated user with id {}", entity.getId());
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
