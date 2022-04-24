package at.tuwien.service.impl;

import at.tuwien.api.auth.JwtResponseDto;
import at.tuwien.api.auth.LoginRequestDto;
import at.tuwien.auth.JwtUtils;
import at.tuwien.mapper.UserMapper;
import at.tuwien.service.AuthenticationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;


@Slf4j
@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private final JwtUtils jwtUtils;
    private final UserMapper userMapper;
    private final AuthenticationManager authenticationManager;

    @Autowired
    public AuthenticationServiceImpl(JwtUtils jwtUtils, UserMapper userMapper,
                                     AuthenticationManager authenticationManager) {
        this.jwtUtils = jwtUtils;
        this.userMapper = userMapper;
        this.authenticationManager = authenticationManager;
    }

    @Override
    @Transactional(readOnly = true)
    public JwtResponseDto authenticate(LoginRequestDto data) {
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
        final Authentication authentication = authenticationManager.authenticate(token);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        final JwtResponseDto response = userMapper.principalToJwtResponseDto(authentication.getPrincipal());
        response.setToken(jwtUtils.generateJwtToken(authentication.getPrincipal()));
        return response;
    }
}
