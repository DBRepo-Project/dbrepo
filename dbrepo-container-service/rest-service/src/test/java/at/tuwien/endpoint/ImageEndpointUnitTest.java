package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.container.*;
import at.tuwien.api.container.image.ImageBriefDto;
import at.tuwien.api.container.image.ImageChangeDto;
import at.tuwien.api.container.image.ImageCreateDto;
import at.tuwien.api.container.image.ImageDto;
import at.tuwien.config.DockerUtil;
import at.tuwien.config.ReadyConfig;
import at.tuwien.endpoints.ContainerEndpoint;
import at.tuwien.endpoints.ImageEndpoint;
import at.tuwien.entities.container.Container;
import at.tuwien.entities.container.image.ContainerImage;
import at.tuwien.exception.*;
import at.tuwien.repository.jpa.ImageRepository;
import at.tuwien.repository.jpa.UserRepository;
import at.tuwien.service.impl.ContainerServiceImpl;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.ws.rs.NotAllowedException;
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
    private ImageRepository imageRepository;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private ImageEndpoint imageEndpoint;

    @Autowired
    private DockerUtil dockerUtil;

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
        final ImageCreateDto request = ImageCreateDto.builder()
                .repository(IMAGE_1_REPOSITORY)
                .tag(IMAGE_1_TAG)
                .defaultPort(IMAGE_1_PORT)
                .dialect(IMAGE_1_DIALECT)
                .jdbcMethod(IMAGE_1_JDBC)
                .environment(IMAGE_1_ENV_DTO)
                .build();

        /* test */
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            create_generic(request, null);
        });
    }

    @Test
    @WithAnonymousUser
    public void create_anonymous2_fails() {
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
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void create_researcher_fails() {
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
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void create_developer_succeeds() throws UserNotFoundException, ImageAlreadyExistsException,
            DockerClientException, ImageNotFoundException, ImageInvalidException {
        final ImageCreateDto request = ImageCreateDto.builder()
                .repository(IMAGE_1_REPOSITORY)
                .tag(IMAGE_1_TAG)
                .defaultPort(IMAGE_1_PORT)
                .dialect(IMAGE_1_DIALECT)
                .jdbcMethod(IMAGE_1_JDBC)
                .environment(IMAGE_1_ENV_DTO)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_2_USERNAME))
                .thenReturn(Optional.of(USER_2));

        /* test */
        create_generic(request, USER_2_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void create_developerMissingEssentialInfo_fails() {
        final ImageCreateDto request = ImageCreateDto.builder()
                .repository(IMAGE_1_REPOSITORY)
                .tag(IMAGE_1_TAG)
                .defaultPort(null)
                .dialect(IMAGE_1_DIALECT)
                .jdbcMethod(IMAGE_1_JDBC)
                .environment(IMAGE_1_ENV_DTO)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_2_USERNAME))
                .thenReturn(Optional.of(USER_2));

        /* test */
        assertThrows(ImageInvalidException.class, () -> {
            create_generic(request, USER_2_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void create_dataSteward_fails() {
        final ImageCreateDto request = ImageCreateDto.builder()
                .repository(IMAGE_1_REPOSITORY)
                .tag(IMAGE_1_TAG)
                .defaultPort(IMAGE_1_PORT)
                .dialect(IMAGE_1_DIALECT)
                .jdbcMethod(IMAGE_1_JDBC)
                .environment(IMAGE_1_ENV_DTO)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_3_USERNAME))
                .thenReturn(Optional.of(USER_3));

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            create_generic(request, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void findById_anonymous_succeeds() throws ImageNotFoundException {

        /* test */
        findById_generic(IMAGE_1_ID, IMAGE_1, null);
    }

    @Test
    public void findById_anonymousNotFound_succeeds() {

        /* mock */
        when(imageRepository.findById(CONTAINER_1_ID))
                .thenReturn(Optional.empty());

        /* test */
        assertThrows(ImageNotFoundException.class, () -> {
            imageEndpoint.findById(CONTAINER_1_ID, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void findById_researcher_succeeds() throws ImageNotFoundException {

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        findById_generic(IMAGE_1_ID, IMAGE_1, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void findById_developer_succeeds() throws ImageNotFoundException {

        /* mock */
        when(userRepository.findByUsername(USER_2_USERNAME))
                .thenReturn(Optional.of(USER_2));

        /* test */
        findById_generic(IMAGE_1_ID, IMAGE_1, USER_2_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void findById_dataSteward_succeeds() throws ImageNotFoundException {

        /* mock */
        when(userRepository.findByUsername(USER_3_USERNAME))
                .thenReturn(Optional.of(USER_3));

        /* test */
        findById_generic(IMAGE_1_ID, IMAGE_1, USER_3_PRINCIPAL);
    }

    @Test
    public void delete_anonymous_fails() {

        /* test */
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            delete_generic(IMAGE_1_ID, IMAGE_1, null);
        });
    }

    @Test
    @WithAnonymousUser
    public void delete_anonymous2_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            delete_generic(IMAGE_1_ID, IMAGE_1, null);
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
            delete_generic(IMAGE_1_ID, IMAGE_1, USER_1_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void delete_developer_succeeds() throws ImageNotFoundException {

        /* mock */
        when(userRepository.findByUsername(USER_2_USERNAME))
                .thenReturn(Optional.of(USER_2));

        /* test */
        delete_generic(IMAGE_1_ID, IMAGE_1, USER_2_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void delete_developer_fails() {

        /* mock */
        doThrow(DataIntegrityViolationException.class)
                .when(imageRepository)
                .deleteById(IMAGE_1_ID);
        when(userRepository.findByUsername(USER_2_USERNAME))
                .thenReturn(Optional.of(USER_2));

        /* test */
        assertThrows(ImageNotFoundException.class, () -> {
            delete_generic(IMAGE_1_ID, IMAGE_1, USER_2_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void delete_dataSteward_fails() {

        /* mock */
        when(userRepository.findByUsername(USER_3_USERNAME))
                .thenReturn(Optional.of(USER_3));

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            delete_generic(IMAGE_1_ID, IMAGE_1, USER_3_PRINCIPAL);
        });
    }

    @Test
    public void modify_anonymous_fails() {
        final ImageChangeDto request = ImageChangeDto.builder()
                .defaultPort(IMAGE_1_PORT)
                .dialect(IMAGE_1_DIALECT)
                .jdbcMethod(IMAGE_1_JDBC)
                .driverClass(IMAGE_1_DRIVER)
                .environment(IMAGE_1_ENV_DTO)
                .build();

        /* test */
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            modify_generic(IMAGE_1_ID, IMAGE_1, request, null);
        });
    }

    @Test
    @WithAnonymousUser
    public void modify_anonymous2_fails() {
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
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void modify_researcher_fails() {
        final ImageChangeDto request = ImageChangeDto.builder()
                .defaultPort(IMAGE_1_PORT)
                .dialect(IMAGE_1_DIALECT)
                .jdbcMethod(IMAGE_1_JDBC)
                .driverClass(IMAGE_1_DRIVER)
                .environment(IMAGE_1_ENV_DTO)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            modify_generic(IMAGE_1_ID, IMAGE_1, request, USER_1_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void modify_developer_succeeds() throws ImageNotFoundException {
        final ImageChangeDto request = ImageChangeDto.builder()
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

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void modify_dataSteward_fails() {
        final ImageChangeDto request = ImageChangeDto.builder()
                .defaultPort(IMAGE_1_PORT)
                .dialect(IMAGE_1_DIALECT)
                .jdbcMethod(IMAGE_1_JDBC)
                .driverClass(IMAGE_1_DRIVER)
                .environment(IMAGE_1_ENV_DTO)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_3_USERNAME))
                .thenReturn(Optional.of(USER_3));

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            modify_generic(IMAGE_1_ID, IMAGE_1, request, USER_3_PRINCIPAL);
        });
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    public void findAll_generic(Principal principal) {

        /* mock */
        when(imageRepository.findAll())
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
        when(imageRepository.save(any(ContainerImage.class)))
                .thenReturn(IMAGE_1);

        /* test */
        final ResponseEntity<ImageDto> response = imageEndpoint.create(data, principal);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    public void findById_generic(Long imageId, ContainerImage image, Principal principal) throws ImageNotFoundException {

        /* mock */
        when(imageRepository.findById(imageId))
                .thenReturn(Optional.of(image));

        /* test */
        final ResponseEntity<ImageDto> response = imageEndpoint.findById(imageId, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    public void delete_generic(Long imageId, ContainerImage image, Principal principal) throws ImageNotFoundException {

        /* mock */
        when(imageRepository.findById(imageId))
                .thenReturn(Optional.of(image));

        /* test */
        final ResponseEntity<?> response = imageEndpoint.delete(imageId, principal);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNull(response.getBody());
    }

    public void modify_generic(Long imageId, ContainerImage image, ImageChangeDto data, Principal principal)
            throws ImageNotFoundException {

        /* mock */
        when(imageRepository.findById(imageId))
                .thenReturn(Optional.of(image));
        when(imageRepository.save(any(ContainerImage.class)))
                .thenReturn(image);

        /* test */
        final ResponseEntity<?> response = imageEndpoint.update(imageId, data, principal);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

}
