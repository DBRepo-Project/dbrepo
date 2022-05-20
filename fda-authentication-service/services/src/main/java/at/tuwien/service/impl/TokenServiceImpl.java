package at.tuwien.service.impl;

import at.tuwien.entities.user.Token;
import at.tuwien.entities.user.User;
import at.tuwien.exception.TokenInvalidException;
import at.tuwien.repositories.TokenRepository;
import at.tuwien.repositories.UserRepository;
import at.tuwien.service.TokenService;
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
public class TokenServiceImpl implements TokenService {

    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;

    @Autowired
    public TokenServiceImpl(UserRepository userRepository, TokenRepository tokenRepository) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Token find(String token) throws TokenInvalidException {
        /* check */
        final Optional<Token> entity = tokenRepository.findByToken(token);
        if (entity.isEmpty()) {
            log.error("Failed to find valid token");
            log.debug("Failed to find valid token [{}]", token);
            throw new TokenInvalidException("Failed to find valid token");
        }
        return entity.get();
    }

    @Override
    @Transactional
    public Token create(User user) {
        /* check */
        final Token token = Token.builder()
                .processed(false)
                .uid(user.getId())
                .validTo(Instant.now().plus(1, ChronoUnit.DAYS))
                .user(user)
                .token(RandomStringUtils.randomAlphabetic(10))
                .build();
        final Token out = tokenRepository.save(token);
        log.info("Created token with id {}", out.getId());
        log.debug("created token {}", out);
        return out;
    }

    @Override
    @Transactional
    public void invalidate(String token) throws TokenInvalidException {
        /* check */
        final Token token1 = find(token);
        /* verify */
        final User user = token1.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);
        log.info("Verified user with username {}", user.getUsername());
        log.debug("Verified user {}", user);
        /* invalidate */
        token1.setProcessed(true);
        final Token out = tokenRepository.save(token1);
        log.info("Invalidated token with id {}", out.getId());
        log.debug("Invalidated token {}", out);
    }

}
