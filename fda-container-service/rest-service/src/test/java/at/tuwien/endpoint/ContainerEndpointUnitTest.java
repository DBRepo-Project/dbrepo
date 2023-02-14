package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.container.*;
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
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.Principal;
import java.util.List;
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
    @WithAnonymousUser
    public void findById_anonymous2_succeeds() throws DockerClientException, ContainerNotFoundException,
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
    @WithAnonymousUser
    public void delete_anonymous2_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
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
            ContainerNotFoundException {

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

    @Test
    public void findAll_anonymous_succeeds() {

        /* test */
        findAll_generic(null);
    }

    @Test
    @WithAnonymousUser
    public void findAll_anonymous2_succeeds() {

        /* test */
        findAll_generic(null);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void findAll_researcher_succeeds() {

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        findAll_generic(USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void findAll_developer_succeeds() {

        /* mock */
        when(userRepository.findByUsername(USER_2_USERNAME))
                .thenReturn(Optional.of(USER_2));

        /* test */
        findAll_generic(USER_2_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void findAll_dataSteward_succeeds() {

        /* mock */
        when(userRepository.findByUsername(USER_3_USERNAME))
                .thenReturn(Optional.of(USER_3));

        /* test */
        findAll_generic(USER_3_PRINCIPAL);
    }

    @Test
    public void create_anonymous_fails() {
        final ContainerCreateRequestDto request = ContainerCreateRequestDto.builder()
                .name(CONTAINER_1_NAME)
                .repository(IMAGE_1_REPOSITORY)
                .tag(IMAGE_1_TAG)
                .build();

        /* test */
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            create_generic(request, null);
        });
    }

    @Test
    @WithAnonymousUser
    public void create_anonymous2_fails() {
        final ContainerCreateRequestDto request = ContainerCreateRequestDto.builder()
                .name(CONTAINER_1_NAME)
                .repository(IMAGE_1_REPOSITORY)
                .tag(IMAGE_1_TAG)
                .build();

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            create_generic(request, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void create_researcher_succeeds() throws UserNotFoundException, DockerClientException,
            ContainerAlreadyExistsException, ImageNotFoundException {
        final ContainerCreateRequestDto request = ContainerCreateRequestDto.builder()
                .name(CONTAINER_1_NAME)
                .repository(IMAGE_1_REPOSITORY)
                .tag(IMAGE_1_TAG)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        create_generic(request, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void create_researcherEmpty_fails() throws UserNotFoundException, DockerClientException,
            ContainerAlreadyExistsException, ImageNotFoundException {

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        create_generic(null, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void create_developer_fails() {
        final ContainerCreateRequestDto request = ContainerCreateRequestDto.builder()
                .name(CONTAINER_1_NAME)
                .repository(IMAGE_1_REPOSITORY)
                .tag(IMAGE_1_TAG)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_2_USERNAME))
                .thenReturn(Optional.of(USER_2));

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            create_generic(request, USER_2_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void create_dataSteward_fails() {
        final ContainerCreateRequestDto request = ContainerCreateRequestDto.builder()
                .name(CONTAINER_1_NAME)
                .repository(IMAGE_1_REPOSITORY)
                .tag(IMAGE_1_TAG)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_3_USERNAME))
                .thenReturn(Optional.of(USER_3));

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            create_generic(request, USER_3_PRINCIPAL);
        });
    }

    @Test
    public void modify_anonymous_fails() {

        /* test */
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            modify_generic(ContainerActionTypeDto.START, CONTAINER_1_ID, CONTAINER_1, null);
        });
    }

    @Test
    @WithAnonymousUser
    public void modify_anonymous2_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            modify_generic(ContainerActionTypeDto.START, CONTAINER_1_ID, CONTAINER_1, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void modify_researcherStart_succeeds() throws ContainerAlreadyRunningException,
            ContainerAlreadyStoppedException, ContainerNotFoundException, UserNotFoundException, NotAllowedException {

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        modify_generic(ContainerActionTypeDto.START, CONTAINER_1_ID, CONTAINER_1, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void modify_researcherStart_fails() throws ContainerNotFoundException, ContainerAlreadyRunningException {

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));
        doThrow(ContainerAlreadyRunningException.class)
                .when(containerService)
                .start(CONTAINER_1_ID);

        /* test */
        assertThrows(ContainerAlreadyRunningException.class, () -> {
            modify_generic(ContainerActionTypeDto.START, CONTAINER_1_ID, CONTAINER_1, USER_1_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void modify_researcherStop_succeeds() throws ContainerAlreadyRunningException,
            ContainerAlreadyStoppedException, ContainerNotFoundException, UserNotFoundException, NotAllowedException {

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        modify_generic(ContainerActionTypeDto.STOP, CONTAINER_1_ID, CONTAINER_1, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void modify_researcherStop_fails() throws ContainerAlreadyStoppedException, ContainerNotFoundException {

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));
        doThrow(ContainerAlreadyStoppedException.class)
                .when(containerService)
                .stop(CONTAINER_1_ID);


        /* test */
        assertThrows(ContainerAlreadyStoppedException.class, () -> {
            modify_generic(ContainerActionTypeDto.STOP, CONTAINER_1_ID, CONTAINER_1, USER_1_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void modify_developerForeignStart_succeeds() throws UserNotFoundException, ContainerAlreadyRunningException,
            NotAllowedException, ContainerAlreadyStoppedException, ContainerNotFoundException {

        /* mock */
        when(userRepository.findByUsername(USER_2_USERNAME))
                .thenReturn(Optional.of(USER_2));

        /* test */
        modify_generic(ContainerActionTypeDto.START, CONTAINER_1_ID, CONTAINER_1, USER_2_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void modify_developerForeignStop_succeeds() throws UserNotFoundException, ContainerAlreadyRunningException,
            NotAllowedException, ContainerAlreadyStoppedException, ContainerNotFoundException {

        /* mock */
        when(userRepository.findByUsername(USER_2_USERNAME))
                .thenReturn(Optional.of(USER_2));

        /* test */
        modify_generic(ContainerActionTypeDto.STOP, CONTAINER_1_ID, CONTAINER_1, USER_2_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void modify_researcherForeignStart_fails() {

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            modify_generic(ContainerActionTypeDto.START, CONTAINER_2_ID, CONTAINER_2, USER_1_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void modify_dataStewardForeignStop_fails() {

        /* mock */
        when(userRepository.findByUsername(USER_3_USERNAME))
                .thenReturn(Optional.of(USER_3));

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            modify_generic(ContainerActionTypeDto.STOP, CONTAINER_1_ID, CONTAINER_1, USER_3_PRINCIPAL);
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

    public void findAll_generic(Principal principal) {

        /* mock */
        when(containerService.getAll())
                .thenReturn(List.of(CONTAINER_1, CONTAINER_2));

        /* test */
        final ResponseEntity<List<ContainerBriefDto>> response = containerEndpoint.findAll(principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        final List<ContainerBriefDto> body = response.getBody();
        assertEquals(2, body.size());
        final ContainerBriefDto container1 = body.get(0);
        assertEquals(CONTAINER_1_ID, container1.getId());
        assertEquals(CONTAINER_1_NAME, container1.getName());
        assertEquals(CONTAINER_1_INTERNALNAME, container1.getInternalName());
        final ContainerBriefDto container2 = body.get(1);
        assertEquals(CONTAINER_2_ID, container2.getId());
        assertEquals(CONTAINER_2_NAME, container2.getName());
        assertEquals(CONTAINER_2_INTERNALNAME, container2.getInternalName());
    }

    public void create_generic(ContainerCreateRequestDto data, Principal principal) throws UserNotFoundException,
            DockerClientException, ContainerAlreadyExistsException, ImageNotFoundException {

        /* mock */
        when(containerService.create(data, principal))
                .thenReturn(CONTAINER_1);

        /* test */
        final ResponseEntity<ContainerBriefDto> response = containerEndpoint.create(data, principal);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    public void modify_generic(ContainerActionTypeDto data, Long containerId, Container container, Principal principal)
            throws ContainerAlreadyRunningException, ContainerNotFoundException, ContainerAlreadyStoppedException, UserNotFoundException, NotAllowedException {
        final ContainerChangeDto request = ContainerChangeDto.builder()
                .action(data)
                .build();

        /* mock */
        when(containerService.find(containerId))
                .thenReturn(container);
        if (data.equals(ContainerActionTypeDto.START)) {
            when(containerService.start(containerId))
                    .thenReturn(container);
        } else if (data.equals(ContainerActionTypeDto.STOP)) {
            when(containerService.stop(containerId))
                    .thenReturn(container);
        }

        /* test */
        final ResponseEntity<ContainerBriefDto> response = containerEndpoint.modify(containerId, request, principal);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

}
