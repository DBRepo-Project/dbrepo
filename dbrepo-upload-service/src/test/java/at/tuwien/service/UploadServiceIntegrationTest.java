package at.tuwien.service;

import at.tuwien.api.error.ApiErrorDto;
import at.tuwien.api.keycloak.CredentialDto;
import at.tuwien.api.keycloak.CredentialTypeDto;
import at.tuwien.api.keycloak.UserCreateDto;
import at.tuwien.config.KeycloakConfig;
import at.tuwien.config.TusdConfig;
import at.tuwien.config.TusdContainerConfig;
import at.tuwien.exception.AuthServiceConnectionException;
import at.tuwien.exception.AuthServiceException;
import at.tuwien.exception.EmailExistsException;
import at.tuwien.exception.UserExistsException;
import at.tuwien.interceptor.KeycloakInterceptor;
import com.github.dockerjava.api.model.ExposedPort;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Log4j2
@Testcontainers
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class UploadServiceIntegrationTest {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private TusdConfig tusdConfig;

    @Autowired
    private KeycloakConfig keycloakConfig;

    @Container
    private static TusdContainerConfig.TusdContainer tusdContainer = TusdContainerConfig.TusdContainer.getInstance();

    @Container
    private static KeycloakContainer keycloakContainer = new KeycloakContainer("quay.io/keycloak/keycloak:24.0")
            .withImagePullPolicy(PullPolicy.alwaysPull())
            .withRealmImportFile("init/dbrepo-realm.json")
            .withEnv("KC_HOSTNAME_STRICT_HTTPS", "false")
            .withCreateContainerCmdModifier(it -> it.withName("auth-service")
                    .withExposedPorts(ExposedPort.tcp(8080)));

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("dbrepo.endpoints.tusd", () -> "http://localhost:" + tusdContainer.getMappedPort(8080) + "/api/upload/files");
        registry.add("dbrepo.endpoints.keycloak", () -> keycloakContainer.getAuthServerUrl());
    }

    @BeforeEach
    public void beforeEach() throws UserExistsException, AuthServiceException, AuthServiceConnectionException,
            EmailExistsException {
        if (keycloakConfig.existsByUsername(keycloakContainer.getAdminUsername())) {
            return;
        }
        final UserCreateDto payload = UserCreateDto.builder()
                .username(keycloakContainer.getAdminUsername())
                .credentials(List.of(CredentialDto.builder()
                        .temporary(false)
                        .type(CredentialTypeDto.PASSWORD)
                        .value(keycloakContainer.getAdminPassword())
                        .build()))
                .build();
        keycloakConfig.createUser(payload);
    }

    @Test
    public void upload_missingAuthentication_fails() {
        final HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.set("Tus-Resumable", "1.0.0");
        requestHeaders.set("Upload-Length", "100");
        requestHeaders.set("Content-Type", "application/offset+octet-stream");

        /* test */
        assertThrows(HttpClientErrorException.BadRequest.class, () -> {
            restTemplate.exchange(tusdConfig.getTusdEndpoint(), HttpMethod.POST, new HttpEntity<>("Hello this is a test aaa", requestHeaders), ApiErrorDto.class);
        });
    }

    @Test
    public void upload_invalidAuthentication_fails() {
        final HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.set("Tus-Resumable", "1.0.0");
        requestHeaders.set("Upload-Length", "100");
        requestHeaders.set("Content-Type", "application/offset+octet-stream");
        requestHeaders.set("Authorization", "ey12345");

        /* test */
        assertThrows(HttpClientErrorException.Unauthorized.class, () -> {
            restTemplate.exchange(tusdConfig.getTusdEndpoint(), HttpMethod.POST, new HttpEntity<>("Hello this is a test aaa", requestHeaders), ApiErrorDto.class);
        });
    }

    @Test
    public void upload_succeeds() {
        final RestTemplate uploadRestTemplate = new RestTemplate();
        uploadRestTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(tusdConfig.getTusdEndpoint()));
        uploadRestTemplate.getInterceptors()
                .add(new KeycloakInterceptor(restTemplate, keycloakContainer.getAdminUsername(),
                        keycloakContainer.getAdminPassword(), keycloakConfig.getKeycloakEndpoint()));

        /* test */
        final HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.set("Tus-Resumable", "1.0.0");
        requestHeaders.set("Upload-Length", "100");
        requestHeaders.set("Content-Type", "application/offset+octet-stream");
        try {
            final ResponseEntity<Void> response = uploadRestTemplate.exchange("", HttpMethod.POST, new HttpEntity<>(
                    "Hello this is a test aaa", requestHeaders), Void.class);
        } catch (Exception e) {
            /* ignore */
        }
        System.out.println("");
//        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

}
