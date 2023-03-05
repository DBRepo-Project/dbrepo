package at.tuwien.config;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.amqp.CreateUserDto;
import at.tuwien.api.amqp.GrantVirtualHostPermissionsDto;
import at.tuwien.dto.AmqpUserBriefDto;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.net.URI;
import java.util.Base64;

@Log4j2
@Configuration
public class RabbitMqConfig extends BaseUnitTest {

    @Primary
    @Bean("junitRestTemplate")
    public RestTemplate restTemplate() {
        final RestTemplate restTemplate = new RestTemplate();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory("http://" + CONTAINER_BROKER_IP + ":15672"));
        return restTemplate;
    }

    @Bean
    public HttpHeaders httpHeaders() {
        final String username = "guest";
        final String password = "guest";
        log.debug("add basic authorization header with username={}, password={}", username, password);
        return httpHeaders(username, password);
    }

    public HttpHeaders httpHeaders(String username, String password) {
        final HttpHeaders headers = new HttpHeaders();
        final String basic = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
        headers.add("Authorization", basic);
        log.debug("add basic authorization header with username={}, password={}", username, password);
        return headers;
    }

    public void addUser(String username, String password, String tags) {
        final RestTemplate restTemplate = restTemplate();
        final CreateUserDto request = CreateUserDto.builder()
                .password(password)
                .tags(tags)
                .build();
        final ResponseEntity<AmqpUserBriefDto> response = restTemplate.exchange("/api/users/" + username, HttpMethod.PUT,
                new HttpEntity<>(request, httpHeaders()), AmqpUserBriefDto.class);
        if (!response.getStatusCode().equals(HttpStatus.CREATED)) {
            log.error("Failed to add user, status is: {}", response.getStatusCode());
            throw new RuntimeException("Failed to add user");
        }
    }

    public void grantAccess(String username) {
        final RestTemplate restTemplate = restTemplate();
        final GrantVirtualHostPermissionsDto request = GrantVirtualHostPermissionsDto.builder()
                .configure(".*")
                .write(".*")
                .read(".*")
                .build();
        final URI uri = URI.create("http://" + CONTAINER_BROKER_IP + ":15672/api/permissions/%2F/" + username);
        final ResponseEntity<AmqpUserBriefDto> response = restTemplate.exchange(uri, HttpMethod.PUT,
                new HttpEntity<>(request, httpHeaders()), AmqpUserBriefDto.class);
        if (!response.getStatusCode().equals(HttpStatus.CREATED)) {
            log.error("Failed to grant access, status is: {}", response.getStatusCode());
            throw new RuntimeException("Failed to grant access");
        }
    }

    public AmqpUserBriefDto whoami(String username, String password) {
        final RestTemplate restTemplate = restTemplate();
        final ResponseEntity<AmqpUserBriefDto> response = restTemplate.exchange("/api/whoami", HttpMethod.GET,
                new HttpEntity<>(null, httpHeaders(username, password)), AmqpUserBriefDto.class);
        if (!response.getStatusCode().equals(HttpStatus.OK)) {
            log.error("Failed to determine whoami, status is: {}", response.getStatusCode());
            throw new RuntimeException("Failed to determine whoami");
        }
        return response.getBody();
    }

}
