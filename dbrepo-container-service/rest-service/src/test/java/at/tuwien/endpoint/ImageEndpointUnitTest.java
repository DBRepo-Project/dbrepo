package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.container.image.ImageBriefDto;
import at.tuwien.api.container.image.ImageChangeDto;
import at.tuwien.api.container.image.ImageCreateDto;
import at.tuwien.api.container.image.ImageDto;
import at.tuwien.config.DockerDaemonConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.endpoints.ImageEndpoint;
import at.tuwien.entities.container.image.ContainerImage;
import at.tuwien.exception.*;
import at.tuwien.repository.mdb.UserRepository;
import at.tuwien.service.impl.ImageServiceImpl;
import lombok.extern.log4j.Log4j2;
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
import static org.mockito.Mockito.*;

@Log4j2
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class ImageEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private ImageServiceImpl imageService;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private ImageEndpoint imageEndpoint;

    @Autowired
    private DockerDaemonConfig dockerUtil;

    @Test
    @WithAnonymousUser
    public void findAll_anonymous_succeeds() {

        /* test */
        findAll_generic(null);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"find-image"})
    public void findAll_hasRole_succeeds() {

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        findAll_generic(USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void findAll_noRole_succeeds() {

        /* mock */
        when(userRepository.findByUsername(USER_4_USERNAME))
                .thenReturn(Optional.of(USER_4));

        /* test */
        findAll_generic(USER_4_PRINCIPAL);
    }

    @Test
    @WithAnonymousUser
    public void create_anonymous_fails() {
        final ImageCreateDto request = ImageCreateDto.builder()
                .repository(IMAGE_1_REPOSITORY)
                .tag(IMAGE_1_TAG)
                .defaultPort(IMAGE_1_PORT)
                .dialect(IMAGE_1_DIALECT)
                .jdbcMethod(IMAGE_1_JDBC)
                .environment(IMAGE_1_ENV_DTO)
                .build();

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            create_generic(request, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"create-image"})
    public void create_hasRole_fails() {
        final ImageCreateDto request = ImageCreateDto.builder()
                .repository(IMAGE_1_REPOSITORY)
                .tag(IMAGE_1_TAG)
                .defaultPort(IMAGE_1_PORT)
                .dialect(IMAGE_1_DIALECT)
                .jdbcMethod(IMAGE_1_JDBC)
                .environment(IMAGE_1_ENV_DTO)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            create_generic(request, USER_1_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void create_noRole_fails() {
        final ImageCreateDto request = ImageCreateDto.builder()
                .repository(IMAGE_1_REPOSITORY)
                .tag(IMAGE_1_TAG)
                .defaultPort(IMAGE_1_PORT)
                .dialect(IMAGE_1_DIALECT)
                .jdbcMethod(IMAGE_1_JDBC)
                .environment(IMAGE_1_ENV_DTO)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_4_USERNAME))
                .thenReturn(Optional.of(USER_4));

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            create_generic(request, USER_4_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-image"})
    public void create_missingEssentialInfo_fails() {
        final ImageCreateDto request = ImageCreateDto.builder()
                .repository(IMAGE_1_REPOSITORY)
                .tag(IMAGE_1_TAG)
                .defaultPort(null)
                .dialect(IMAGE_1_DIALECT)
                .jdbcMethod(IMAGE_1_JDBC)
                .environment(IMAGE_1_ENV_DTO)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        assertThrows(ImageInvalidException.class, () -> {
            create_generic(request, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void findById_anonymous_succeeds() throws ImageNotFoundException {

        /* test */
        findById_generic(IMAGE_1_ID, IMAGE_1);
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
        assertThrows(AccessDeniedException.class, () -> {
            delete_generic(IMAGE_1_ID, IMAGE_1, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void delete_noRole_fails() {

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            delete_generic(IMAGE_1_ID, IMAGE_1, USER_1_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"delete-image"})
    public void delete_hasRole_succeeds() throws ImageNotFoundException, PersistenceException {

        /* mock */
        doNothing()
                .when(imageService)
                .delete(IMAGE_1_ID);
        when(userRepository.findByUsername(USER_2_USERNAME))
                .thenReturn(Optional.of(USER_2));

        /* test */
        delete_generic(IMAGE_1_ID, IMAGE_1, USER_2_PRINCIPAL);
    }

    @Test
    @WithAnonymousUser
    public void modify_anonymous_fails() {
        final ImageChangeDto request = ImageChangeDto.builder()
                .defaultPort(IMAGE_1_PORT)
                .dialect(IMAGE_1_DIALECT)
                .jdbcMethod(IMAGE_1_JDBC)
                .driverClass(IMAGE_1_DRIVER)
                .environment(IMAGE_1_ENV_DTO)
                .build();

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            modify_generic(IMAGE_1_ID, IMAGE_1, request, null);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void modify_noRole_fails() {
        final ImageChangeDto request = ImageChangeDto.builder()
                .defaultPort(IMAGE_1_PORT)
                .dialect(IMAGE_1_DIALECT)
                .jdbcMethod(IMAGE_1_JDBC)
                .driverClass(IMAGE_1_DRIVER)
                .environment(IMAGE_1_ENV_DTO)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_4_USERNAME))
                .thenReturn(Optional.of(USER_4));

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            modify_generic(IMAGE_1_ID, IMAGE_1, request, USER_4_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"modify-image"})
    public void modify_hasRole_succeeds() throws ImageNotFoundException, DockerClientException {
        final ImageChangeDto request = ImageChangeDto.builder()
                .registry(IMAGE_1_REGISTRY)
                .defaultPort(IMAGE_1_PORT)
                .dialect(IMAGE_1_DIALECT)
                .jdbcMethod(IMAGE_1_JDBC)
                .driverClass(IMAGE_1_DRIVER)
                .environment(IMAGE_1_ENV_DTO)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_2_USERNAME))
                .thenReturn(Optional.of(USER_2));

        /* test */
        modify_generic(IMAGE_1_ID, IMAGE_1, request, USER_2_PRINCIPAL);
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    public void findAll_generic(Principal principal) {

        /* mock */
        when(imageService.getAll())
                .thenReturn(List.of(IMAGE_1));

        /* test */
        final ResponseEntity<List<ImageBriefDto>> response = imageEndpoint.findAll(principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        final List<ImageBriefDto> body = response.getBody();
        assertEquals(1, body.size());
    }

    public void create_generic(ImageCreateDto data, Principal principal) throws UserNotFoundException,
            ImageAlreadyExistsException, DockerClientException, ImageNotFoundException, ImageInvalidException {

        /* mock */
        when(imageService.create(data, principal))
                .thenReturn(IMAGE_1);

        /* test */
        final ResponseEntity<ImageDto> response = imageEndpoint.create(data, principal);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    public void findById_generic(Long imageId, ContainerImage image) throws ImageNotFoundException {

        /* mock */
        when(imageService.find(imageId))
                .thenReturn(image);

        /* test */
        final ResponseEntity<ImageDto> response = imageEndpoint.findById(imageId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    public void delete_generic(Long imageId, ContainerImage image, Principal principal) throws ImageNotFoundException {

        /* mock */
        when(imageService.find(imageId))
                .thenReturn(image);

        /* test */
        final ResponseEntity<?> response = imageEndpoint.delete(imageId, principal);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNull(response.getBody());
    }

    public void modify_generic(Long imageId, ContainerImage image, ImageChangeDto data, Principal principal)
            throws ImageNotFoundException, DockerClientException {

        /* mock */
        when(imageService.find(imageId))
                .thenReturn(image);
        when(imageService.update(imageId, data))
                .thenReturn(image);

        /* test */
        final ResponseEntity<?> response = imageEndpoint.update(imageId, data, principal);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

}
