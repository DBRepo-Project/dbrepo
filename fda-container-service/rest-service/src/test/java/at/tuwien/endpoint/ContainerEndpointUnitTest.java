package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.container.*;
import at.tuwien.auth.PermissionEvaluatorImpl;
import at.tuwien.config.ReadyConfig;
import at.tuwien.endpoints.ContainerEndpoint;
import at.tuwien.exception.*;
import at.tuwien.repository.jpa.ContainerRepository;
import at.tuwien.repository.jpa.ImageRepository;
import at.tuwien.service.impl.ContainerServiceImpl;
import org.apache.http.auth.BasicUserPrincipal;
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
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class ContainerEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private ImageRepository imageRepository;

    @MockBean
    private ContainerRepository containerRepository;

    @MockBean
    private PermissionEvaluatorImpl permissionEvaluator;

    @Autowired
    private ContainerEndpoint containerEndpoint;

    @Autowired
    private ContainerServiceImpl containerService;

    @Test
    public void listAllDatabases_succeeds() {
        when(containerService.getAll())
                .thenReturn(List.of(CONTAINER_1));

        final ResponseEntity<List<ContainerBriefDto>> response = containerEndpoint.findAll(null);

        /* test */
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, Objects.requireNonNull(response.getBody()).size());
    }

    @Test
    @WithMockUser(roles = "RESEARCHER")
    public void create_succeeds() throws ImageNotFoundException, DockerClientException, UserNotFoundException, ContainerAlreadyExistsException {
        final ContainerCreateRequestDto request = ContainerCreateRequestDto.builder()
                .name(CONTAINER_1_NAME)
                .repository(IMAGE_1.getRepository())
                .tag(IMAGE_1.getTag())
                .build();
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);

        /* mock */
        when(containerService.create(request, principal))
                .thenReturn(CONTAINER_1);

        final ResponseEntity<ContainerBriefDto> response = containerEndpoint.create(request, principal);

        /* test */
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(CONTAINER_1_NAME, Objects.requireNonNull(response.getBody()).getName());
    }

    @Test
    @WithMockUser(roles = "RESEARCHER")
    public void create_noImage_fails() {
        final ContainerCreateRequestDto request = ContainerCreateRequestDto.builder()
                .name(CONTAINER_1_NAME)
                .repository(IMAGE_1_REPOSITORY)
                .tag(IMAGE_1_TAG)
                .build();
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);

        /* mock */
        when(imageRepository.findByRepositoryAndTag(IMAGE_1_REPOSITORY, IMAGE_1_TAG))
                .thenReturn(Optional.empty());

        assertThrows(ImageNotFoundException.class, () -> {
            containerEndpoint.create(request, principal);
        });
    }

    @Test
    public void create_noAuth_fails() {
        final ContainerCreateRequestDto request = ContainerCreateRequestDto.builder()
                .name(CONTAINER_1_NAME)
                .repository(IMAGE_1_REPOSITORY)
                .tag(IMAGE_1_TAG)
                .build();
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);

        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            containerEndpoint.create(request, principal);
        });
    }

    @Test
    public void findById_succeeds() throws ContainerNotFoundException, DockerClientException, ContainerNotRunningException {
        when(containerService.find(CONTAINER_1_ID))
                .thenReturn(CONTAINER_1);

        /* test */
        final ResponseEntity<ContainerDto> response = containerEndpoint.findById(CONTAINER_1_ID);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void findById_notFound_fails() throws ContainerNotFoundException, DockerClientException, ContainerNotRunningException {
        doThrow(ContainerNotFoundException.class)
                .when(containerService)
                .inspect(CONTAINER_1_ID);

        /* test */
        assertThrows(ContainerNotFoundException.class, () -> {
            containerEndpoint.findById(CONTAINER_1_ID);
        });
    }

    @Test
    public void findById_docker_fails() throws ContainerNotFoundException, DockerClientException, ContainerNotRunningException {
        doThrow(DockerClientException.class)
                .when(containerService)
                .inspect(CONTAINER_1_ID);

        /* test */
        assertThrows(DockerClientException.class, () -> {
            containerEndpoint.findById(CONTAINER_1_ID);
        });
    }

    @Test
    public void findById_notRunning_fails() throws ContainerNotFoundException, DockerClientException, ContainerNotRunningException {
        doThrow(ContainerNotRunningException.class)
                .when(containerService)
                .inspect(CONTAINER_1_ID);

        /* test */
        assertThrows(ContainerNotRunningException.class, () -> {
            containerEndpoint.findById(CONTAINER_1_ID);
        });
    }

    @Test
    @WithMockUser(roles = "RESEARCHER")
    public void modify_start_succeeds() throws DockerClientException, ContainerNotFoundException {
        final ContainerChangeDto request = ContainerChangeDto.builder()
                .action(ContainerActionTypeDto.START)
                .build();
        when(containerService.start(CONTAINER_1_ID))
                .thenReturn(CONTAINER_1);

        /* test */
        final ResponseEntity<ContainerBriefDto> response = containerEndpoint.modify(CONTAINER_1_ID, request);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertEquals(CONTAINER_1_ID, Objects.requireNonNull(response.getBody()).getId());
    }

    @Test
    @WithMockUser(roles = "RESEARCHER")
    public void modify_stop_succeeds() throws DockerClientException, ContainerNotFoundException, ContainerStillRunningException {
        final ContainerChangeDto request = ContainerChangeDto.builder()
                .action(ContainerActionTypeDto.STOP)
                .build();
        when(containerService.stop(CONTAINER_1_ID))
                .thenReturn(CONTAINER_1);

        /* test */
        final ResponseEntity<ContainerBriefDto> response = containerEndpoint.modify(CONTAINER_1_ID, request);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertEquals(CONTAINER_1_ID, Objects.requireNonNull(response.getBody()).getId());
    }

    @Test
    @WithMockUser(roles = "RESEARCHER")
    public void modify_startDocker_fails() throws DockerClientException, ContainerNotFoundException {
        final ContainerChangeDto request = ContainerChangeDto.builder()
                .action(ContainerActionTypeDto.START)
                .build();
        when(containerService.start(CONTAINER_1_ID))
                .thenThrow(DockerClientException.class);

        /* test */
        assertThrows(DockerClientException.class, () -> {
            containerEndpoint.modify(CONTAINER_1_ID, request);
        });
    }

    @Test
    @WithMockUser(roles = "RESEARCHER")
    public void modify_stopDocker_fails() throws DockerClientException, ContainerNotFoundException {
        final ContainerChangeDto request = ContainerChangeDto.builder()
                .action(ContainerActionTypeDto.STOP)
                .build();
        when(containerService.stop(CONTAINER_1_ID))
                .thenThrow(DockerClientException.class);

        /* test */
        assertThrows(DockerClientException.class, () -> {
            containerEndpoint.modify(CONTAINER_1_ID, request);
        });
    }

    @Test
    @WithMockUser(roles = "RESEARCHER")
    public void modify_stopNoContainer_fails() throws DockerClientException, ContainerNotFoundException {
        final ContainerChangeDto request = ContainerChangeDto.builder()
                .action(ContainerActionTypeDto.STOP)
                .build();
        when(containerService.stop(CONTAINER_1_ID))
                .thenThrow(ContainerNotFoundException.class);

        /* test */
        assertThrows(ContainerNotFoundException.class, () -> {
            containerEndpoint.modify(CONTAINER_1_ID, request);
        });
    }

    @Test
    @WithMockUser(roles = "DEVELOPER")
    public void delete_noContainer_fails() {
        doReturn(true)
                .when(permissionEvaluator)
                .hasPermission(any(), eq(CONTAINER_1_ID), eq("DELETE_CONTAINER"));

        /* test */
        assertThrows(ContainerNotFoundException.class, () -> {
            containerEndpoint.delete(CONTAINER_1_ID);
        });
    }

    @Test
    @WithMockUser(roles = "DEVELOPER")
    public void delete_success() throws DockerClientException, ContainerStillRunningException, ContainerNotFoundException {
        doReturn(true)
                .when(permissionEvaluator)
                .hasPermission(any(), eq(CONTAINER_1_ID), eq("DELETE_CONTAINER"));

        /* mock */
        when(containerRepository.findById(CONTAINER_1_ID))
                .thenReturn(Optional.of(CONTAINER_1));

        /* test */
        final ResponseEntity<?> response = containerEndpoint.delete(CONTAINER_1_ID);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @WithMockUser(roles = "RESEARCHER")
    public void delete_notDeveloper_fails() {
        doReturn(true)
                .when(permissionEvaluator)
                .hasPermission(any(), eq(CONTAINER_1_ID), eq("DELETE_CONTAINER"));

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            containerEndpoint.delete(CONTAINER_1_ID);
        });
    }

    @Test
    @WithMockUser(roles = "DEVELOPER")
    public void delete_docker_fails() {
        doReturn(true)
                .when(permissionEvaluator)
                .hasPermission(any(), eq(CONTAINER_1_ID), eq("DELETE_CONTAINER"));

        /* test */
        assertThrows(DockerClientException.class, () -> {
            containerEndpoint.delete(CONTAINER_1_ID);
        });
    }

    @Test
    @WithMockUser(roles = "DEVELOPER")
    public void delete_dockerStillRunning_fails() {
        doReturn(true)
                .when(permissionEvaluator)
                .hasPermission(any(), eq(CONTAINER_1_ID), eq("DELETE_CONTAINER"));

        /* test */
        assertThrows(ContainerStillRunningException.class, () -> {
            containerEndpoint.delete(CONTAINER_1_ID);
        });
    }
}
