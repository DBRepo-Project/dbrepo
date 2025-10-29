package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.container.image.ImageBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.container.image.ImageChangeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.container.image.ImageCreateDto;
import at.ac.tuwien.ifs.dbrepo.core.api.container.image.ImageDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.container.image.ContainerImage;
import at.ac.tuwien.ifs.dbrepo.core.exception.ImageAlreadyExistsException;
import at.ac.tuwien.ifs.dbrepo.core.exception.ImageInvalidException;
import at.ac.tuwien.ifs.dbrepo.core.exception.ImageNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.service.impl.ImageServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class ImageEndpointUnitTest extends BaseTest {

    @MockitoBean
    private ImageServiceImpl imageService;

    @Autowired
    private ImageEndpoint imageEndpoint;

    @Autowired
    private MetadataMapper metadataMapper;

    @Test
    @WithAnonymousUser
    public void findAll_anonymous_succeeds() {

        /* test */
        findAll_generic();
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"find-image"})
    public void findAll_hasRole_succeeds() {

        /* test */
        findAll_generic();
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void findAll_noRole_succeeds() {

        /* test */
        findAll_generic();
    }

    @Test
    @WithAnonymousUser
    public void create_anonymous_fails() {
        final ImageCreateDto request = ImageCreateDto.builder()
                .name(IMAGE_1_NAME)
                .version(IMAGE_1_VERSION)
                .defaultPort(IMAGE_1_DEFAULT_PORT)
                .dialect(IMAGE_1_DIALECT)
                .jdbcMethod(IMAGE_1_JDBC_METHOD)
                .build();

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            create_generic(request, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"create-image"})
    public void create_hasRole_fails() {
        final ImageCreateDto request = ImageCreateDto.builder()
                .name(IMAGE_1_NAME)
                .version(IMAGE_1_VERSION)
                .defaultPort(IMAGE_1_DEFAULT_PORT)
                .dialect(IMAGE_1_DIALECT)
                .jdbcMethod(IMAGE_1_JDBC_METHOD)
                .build();

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            create_generic(request, USER_1_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void create_noRole_fails() {
        final ImageCreateDto request = ImageCreateDto.builder()
                .name(IMAGE_1_NAME)
                .version(IMAGE_1_VERSION)
                .defaultPort(IMAGE_1_DEFAULT_PORT)
                .dialect(IMAGE_1_DIALECT)
                .jdbcMethod(IMAGE_1_JDBC_METHOD)
                .build();

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            create_generic(request, USER_4_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-image"})
    public void create_succeeds() throws ImageAlreadyExistsException, ImageInvalidException {

        /* test */
        create_generic(metadataMapper.containerImageToImageCreateDto(IMAGE_1), USER_1_PRINCIPAL);
    }

    @Test
    public void findById_anonymous_succeeds() throws ImageNotFoundException {

        /* test */
        findById_generic(CONTAINER_1_ID, IMAGE_1);
    }

    @Test
    public void findById_anonymousNotFound_succeeds() throws ImageNotFoundException {

        /* mock */
        doThrow(ImageNotFoundException.class)
                .when(imageService)
                .find(CONTAINER_1_ID);

        /* test */
        assertThrows(ImageNotFoundException.class, () -> {
            imageEndpoint.findById(CONTAINER_1_ID);
        });
    }

    @Test
    @WithAnonymousUser
    public void delete_anonymous_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            delete_generic(CONTAINER_1_ID, IMAGE_1);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void delete_noRole_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            delete_generic(CONTAINER_1_ID, IMAGE_1);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"delete-image"})
    public void delete_hasRole_succeeds() throws ImageNotFoundException {

        /* mock */
        doNothing()
                .when(imageService)
                .delete(IMAGE_1);

        /* test */
        delete_generic(CONTAINER_1_ID, IMAGE_1);
    }

    @Test
    @WithAnonymousUser
    public void modify_anonymous_fails() {
        final ImageChangeDto request = ImageChangeDto.builder()
                .defaultPort(IMAGE_1_DEFAULT_PORT)
                .dialect(IMAGE_1_DIALECT)
                .jdbcMethod(IMAGE_1_JDBC_METHOD)
                .driverClass(IMAGE_1.getDriverClass())
                .build();

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            modify_generic(CONTAINER_1_ID, IMAGE_1, request);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void modify_noRole_fails() {
        final ImageChangeDto request = ImageChangeDto.builder()
                .defaultPort(IMAGE_1_DEFAULT_PORT)
                .dialect(IMAGE_1_DIALECT)
                .jdbcMethod(IMAGE_1_JDBC_METHOD)
                .driverClass(IMAGE_1.getDriverClass())
                .build();

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            modify_generic(CONTAINER_1_ID, IMAGE_1, request);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"modify-image"})
    public void modify_hasRole_succeeds() throws ImageNotFoundException {
        final ImageChangeDto request = ImageChangeDto.builder()
                .registry(IMAGE_1.getRegistry())
                .defaultPort(IMAGE_1_DEFAULT_PORT)
                .dialect(IMAGE_1_DIALECT)
                .jdbcMethod(IMAGE_1_JDBC_METHOD)
                .driverClass(IMAGE_1.getDriverClass())
                .build();

        /* test */
        modify_generic(CONTAINER_1_ID, IMAGE_1, request);
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    public void findAll_generic() {

        /* mock */
        when(imageService.getAll())
                .thenReturn(List.of(IMAGE_1));

        /* test */
        final ResponseEntity<List<ImageBriefDto>> response = imageEndpoint.findAll();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        final List<ImageBriefDto> body = response.getBody();
        assertEquals(1, body.size());
    }

    public void create_generic(ImageCreateDto data, Principal principal) throws ImageAlreadyExistsException,
            ImageInvalidException {

        /* mock */
        when(imageService.create(data, principal))
                .thenReturn(IMAGE_1);

        /* test */
        final ResponseEntity<ImageDto> response = imageEndpoint.create(data, principal);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    public void findById_generic(UUID imageId, ContainerImage image) throws ImageNotFoundException {

        /* mock */
        when(imageService.find(imageId))
                .thenReturn(image);

        /* test */
        final ResponseEntity<ImageDto> response = imageEndpoint.findById(imageId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    public void delete_generic(UUID imageId, ContainerImage image) throws ImageNotFoundException {

        /* mock */
        when(imageService.find(imageId))
                .thenReturn(image);

        /* test */
        final ResponseEntity<?> response = imageEndpoint.delete(imageId);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNull(response.getBody());
    }

    public void modify_generic(UUID imageId, ContainerImage image, ImageChangeDto data) throws ImageNotFoundException {

        /* mock */
        when(imageService.find(imageId))
                .thenReturn(image);
        when(imageService.update(image, data))
                .thenReturn(image);

        /* test */
        final ResponseEntity<?> response = imageEndpoint.update(imageId, data);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

}
