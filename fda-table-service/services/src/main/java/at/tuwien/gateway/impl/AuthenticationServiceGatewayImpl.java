package at.tuwien.gateway.impl;

import at.tuwien.api.auth.JwtResponseDto;
import at.tuwien.api.auth.LoginRequestDto;
import at.tuwien.api.user.UserDto;
import at.tuwien.config.SecurityConfig;
import at.tuwien.gateway.AuthenticationServiceGateway;
import at.tuwien.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class AuthenticationServiceGatewayImpl implements AuthenticationServiceGateway {

    private final UserMapper userMapper;
    private final RestTemplate restTemplate;
    private final SecurityConfig securityConfig;

    @Autowired
    public AuthenticationServiceGatewayImpl(UserMapper userMapper, RestTemplate restTemplate,
                                            SecurityConfig securityConfig) {
        this.userMapper = userMapper;
        this.restTemplate = restTemplate;
        this.securityConfig = securityConfig;
    }

    @Override
    public UserDetails validate(String token) {
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        final ResponseEntity<UserDto> response = restTemplate.exchange("/api/auth", HttpMethod.PUT,
                new HttpEntity<>("", headers), UserDto.class);
        return userMapper.userDtoToUserDetailsDto(response.getBody());
    }

    @Override
    public JwtResponseDto obtain() {
        final LoginRequestDto request = LoginRequestDto.builder()
                .username(securityConfig.getSystemUsername())
                .password(securityConfig.getSystemPassword())
                .build();
        log.debug("send login request {}", request);
        final ResponseEntity<JwtResponseDto> response = restTemplate.exchange("/api/auth", HttpMethod.POST,
                new HttpEntity<>(request), JwtResponseDto.class);
        return response.getBody();
    }

}
