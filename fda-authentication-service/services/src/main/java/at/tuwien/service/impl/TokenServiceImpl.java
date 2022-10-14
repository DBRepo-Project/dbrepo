package at.tuwien.service.impl;

import at.tuwien.auth.JwtUtils;
import at.tuwien.entities.user.Token;
import at.tuwien.entities.user.User;
import at.tuwien.exception.TokenNotEligableException;
import at.tuwien.exception.TokenNotFoundException;
import at.tuwien.exception.UserNotFoundException;
import at.tuwien.repositories.TokenRepository;
import at.tuwien.service.TokenService;
import at.tuwien.service.UserService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Log4j2
@Service
public class TokenServiceImpl implements TokenService {

    private final JwtUtils jwtUtils;
    private final UserService userService;
    private final TokenRepository tokenRepository;

    @Autowired
    public TokenServiceImpl(UserService userService, JwtUtils jwtUtils, TokenRepository tokenRepository) {
        this.userService = userService;
        this.jwtUtils = jwtUtils;
        this.tokenRepository = tokenRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Token> findAll(Principal principal) throws UserNotFoundException {
        final User user = userService.findByUsername(principal.getName());
        return tokenRepository.findMine(user.getId());
    }

    @Override
    @Transactional
    public Token create(Principal principal) throws UserNotFoundException, TokenNotEligableException {
        final User user = userService.findByUsername(principal.getName());
        if (user.getRoles().stream().noneMatch(r -> r.name().equals("ROLE_RESEARCHER"))) {
            log.error("User is not researcher");
            throw new TokenNotEligableException("User is not researcher");
        }
        final String token = jwtUtils.generateDeveloperJwtToken(principal.getName());
        final String tokenHash = DigestUtils.sha256Hex(token);
        /* save */
        final Token tmp = Token.builder()
                .tokenHash(tokenHash)
                .creator(user.getId())
                .build();
        final Token entity = tokenRepository.save(tmp);
        entity.setToken(token);
        log.info("Created token with id {}", entity.getId());
        log.debug("created token {}", entity);
        return entity;
    }

    @Override
    @Transactional(readOnly = true)
    public Token findOne(String tokenHash) throws TokenNotFoundException {
        final Optional<Token> optional = tokenRepository.findByTokenHash(tokenHash);
        if (optional.isEmpty()) {
            log.error("Failed to find token with hash {}", tokenHash);
            throw new TokenNotFoundException("Failed to find token");
        }
        return optional.get();
    }

    @Override
    @Transactional
    public void delete(String tokenHash, Principal principal) throws TokenNotFoundException, UserNotFoundException {
        final Token token = findOne(tokenHash);
        final User user = userService.findByUsername(principal.getName());
        if (!token.getCreator().equals(user.getId())) {
            log.error("Attempted to delete foreign token");
            throw new TokenNotFoundException("Attempted to delete foreign token");
        }
        tokenRepository.deleteById(token.getId());
        log.info("Deleted token with id {}", token.getId());
        log.debug("deleted token {}", token);
    }

}
