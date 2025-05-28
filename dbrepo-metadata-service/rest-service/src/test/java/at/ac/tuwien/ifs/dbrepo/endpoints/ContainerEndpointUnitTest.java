package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.container.ContainerBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.container.ContainerDto;
import at.ac.tuwien.ifs.dbrepo.core.api.container.CreateContainerDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.container.Container;
import at.ac.tuwien.ifs.dbrepo.core.exception.ContainerAlreadyExistsException;
import at.ac.tuwien.ifs.dbrepo.core.exception.ContainerNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.ImageNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.service.impl.ContainerServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class ContainerEndpointUnitTest extends BaseTest {

    @MockBean
    private ContainerServiceImpl containerService;

    @Autowired
    private ContainerEndpoint containerEndpoint;

    @Test
    @WithAnonymousUser
    public void findById_anonymous_succeeds() throws ContainerNotFoundException {

        /* test */
        findById_generic(CONTAINER_1_ID, CONTAINER_1, null);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void findById_succeeds() throws ContainerNotFoundException {

        /* test */
        findById_generic(CONTAINER_1_ID, CONTAINER_1, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME)
    public void findById_system_succeeds() throws ContainerNotFoundException {

        /* test */
        final ResponseEntity<ContainerDto> response = findById_generic(CONTAINER_1_ID, CONTAINER_1, USER_LOCAL_ADMIN_PRINCIPAL);
        final HttpHeaders headers = response.getHeaders();
        assertEquals(List.of(CONTAINER_1_HOST), headers.get("X-Host"));
        assertEquals(List.of("" + CONTAINER_1_PORT), headers.get("X-Port"));
        assertEquals(List.of(CONTAINER_1_PRIVILEGED_USERNAME), headers.get("X-Username"));
        assertEquals(List.of(CONTAINER_1_PRIVILEGED_PASSWORD), headers.get("X-Password"));
        assertEquals(List.of(IMAGE_1_JDBC_METHOD), headers.get("X-Jdbc-Method"));
        assertEquals(List.of("X-Username X-Password X-Jdbc-Method X-Host X-Port"), headers.get("Access-Control-Expose-Headers"));
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
        final CreateContainerDto request = CreateContainerDto.builder()
                .name(CONTAINER_1_NAME)
                .imageId(CONTAINER_1_ID)
                .build();

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            create_generic(request);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-container"})
    public void create_hasRole_succeeds() throws ContainerAlreadyExistsException, ImageNotFoundException {
        final CreateContainerDto request = CreateContainerDto.builder()
                .name(CONTAINER_1_NAME)
                .imageId(CONTAINER_1_ID)
                .build();

        /* test */
        create_generic(request);
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void create_noRole_fails() {
        final CreateContainerDto request = CreateContainerDto.builder()
                .name(CONTAINER_1_NAME)
                .imageId(CONTAINER_1_ID)
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

    public ResponseEntity<ContainerDto> findById_generic(UUID containerId, Container container, Principal principal)
            throws ContainerNotFoundException {

        /* mock */
        when(containerService.find(containerId))
                .thenReturn(container);

        /* test */
        final ResponseEntity<ContainerDto> response = containerEndpoint.findById(containerId, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        return response;
    }

    public void delete_generic(UUID containerId, Container container) throws ContainerNotFoundException {

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
        assertEquals(CONTAINER_1_INTERNAL_NAME, container1.getInternalName());
        final ContainerBriefDto container2 = body.get(1);
        assertEquals(CONTAINER_2_ID, container2.getId());
        assertEquals(CONTAINER_2_NAME, container2.getName());
        assertEquals(CONTAINER_2_INTERNAL_NAME, container2.getInternalName());
    }

    public void create_generic(CreateContainerDto data) throws ContainerAlreadyExistsException, ImageNotFoundException {

        /* mock */
        when(containerService.create(data))
                .thenReturn(CONTAINER_1);

        /* test */
        final ResponseEntity<ContainerDto> response = containerEndpoint.create(data);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

}
