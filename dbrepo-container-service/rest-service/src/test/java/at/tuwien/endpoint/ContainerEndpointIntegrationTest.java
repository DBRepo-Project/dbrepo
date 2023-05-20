package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.container.*;
import at.tuwien.config.DockerConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.endpoints.ContainerEndpoint;
import at.tuwien.entities.container.Container;
import at.tuwien.exception.*;
import at.tuwien.repository.jpa.ContainerRepository;
import at.tuwien.repository.jpa.ImageRepository;
import at.tuwien.repository.jpa.RealmRepository;
import at.tuwien.repository.jpa.UserRepository;
import at.tuwien.service.impl.ContainerServiceImpl;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@Log4j2
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class ContainerEndpointIntegrationTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @Autowired
    private RealmRepository realmRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContainerEndpoint containerEndpoint;

    @BeforeEach
    public void beforeEach() {
        afterEach();
        /* networks */
        DockerConfig.createAllNetworks();
        /* metadata database */
        realmRepository.save(REALM_DBREPO);
        userRepository.save(USER_1_SIMPLE);
        userRepository.save(USER_2_SIMPLE);
        userRepository.save(USER_3_SIMPLE);
        imageRepository.save(IMAGE_1);
    }

    @AfterEach
    public void afterEach() {
        DockerConfig.removeAllContainers();
        DockerConfig.removeAllNetworks();
    }

    @Test
    @WithAnonymousUser
    public void findAll_anonymousNoLimit_succeeds() throws InterruptedException {

        /* mock */
        DockerConfig.createContainer(null, CONTAINER_1_SIMPLE, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1_SIMPLE);
        containerRepository.save(CONTAINER_1_SIMPLE);

        /* test */
        final ResponseEntity<List<ContainerBriefDto>> response = containerEndpoint.findAll(null, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        final List<ContainerBriefDto> body = response.getBody();
        assertEquals(1, body.size());
        final ContainerBriefDto container0 = body.get(0);
        assertTrue(container0.getRunning());
    }

    @Test
    @WithAnonymousUser
    public void findById_anonymousNotRunning_succeeds() throws DockerClientException, ContainerNotFoundException {

        /* mock */
        DockerConfig.createContainer(null, CONTAINER_1_SIMPLE, CONTAINER_1_ENV);
        containerRepository.save(CONTAINER_1_SIMPLE);

        /* test */
        final ResponseEntity<ContainerDto> response = containerEndpoint.findById(CONTAINER_1_ID);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        final ContainerDto body = response.getBody();
        assertFalse(body.getRunning());
        assertEquals(ContainerStateDto.EXITED, body.getState());
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"modify-container-state"})
    public void modify_foreign_fails() {
        final ContainerChangeDto request = ContainerChangeDto.builder()
                .action(ContainerActionTypeDto.STOP)
                .build();

        /* mock */
        containerRepository.save(CONTAINER_1_SIMPLE);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            containerEndpoint.modify(CONTAINER_1_ID, request, USER_3_PRINCIPAL);
        });
    }

}
