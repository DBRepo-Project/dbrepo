package at.tuwien.service.impl;

import at.tuwien.api.auth.JwtResponseDto;
import at.tuwien.api.auth.LoginRequestDto;
import at.tuwien.api.user.UserModifyPasswordDto;
import at.tuwien.auth.JwtUtils;
import at.tuwien.config.MailConfig;
import at.tuwien.entities.user.Token;
import at.tuwien.entities.user.User;
import at.tuwien.exception.TokenRevokedException;
import at.tuwien.exception.UserEmailNotVerifiedException;
import at.tuwien.exception.UserNotFoundException;
import at.tuwien.mapper.AuthenticationMapper;
import at.tuwien.mapper.UserMapper;
import at.tuwien.repositories.TokenRepository;
import at.tuwien.service.AuthenticationService;
import at.tuwien.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.Instant;
import java.util.Optional;


@Slf4j
@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private final JwtUtils jwtUtils;
    private final UserMapper userMapper;
    private final MailConfig mailConfig;
    private final UserService userService;
    private final TokenRepository tokenRepository;
    private final AuthenticationMapper authenticationMapper;
    private final AuthenticationManager authenticationManager;

    @Autowired
    public AuthenticationServiceImpl(JwtUtils jwtUtils, UserMapper userMapper, MailConfig mailConfig,
                                     UserService userService, TokenRepository tokenRepository,
                                     AuthenticationMapper authenticationMapper,
                                     AuthenticationManager authenticationManager) {
        this.jwtUtils = jwtUtils;
        this.userMapper = userMapper;
        this.mailConfig = mailConfig;
        this.userService = userService;
        this.tokenRepository = tokenRepository;
        this.authenticationMapper = authenticationMapper;
        this.authenticationManager = authenticationManager;
    }

    @Override
    @Transactional(readOnly = true)
    public JwtResponseDto authenticate(LoginRequestDto data) throws UserEmailNotVerifiedException,
            UserNotFoundException {
        final User user = userService.findByUsername(data.getUsername());
        if (mailConfig.getMailVerify() && !user.getEmailVerified()) {
            log.error("E-Mail not verified for username {}", data.getUsername());
            throw new UserEmailNotVerifiedException("E-Mail not verified");
        }
        final UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(data.getUsername(),
                data.getPassword());
        final Authentication authentication = authenticationManager.authenticate(token);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        final JwtResponseDto response = userMapper.principalToJwtResponseDto(authentication.getPrincipal());
        response.setToken(jwtUtils.generateJwtToken(data.getUsername()));
        return response;
    }

    @Override
    @Transactional
    public void verifyToken(String authorization) throws TokenRevokedException {
        final String hash = authenticationMapper.authorizationToTokenHash(authorization);
        final Optional<Token> optional = tokenRepository.findByTokenHash(hash);
        if (optional.isEmpty()) {
            log.trace("token with hash {} is not a developer token, skip update", hash);
            return;
        }
        final Token token = optional.get();
        if (token.getDeleted() != null) {
            log.warn("Token with hash {} is marked as revoked", hash);
            throw new TokenRevokedException("Token is marked as revoked");
        }
        token.setLastUsed(Instant.now());
        tokenRepository.save(token);
        log.info("Updated token usage of token with hash {}", hash);
    }

    @Override
    @Transactional(readOnly = true)
    public JwtResponseDto authenticate(UserModifyPasswordDto data) throws UserEmailNotVerifiedException,
            UserNotFoundException {
        final User user = userService.findByUsername(data.getUsername());
        if (mailConfig.getMailVerify() && !user.getEmailVerified()) {
            log.error("E-Mail not verified for username {}", data.getUsername());
            throw new UserEmailNotVerifiedException("E-Mail not verified");
        }
        final UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(data.getUsername(),
                data.getPassword());
        final Authentication authentication = authenticationManager.authenticate(token);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        final JwtResponseDto response = userMapper.principalToJwtResponseDto(authentication.getPrincipal());
        response.setToken(jwtUtils.generateJwtToken(data.getUsername()));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public JwtResponseDto renew(Principal principal) {
        final UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) principal;
        final JwtResponseDto response = userMapper.principalToJwtResponseDto(token.getPrincipal());
        response.setToken(jwtUtils.generateJwtToken(principal.getName()));
        return response;
    }
}
