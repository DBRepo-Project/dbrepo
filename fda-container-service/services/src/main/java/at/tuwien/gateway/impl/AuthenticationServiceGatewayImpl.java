package at.tuwien.gateway.impl;

import at.tuwien.api.user.UserDto;
import at.tuwien.gateway.AuthenticationServiceGateway;
import at.tuwien.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.servlet.ServletException;

@Slf4j
@Service
public class AuthenticationServiceGatewayImpl implements AuthenticationServiceGateway {

    private final UserMapper userMapper;
    private final RestTemplate restTemplate;

    @Autowired
    public AuthenticationServiceGatewayImpl(UserMapper userMapper, RestTemplate restTemplate) {
        this.userMapper = userMapper;
        this.restTemplate = restTemplate;
    }

    @Override
    public UserDetails validate(String token) throws ServletException {
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        try {
            final ResponseEntity<UserDto> response = restTemplate.exchange("/api/auth", HttpMethod.PUT,
                    new HttpEntity<>(null, headers), UserDto.class);
            if (!response.getStatusCode().equals(HttpStatus.ACCEPTED)) {
                log.error("Failed to validate token with status code {}", response.getStatusCode());
                throw new ServletException("Failed to validate token");
            }
            final UserDetails dto = userMapper.userDtoToUserDetailsDto(response.getBody());
            log.trace("gateway authenticated user {}", dto);
            return dto;
        } catch (HttpStatusCodeException e) {
            log.error("Failed to validate token with status code {}", e.getStatusCode());
            throw new ServletException("Failed to validate token", e);
        }
    }

}
