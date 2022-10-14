package at.tuwien.service.impl;

import at.tuwien.entities.user.TimeSecret;
import at.tuwien.entities.user.User;
import at.tuwien.exception.SecretInvalidException;
import at.tuwien.repositories.TimeSecretRepository;
import at.tuwien.repositories.UserRepository;
import at.tuwien.service.TimeSecretService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Log4j2
@Service
public class TimeSecretServiceImpl implements TimeSecretService {

    private final UserRepository userRepository;
    private final TimeSecretRepository tokenRepository;

    @Autowired
    public TimeSecretServiceImpl(UserRepository userRepository, TimeSecretRepository tokenRepository) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public TimeSecret find(String token) throws SecretInvalidException {
        /* check */
        final Optional<TimeSecret> entity = tokenRepository.findByToken(token);
        if (entity.isEmpty()) {
            log.error("Failed to find valid token");
            log.debug("Failed to find valid token [{}]", token);
            throw new SecretInvalidException("Failed to find valid token");
        }
        return entity.get();
    }

    @Override
    @Transactional
    public TimeSecret create(User user) {
        /* check */
        final TimeSecret token = TimeSecret.builder()
                .processed(false)
                .uid(user.getId())
                .validTo(Instant.now().plus(1, ChronoUnit.DAYS))
                .user(user)
                .token(RandomStringUtils.randomAlphabetic(10))
                .build();
        final TimeSecret out = tokenRepository.save(token);
        log.info("Created token with id {}", out.getId());
        log.debug("created token {}", out);
        return out;
    }

    @Override
    @Transactional
    public User invalidate(String token) throws SecretInvalidException {
        /* check */
        final TimeSecret token1 = find(token);
        /* verify */
        final User user = token1.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);
        log.info("Verified user with username {}", user.getUsername());
        log.debug("Verified user {}", user);
        /* invalidate */
        token1.setProcessed(true);
        final TimeSecret out = tokenRepository.save(token1);
        log.info("Invalidated token with id {}", out.getId());
        log.debug("Invalidated token {}", out);
        return user;
    }

}
