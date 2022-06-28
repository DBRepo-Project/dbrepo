package at.tuwien.gateway.impl;

import at.tuwien.api.user.UserDto;
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
    private final RestTemplate gatewayRestTemplate;

    @Autowired
    public AuthenticationServiceGatewayImpl(UserMapper userMapper, RestTemplate gatewayRestTemplate) {
        this.userMapper = userMapper;
        this.gatewayRestTemplate = gatewayRestTemplate;
    }

    @Override
    public UserDetails validate(String token) {
        final HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        final ResponseEntity<UserDto> response = gatewayRestTemplate.exchange("/api/auth", HttpMethod.PUT,
                new HttpEntity<>(null, headers), UserDto.class);
        return userMapper.userDtoToUserDetailsDto(response.getBody());
    }

}
