package at.tuwien.endpoints;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.container.ContainerBriefDto;
import at.tuwien.api.container.ContainerCreateRequestDto;
import at.tuwien.api.container.ContainerDto;
import at.tuwien.entities.container.Container;
import at.tuwien.exception.*;
import at.tuwien.repository.mdb.ImageRepository;
import at.tuwien.service.impl.ContainerServiceImpl;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.Principal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@Log4j2
@ExtendWith(SpringExtension.class)
@SpringBootTest
@MockAmqp
@MockOpensearch
public class ContainerEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private ContainerServiceImpl containerService;

    @MockBean
    private ImageRepository imageRepository;

    @Autowired
    private ContainerEndpoint containerEndpoint;

    @Test
    @WithAnonymousUser
    public void findById_anonymous_succeeds() throws ContainerNotFoundException {

        /* test */
        findById_generic(CONTAINER_1_ID, CONTAINER_1);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"find-container"})
    public void findById_hasRole_succeeds() throws ContainerNotFoundException {

        /* test */
        findById_generic(CONTAINER_1_ID, CONTAINER_1);
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void findById_noRole_succeeds() throws ContainerNotFoundException {

        /* test */
        findById_generic(CONTAINER_1_ID, CONTAINER_1);
    }

    @Test
    @WithAnonymousUser
    public void delete_anonymous_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            delete_generic(CONTAINER_1_ID, CONTAINER_1, null);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME)
    public void delete_noRole_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            delete_generic(CONTAINER_1_ID, CONTAINER_1, USER_4_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"delete-container"})
    public void delete_hasRole_succeeds() throws ContainerStillRunningException, ContainerAlreadyRemovedException,
            ContainerNotFoundException {

        /* test */
        delete_generic(CONTAINER_1_ID, CONTAINER_1, USER_2_PRINCIPAL);
    }

    @Test
    @WithAnonymousUser
    public void findAll_anonymous_succeeds() {

        /* test */
        findAll_generic(null, null);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"find-containers"})
    public void findAll_hasRole_succeeds() {

        /* test */
        findAll_generic(USER_1_PRINCIPAL, null);
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void findAll_noRole_succeeds() {

        /* test */
        findAll_generic(USER_4_PRINCIPAL, null);
    }

    @Test
    @WithAnonymousUser
    public void create_anonymous_fails() {
        final ContainerCreateRequestDto request = ContainerCreateRequestDto.builder()
                .name(CONTAINER_1_NAME)
                .imageId(IMAGE_1_ID)
                .build();

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            create_generic(request, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-container"})
    public void create_hasRole_succeeds() throws ContainerAlreadyExistsException, ImageNotFoundException {
        final ContainerCreateRequestDto request = ContainerCreateRequestDto.builder()
                .name(CONTAINER_1_NAME)
                .imageId(IMAGE_1_ID)
                .build();

        /* test */
        create_generic(request, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void create_noRole_fails() {
        final ContainerCreateRequestDto request = ContainerCreateRequestDto.builder()
                .name(CONTAINER_1_NAME)
                .imageId(IMAGE_1_ID)
                .build();

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            create_generic(request, USER_4_PRINCIPAL);
        });
    }

    @Test
    @WithAnonymousUser
    public void findAll_anonymousNoLimit_succeeds() {

        /* test */
        findAll_generic(null, null);
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    public void findById_generic(Long containerId, Container container)
            throws ContainerNotFoundException {

        /* mock */
        when(containerService.find(containerId))
                .thenReturn(container);

        /* test */
        final ResponseEntity<ContainerDto> response = containerEndpoint.findById(containerId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
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

    public void findAll_generic(Principal principal, Integer limit) {

        /* mock */
        when(containerService.getAll(limit))
                .thenReturn(List.of(CONTAINER_1, CONTAINER_2));

        /* test */
        final ResponseEntity<List<ContainerBriefDto>> response = containerEndpoint.findAll(principal, limit);
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

    public void create_generic(ContainerCreateRequestDto data, Principal principal) throws ContainerAlreadyExistsException, ImageNotFoundException {

        /* mock */
        when(containerService.create(data, principal))
                .thenReturn(CONTAINER_1);

        /* test */
        final ResponseEntity<ContainerBriefDto> response = containerEndpoint.create(data, principal);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

}
