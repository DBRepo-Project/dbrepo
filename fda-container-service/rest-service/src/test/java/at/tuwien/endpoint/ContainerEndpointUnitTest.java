package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.container.ContainerDto;
import at.tuwien.api.container.ContainerStateDto;
import at.tuwien.config.DockerUtil;
import at.tuwien.config.ReadyConfig;
import at.tuwien.endpoints.ContainerEndpoint;
import at.tuwien.entities.container.Container;
import at.tuwien.exception.*;
import at.tuwien.repository.jpa.UserRepository;
import at.tuwien.service.impl.ContainerServiceImpl;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.Principal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Log4j2
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class ContainerEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private ContainerServiceImpl containerService;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private ContainerEndpoint containerEndpoint;

    @Autowired
    private DockerUtil dockerUtil;

    @Test
    public void findById_anonymous_succeeds() throws DockerClientException, ContainerNotFoundException,
            ContainerNotRunningException {

        /* test */
        findById_generic(CONTAINER_1_ID, CONTAINER_1);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void findById_researcher_succeeds() throws DockerClientException, ContainerNotFoundException,
            ContainerNotRunningException {

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        findById_generic(CONTAINER_1_ID, CONTAINER_1);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void findById_developer_succeeds() throws DockerClientException, ContainerNotFoundException,
            ContainerNotRunningException {

        /* mock */
        when(userRepository.findByUsername(USER_2_USERNAME))
                .thenReturn(Optional.of(USER_2));

        /* test */
        findById_generic(CONTAINER_1_ID, CONTAINER_1);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void findById_dataSteward_succeeds() throws DockerClientException, ContainerNotFoundException,
            ContainerNotRunningException {

        /* mock */
        when(userRepository.findByUsername(USER_3_USERNAME))
                .thenReturn(Optional.of(USER_3));

        /* test */
        findById_generic(CONTAINER_1_ID, CONTAINER_1);
    }

    @Test
    public void delete_anonymous_fails() {

        /* test */
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            delete_generic(CONTAINER_1_ID, CONTAINER_1, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void delete_researcher_fails() {

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            delete_generic(CONTAINER_1_ID, CONTAINER_1, USER_1_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void delete_developer_succeeds() throws ContainerStillRunningException, ContainerAlreadyRemovedException,
            ContainerNotFoundException, NotAllowedException {

        /* mock */
        when(userRepository.findByUsername(USER_2_USERNAME))
                .thenReturn(Optional.of(USER_2));

        /* test */
        delete_generic(CONTAINER_1_ID, CONTAINER_1, USER_2_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void delete_dataSteward_fails() {

        /* mock */
        when(userRepository.findByUsername(USER_3_USERNAME))
                .thenReturn(Optional.of(USER_3));

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            delete_generic(CONTAINER_1_ID, CONTAINER_1, USER_3_PRINCIPAL);
        });
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    public void findById_generic(Long containerId, Container container) throws DockerClientException,
            ContainerNotFoundException, ContainerNotRunningException {

        /* mock */
        when(containerService.find(containerId))
                .thenReturn(container);
        when(containerService.inspect(containerId))
                .thenReturn(container);

        /* test */
        final ResponseEntity<ContainerDto> response = containerEndpoint.findById(containerId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        final ContainerDto dto = response.getBody();
        assertEquals(ContainerStateDto.RUNNING, dto.getState());
    }

    public void delete_generic(Long containerId, Container container, Principal principal) throws ContainerNotFoundException,
            ContainerStillRunningException, ContainerAlreadyRemovedException {

        /* mock */
        when(containerService.find(containerId))
                .thenReturn(container);
        doNothing()
                .when(containerService)
                .remove(CONTAINER_1_ID);

        /* test */
        final ResponseEntity<?> response = containerEndpoint.delete(containerId, principal);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNull(response.getBody());
    }

}
