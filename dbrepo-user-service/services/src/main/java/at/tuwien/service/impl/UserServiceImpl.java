package at.tuwien.service.impl;

import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.api.user.UserPasswordDto;
import at.tuwien.api.user.UserThemeSetDto;
import at.tuwien.api.user.UserUpdateDto;
import at.tuwien.entities.user.*;
import at.tuwien.exception.*;
import at.tuwien.mapper.UserMapper;
import at.tuwien.repository.mdb.*;
import at.tuwien.repository.sdb.UserIdxRepository;
import at.tuwien.service.UserAttributeService;
import at.tuwien.service.UserService;
import lombok.extern.log4j.Log4j2;
import org.keycloak.common.util.Base64;
import org.keycloak.common.util.PaddingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Log4j2
@Service
public class UserServiceImpl implements UserService {


    private static final String ID = "pbkdf2-sha256";
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int DEFAULT_ITERATIONS = 27500;
    private static final Integer DERIVED_KEY_SIZE = 256;
    private static final Integer MAX_PADDING_LENGTH = 14;

    private final UserMapper userMapper;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final UserIdxRepository userIdxRepository;
    private final UserAttributeService userAttributeService;
    private final CredentialRepository credentialRepository;

    @Autowired
    public UserServiceImpl(UserMapper userMapper, RoleRepository roleRepository, UserRepository userRepository,
                           GroupRepository groupRepository, UserIdxRepository userIdxRepository,
                           UserAttributeService userAttributeService, CredentialRepository credentialRepository) {
        this.userMapper = userMapper;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.userIdxRepository = userIdxRepository;
        this.userAttributeService = userAttributeService;
        this.credentialRepository = credentialRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public User create(SignupRequestDto data, Realm realm) throws UserAlreadyExistsException {
        /* create secret */
        final byte[] salt = getSalt();
        final StringBuilder secretData = new StringBuilder("{\"value\":\"")
                .append(encodedCredential(data.getPassword(), DEFAULT_ITERATIONS, salt, DERIVED_KEY_SIZE))
                .append("\",\"salt\":\"")
                .append(Base64.encodeBytes(salt))
                .append("\",\"additionalParameters\":{}}");
        Credential credential = Credential.builder()
                .id(UUID.randomUUID())
                .createdDate(Instant.now().toEpochMilli())
                .secretData(secretData.toString())
                .type("password")
                .priority(10)
                .credentialData("{\"hashIterations\":" + DEFAULT_ITERATIONS + ",\"algorithm\":\"" + ID + "\",\"additionalParameters\":{}}")
                .build();
        /* save user attributes */
        User user = userMapper.signupRequestDtoToUser(data);
        user.setId(UUID.randomUUID());
        user.setEmailVerified(false);
        user.setEnabled(true);
        user.setRealmId(realm.getId());
        user.setCreatedTimestamp(Instant.now().toEpochMilli());
        user = userRepository.save(user);
        final UserAttribute userAttribute1 = userAttributeService.create(userMapper.tripleToUserAttribute(user.getId(),
                "theme_dark", "false"));
        final UserAttribute userAttribute2 = userAttributeService.create(userMapper.tripleToUserAttribute(user.getId(),
                "orcid", ""));
        final UserAttribute userAttribute3 = userAttributeService.create(userMapper.tripleToUserAttribute(user.getId(),
                "affiliation", ""));
        credential.setUserId(user.getId());
        /* find default roles and groups */
        final List<Group> groups = groupRepository.findDefault();
        final Optional<Role> optionalRole = roleRepository.findDefault();
        if (optionalRole.isPresent()) {
            final Role defaultRole = optionalRole.get();
            log.debug("set default role: {}", defaultRole.getName());
            user.setRoles(List.of(defaultRole));
        } else {
            user.setRoles(List.of());
        }
        /* save in metadata database */
        credential = credentialRepository.save(credential);
        user.setCredentials(List.of(credential));
        user.setAttributes(List.of(userAttribute1, userAttribute2, userAttribute3));
        user.setGroups(groups);
        log.info("Created user with id {} in metadata database", user.getId());
        /* save in open search database */
        userIdxRepository.save(userMapper.userToUserDto(user));
        log.info("Created user with id {} in open search database", user.getId());
        return user;
    }

    @Override
    @Transactional
    public User modify(UUID id, UserUpdateDto data) throws UserNotFoundException, UserAttributeNotFoundException {
        /* check */
        final User entity = find(id);
        entity.setFirstname(data.getFirstname());
        entity.setLastname(data.getLastname());
        /* save in metadata database */
        final User user = userRepository.save(entity);
        log.info("Modified user with id {}", user.getId());
        /* modify attributes */
        userAttributeService.update(user.getId(), "orcid", data.getOrcid());
        userAttributeService.update(user.getId(), "affiliation", data.getAffiliation());
        /* save in open search database */
        userIdxRepository.save(userMapper.userToUserDto(user));
        return user;
    }

    @Override
    @Transactional(rollbackFor = RuntimeException.class)
    public User updatePassword(UUID id, UserPasswordDto data) throws UserNotFoundException {
        final User user = find(id);
        /* create secret */
        final byte[] salt = getSalt();
        final StringBuilder secretData = new StringBuilder("{\"value\":\"")
                .append(encodedCredential(data.getPassword(), DEFAULT_ITERATIONS, salt, DERIVED_KEY_SIZE))
                .append("\",\"salt\":\"")
                .append(Base64.encodeBytes(salt))
                .append("\",\"additionalParameters\":{}}");
        final Credential entity = Credential.builder()
                .id(UUID.randomUUID())
                .userId(user.getId())
                .createdDate(Instant.now().toEpochMilli())
                .secretData(secretData.toString())
                .type("password")
                .priority(10)
                .credentialData("{\"hashIterations\":" + DEFAULT_ITERATIONS + ",\"algorithm\":\"" + ID + "\",\"additionalParameters\":{}}")
                .build();
        /* save */
        final Credential credential = credentialRepository.save(entity);
        user.setCredentials(List.of(credential));
        log.info("Updated user password with id {}", user.getId());
        return user;
    }

    @Override
    @Transactional
    public User toggleTheme(UUID id, UserThemeSetDto data) throws UserNotFoundException,
            UserAttributeNotFoundException {
        /* check */
        final User user = find(id);
        final UserAttribute entity = userAttributeService.update(user.getId(), "theme_dark", data.getThemeDark().toString());
        log.info("Updated theme by updating attribute with id {}", entity.getId());
        return user;
    }

    @Override
    @Transactional(readOnly = true)
    public User find(UUID id) throws UserNotFoundException {
        final Optional<User> optional = userRepository.findById(id);
        if (optional.isEmpty()) {
            log.error("Failed to retrieve user with id {}", id);
            throw new UserNotFoundException("Failed to retrieve user");
        }
        return optional.get();
    }

    @Override
    @Transactional(readOnly = true)
    public void validateUsernameNotExists(String username) throws UserAlreadyExistsException {
        final Optional<User> optional = userRepository.findByUsername(username);
        if (optional.isPresent()) {
            log.error("User with username {} already exists", username);
            throw new UserAlreadyExistsException("User with username " + username + " already exists");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void validateEmailNotExists(String email) throws UserEmailAlreadyExistsException {
        final Optional<User> optional = userRepository.findByEmail(email);
        if (optional.isPresent()) {
            log.error("User with email {} already exists", email);
            throw new UserEmailAlreadyExistsException("User with email already exists");
        }
    }

    private String encodedCredential(String rawPassword, int iterations, byte[] salt, int derivedKeySize) {
        final String rawPasswordWithPadding = PaddingUtils.padding(rawPassword, MAX_PADDING_LENGTH);
        final KeySpec spec = new PBEKeySpec(rawPasswordWithPadding.toCharArray(), salt, iterations, derivedKeySize);
        try {
            byte[] key = getSecretKeyFactory().generateSecret(spec).getEncoded();
            return Base64.encodeBytes(key);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException("Credential could not be encoded", e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] getSalt() {
        byte[] buffer = new byte[16];
        final SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(buffer);
        return buffer;
    }

    private SecretKeyFactory getSecretKeyFactory() {
        try {
            return SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(PBKDF2_ALGORITHM + " algorithm not found", e);
        }
    }
}
