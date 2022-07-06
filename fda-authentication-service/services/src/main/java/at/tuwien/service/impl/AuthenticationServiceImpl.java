package at.tuwien.service.impl;

import at.tuwien.api.auth.JwtResponseDto;
import at.tuwien.api.auth.LoginRequestDto;
import at.tuwien.auth.JwtUtils;
import at.tuwien.entities.user.User;
import at.tuwien.exception.UserEmailNotVerifiedException;
import at.tuwien.exception.UserNotFoundException;
import at.tuwien.mapper.UserMapper;
import at.tuwien.service.AuthenticationService;
import at.tuwien.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.Arrays;


@Slf4j
@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private final JwtUtils jwtUtils;
    private final UserMapper userMapper;
    private final UserService userService;
    private final Environment environment;
    private final AuthenticationManager authenticationManager;

    @Autowired
    public AuthenticationServiceImpl(JwtUtils jwtUtils, UserMapper userMapper,
                                     UserService userService, Environment environment,
                                     AuthenticationManager authenticationManager) {
        this.jwtUtils = jwtUtils;
        this.userMapper = userMapper;
        this.userService = userService;
        this.environment = environment;
        this.authenticationManager = authenticationManager;
    }

    @Override
    @Transactional(readOnly = true)
    public JwtResponseDto authenticate(LoginRequestDto data) throws UserEmailNotVerifiedException,
            UserNotFoundException {
        final User user = userService.findByUsername(data.getUsername());
        if (isProduction() && !user.getEmailVerified()) {
            log.error("E-Mail not verified for username {}", data.getUsername());
            throw new UserEmailNotVerifiedException("E-Mail not verified");
        }
        final UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(data.getUsername(),
                data.getPassword());
        final Authentication authentication = authenticationManager.authenticate(token);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        final JwtResponseDto response = userMapper.principalToJwtResponseDto(authentication.getPrincipal());
        response.setToken(jwtUtils.generateJwtToken(authentication.getPrincipal()));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public JwtResponseDto renew(Principal principal) {
        final UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) principal;
        final JwtResponseDto response = userMapper.principalToJwtResponseDto(token.getPrincipal());
        response.setToken(jwtUtils.generateJwtToken(token.getPrincipal()));
        return response;
    }

    private Boolean isProduction() {
        return Arrays.asList(this.environment.getActiveProfiles()).contains("prod");
    }
}
