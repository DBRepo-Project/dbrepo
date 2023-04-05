package at.tuwien.service.impl;

import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.entities.auth.Realm;
import at.tuwien.entities.user.Credential;
import at.tuwien.entities.user.User;
import at.tuwien.exception.RemoteUnavailableException;
import at.tuwien.exception.UserAlreadyExistsException;
import at.tuwien.exception.UserNotFoundException;
import at.tuwien.mapper.UserMapper;
import at.tuwien.repository.jpa.CredentialRepository;
import at.tuwien.repository.jpa.UserRepository;
import at.tuwien.service.UserService;
import lombok.extern.log4j.Log4j2;
import org.keycloak.common.util.Base64;
import org.keycloak.common.util.PaddingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
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
    private final CredentialRepository credentialRepository;

    @Autowired
    public UserServiceImpl(UserMapper userMapper, UserRepository userRepository,
                           CredentialRepository credentialRepository) {
        this.userMapper = userMapper;
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
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
    public User create(SignupRequestDto data, Realm realm) throws RemoteUnavailableException, UserNotFoundException,
            UserAlreadyExistsException {
        /* check */
        final Optional<User> optional = userRepository.findByUsername(data.getUsername());
        if (optional.isPresent()) {
            log.error("User with username {} already exists", data.getUsername());
            throw new UserAlreadyExistsException("User with username " + data.getUsername() + " already exists");
        }
        /* create secret */

        /* save */
        final User tmp = userMapper.signupRequestDtoToUser(data);
        tmp.setEmailVerified(false);
        tmp.setEnabled(true);
        tmp.setRealmId(realm.getId());
        tmp.setCreatedTimestamp(Instant.now().toEpochMilli());
        final byte[] salt = getSalt();
        final StringBuilder secretData = new StringBuilder("{\"value\":\"")
                .append(encodedCredential(data.getPassword(), DEFAULT_ITERATIONS, salt, DERIVED_KEY_SIZE))
                .append("\",\"salt\":\"")
                .append(Base64.encodeBytes(salt))
                .append("\",\"additionalParameters\":{}}");
        final Credential entity = Credential.builder()
                .createdDate(Instant.now().toEpochMilli())
                .secretData(secretData.toString())
                .type("password")
                .priority(10)
                .credentialData("{\"hashIterations\":" + DEFAULT_ITERATIONS + ",\"algorithm\":\"" + ID + "\",\"additionalParameters\":{}}")
                .build();
        final User user = userRepository.save(tmp);
        entity.setUserId(user.getId());
        final Credential credential = credentialRepository.save(entity);
        user.setCredentials(List.of(credential));
        log.info("Created user with id {}", user.getId());
        log.debug("created user {}", user);
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
        log.trace("padding: {}", rawPasswordWithPadding);
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
