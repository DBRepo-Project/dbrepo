package at.tuwien.endpoints;

import at.tuwien.test.AbstractUnitTest;
import at.tuwien.api.container.ContainerBriefDto;
import at.tuwien.api.container.ContainerCreateDto;
import at.tuwien.api.container.ContainerDto;
import at.tuwien.entities.container.Container;
import at.tuwien.exception.*;
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
public class ContainerEndpointUnitTest extends AbstractUnitTest {

    @MockBean
    private ContainerServiceImpl containerService;

    @Autowired
    private ContainerEndpoint containerEndpoint;

    @Test
    @WithAnonymousUser
    public void findById_anonymous_succeeds() throws ContainerNotFoundException {

        /* test */
        findById_generic(CONTAINER_1_ID, CONTAINER_1, null, false);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"find-container"})
    public void findById_hasRole_succeeds() throws ContainerNotFoundException {

        /* test */
        findById_generic(CONTAINER_1_ID, CONTAINER_1, USER_1_PRINCIPAL, false);
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void findById_noRole_succeeds() throws ContainerNotFoundException {

        /* test */
        findById_generic(CONTAINER_1_ID, CONTAINER_1, USER_4_PRINCIPAL, false);
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"admin"})
    public void findById_admin_succeeds() throws ContainerNotFoundException {

        /* test */
        findById_generic(CONTAINER_1_ID, CONTAINER_1, USER_LOCAL_ADMIN_PRINCIPAL, true);
    }

    @Test
    @WithAnonymousUser
    public void delete_anonymous_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            delete_generic(CONTAINER_1_ID, CONTAINER_1);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME)
    public void delete_noRole_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            delete_generic(CONTAINER_1_ID, CONTAINER_1);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"delete-container"})
    public void delete_hasRole_succeeds() throws ContainerNotFoundException {

        /* test */
        delete_generic(CONTAINER_1_ID, CONTAINER_1);
    }

    @Test
    @WithAnonymousUser
    public void findAll_anonymous_succeeds() {

        /* test */
        findAll_generic(null);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"find-containers"})
    public void findAll_hasRole_succeeds() {

        /* test */
        findAll_generic(null);
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void findAll_noRole_succeeds() {

        /* test */
        findAll_generic(null);
    }

    @Test
    @WithAnonymousUser
    public void create_anonymous_fails() {
        final ContainerCreateDto request = ContainerCreateDto.builder()
                .name(CONTAINER_1_NAME)
                .imageId(IMAGE_1_ID)
                .build();

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            create_generic(request);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-container"})
    public void create_hasRole_succeeds() throws ContainerAlreadyExistsException, ImageNotFoundException {
        final ContainerCreateDto request = ContainerCreateDto.builder()
                .name(CONTAINER_1_NAME)
                .imageId(IMAGE_1_ID)
                .build();

        /* test */
        create_generic(request);
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void create_noRole_fails() {
        final ContainerCreateDto request = ContainerCreateDto.builder()
                .name(CONTAINER_1_NAME)
                .imageId(IMAGE_1_ID)
                .build();

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            create_generic(request);
        });
    }

    @Test
    @WithAnonymousUser
    public void findAll_anonymousNoLimit_succeeds() {

        /* test */
        findAll_generic(null);
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    public void findById_generic(Long containerId, Container container, Principal principal, Boolean isAdmin)
            throws ContainerNotFoundException {

        /* mock */
        when(containerService.find(containerId))
                .thenReturn(container);

        /* test */
        final ResponseEntity<ContainerDto> response = containerEndpoint.findById(containerId, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        if (isAdmin) {
            assertNotNull(response.getHeaders());
            final List<String> xUsername = response.getHeaders().get("X-Username");
            assertNotNull(xUsername);
            assertEquals(CONTAINER_1_PRIVILEGED_USERNAME, xUsername.get(0));
            final List<String> xPassword = response.getHeaders().get("X-Password");
            assertNotNull(xPassword);
            assertEquals(CONTAINER_1_PRIVILEGED_PASSWORD, xPassword.get(0));
        }
    }

    public void delete_generic(Long containerId, Container container) throws ContainerNotFoundException {

        /* mock */
        when(containerService.find(containerId))
                .thenReturn(container);
        doNothing()
                .when(containerService)
                .remove(CONTAINER_1);

        /* test */
        final ResponseEntity<?> response = containerEndpoint.delete(containerId);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNull(response.getBody());
    }

    public void findAll_generic(Integer limit) {

        /* mock */
        when(containerService.getAll(limit))
                .thenReturn(List.of(CONTAINER_1, CONTAINER_2));

        /* test */
        final ResponseEntity<List<ContainerBriefDto>> response = containerEndpoint.findAll(limit);
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

    public void create_generic(ContainerCreateDto data) throws ContainerAlreadyExistsException, ImageNotFoundException {

        /* mock */
        when(containerService.create(data))
                .thenReturn(CONTAINER_1);

        /* test */
        final ResponseEntity<ContainerDto> response = containerEndpoint.create(data);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

}
