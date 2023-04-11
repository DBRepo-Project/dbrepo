package at.tuwien.service.impl;

import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.api.user.UserPasswordDto;
import at.tuwien.api.user.UserThemeSetDto;
import at.tuwien.api.user.UserUpdateDto;
import at.tuwien.entities.auth.Realm;
import at.tuwien.entities.user.*;
import at.tuwien.exception.*;
import at.tuwien.mapper.UserMapper;
import at.tuwien.repository.jpa.CredentialRepository;
import at.tuwien.repository.jpa.RoleMappingRepository;
import at.tuwien.repository.jpa.UserAttributeRepository;
import at.tuwien.repository.jpa.UserRepository;
import at.tuwien.service.UserAttributeService;
import at.tuwien.service.UserService;
import lombok.extern.log4j.Log4j2;
import org.keycloak.common.util.Base64;
import org.keycloak.common.util.PaddingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Log4j2
@Service
public class UserServiceImpl implements UserService {


    private static final String ID = "pbkdf2-sha256";
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int DEFAULT_ITERATIONS = 27500;
    private static final Integer DERIVED_KEY_SIZE = 256;
    private static final Integer MAX_PADDING_LENGTH = 14;

    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final UserAttributeService userAttributeService;
    private final CredentialRepository credentialRepository;
    private final RoleMappingRepository roleMappingRepository;

    @Autowired
    public UserServiceImpl(UserMapper userMapper, UserRepository userRepository,
                           UserAttributeService userAttributeService, CredentialRepository credentialRepository,
                           RoleMappingRepository roleMappingRepository) {
        this.userMapper = userMapper;
        this.userRepository = userRepository;
        this.userAttributeService = userAttributeService;
        this.credentialRepository = credentialRepository;
        this.roleMappingRepository = roleMappingRepository;
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
    public User findById(String id) throws UserNotFoundException {
        final Optional<User> optional = userRepository.findById(id);
        if (optional.isEmpty()) {
            log.error("Failed to retrieve user with id {}", id);
            throw new UserNotFoundException("Failed to retrieve user");
        }
        return optional.get();
    }

    @Override
    public User create(SignupRequestDto data, Realm realm, Role role) throws RemoteUnavailableException, UserNotFoundException,
            UserAlreadyExistsException {
        /* check */
        final Optional<User> optional = userRepository.findByUsername(data.getUsername());
        if (optional.isPresent()) {
            log.error("User with username {} already exists", data.getUsername());
            throw new UserAlreadyExistsException("User with username " + data.getUsername() + " already exists");
        }
        final Optional<User> optional2 = userRepository.findByEmail(data.getEmail());
        if (optional2.isPresent()) {
            log.error("User with email {} already exists", data.getUsername());
            throw new UserAlreadyExistsException("User with email " + data.getUsername() + " already exists");
        }
        /* create secret */
        final byte[] salt = getSalt();
        final StringBuilder secretData = new StringBuilder("{\"value\":\"")
                .append(encodedCredential(data.getPassword(), DEFAULT_ITERATIONS, salt, DERIVED_KEY_SIZE))
                .append("\",\"salt\":\"")
                .append(Base64.encodeBytes(salt))
                .append("\",\"additionalParameters\":{}}");
        Credential credential = Credential.builder()
                .createdDate(Instant.now().toEpochMilli())
                .secretData(secretData.toString())
                .type("password")
                .priority(10)
                .credentialData("{\"hashIterations\":" + DEFAULT_ITERATIONS + ",\"algorithm\":\"" + ID + "\",\"additionalParameters\":{}}")
                .build();
        /* save */
        User user = userMapper.signupRequestDtoToUser(data);
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
        credential = credentialRepository.save(credential);
        user.setCredentials(List.of(credential));
        user.setAttributes(List.of(userAttribute1, userAttribute2, userAttribute3));
        final RoleMapping tmp2 = RoleMapping.builder()
                .userId(user.getId())
                .roleId(role.getId())
                .build();
        roleMappingRepository.save(tmp2);
        user.setRoles(List.of(role));
        log.info("Created user with id {}", user.getId());
        log.debug("created user {}", user);
        return user;
    }

    @Override
    public User modify(String id, UserUpdateDto data, Principal principal) throws UserNotFoundException,
            ForeignUserException, UserAttributeNotFoundException {
        /* check */
        User user = findById(id);
        if (!user.getUsername().equals(principal.getName())) {
            log.error("Failed to modify user: attempting to modify other user");
            throw new ForeignUserException("Failed to modify user: attempting to modify other user");
        }
        user.setFirstname(data.getFirstname());
        user.setLastname(data.getLastname());
        /* save in metadata database */
        user = userRepository.save(user);
        log.info("Modified user with id {}", user.getId());
        /* modify attributes */
        userAttributeService.update(user.getId(), "orcid", data.getOrcid());
        userAttributeService.update(user.getId(), "affiliation", data.getAffiliation());
        return user;
    }

    @Override
    public User updatePassword(String id, UserPasswordDto data, Principal principal) throws UserNotFoundException,
            ForeignUserException {
        /* check */
        final User user = findById(id);
        if (!user.getUsername().equals(principal.getName())) {
            log.error("Failed to modify user: attempting to modify other user");
            throw new ForeignUserException("Failed to modify user: attempting to modify other user");
        }
        /* create secret */
        final byte[] salt = getSalt();
        final StringBuilder secretData = new StringBuilder("{\"value\":\"")
                .append(encodedCredential(data.getPassword(), DEFAULT_ITERATIONS, salt, DERIVED_KEY_SIZE))
                .append("\",\"salt\":\"")
                .append(Base64.encodeBytes(salt))
                .append("\",\"additionalParameters\":{}}");
        Credential credential = Credential.builder()
                .createdDate(Instant.now().toEpochMilli())
                .secretData(secretData.toString())
                .type("password")
                .priority(10)
                .credentialData("{\"hashIterations\":" + DEFAULT_ITERATIONS + ",\"algorithm\":\"" + ID + "\",\"additionalParameters\":{}}")
                .build();
        /* save */
        credential = credentialRepository.save(credential);
        user.setCredentials(List.of(credential));
        log.info("Updated user password with id {}", user.getId());
        return user;
    }

    @Override
    public User toggleTheme(String id, UserThemeSetDto data, Principal principal) throws UserNotFoundException,
            ForeignUserException, UserAttributeNotFoundException {
        /* check */
        final User user = findById(id);
        if (!user.getUsername().equals(principal.getName())) {
            log.error("Failed to modify user: attempting to modify other user");
            throw new ForeignUserException("Failed to modify user: attempting to modify other user");
        }
        final UserAttribute entity = userAttributeService.update(user.getId(), "theme_dark", data.getThemeDark().toString());
        log.info("Updated theme by updating attribute with id {}", entity.getId());
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
