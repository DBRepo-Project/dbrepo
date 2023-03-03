package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.container.ContainerCreateRequestDto;
import at.tuwien.config.DockerConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.container.Container;
import at.tuwien.entities.container.image.ContainerImage;
import at.tuwien.exception.*;
import at.tuwien.repository.jpa.ContainerRepository;
import at.tuwien.repository.jpa.ImageRepository;
import at.tuwien.repository.jpa.UserRepository;
import lombok.extern.log4j.Log4j2;
import org.apache.http.auth.BasicUserPrincipal;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.Principal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class ContainerServiceIntegrationTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContainerService containerService;

    @BeforeAll
    public static void beforeAll() {
        afterAll();
        DockerConfig.createAllNetworks();
    }

    @BeforeEach
    public void beforeEach() {
        afterEach();
        DockerConfig.createAllNetworks();
        /* mock data */
        userRepository.save(USER_1);
        imageRepository.save(ContainerImage.builder()
                .id(IMAGE_1_ID)
                .repository(IMAGE_1_REPOSITORY)
                .tag(IMAGE_1_TAG)
                .hash(IMAGE_1_HASH)
                .compiled(IMAGE_1_BUILT)
                .dialect(IMAGE_1_DIALECT)
                .jdbcMethod(IMAGE_1_JDBC)
                .driverClass(IMAGE_1_DRIVER)
                .size(IMAGE_1_SIZE)
                .environment(IMAGE_1_ENV)
                .defaultPort(IMAGE_1_PORT)
                .build()) /* keep */;
    }

    @AfterEach
    public void afterEach() {
        DockerConfig.removeAllContainers();
        DockerConfig.removeAllNetworks();
    }

    @AfterAll
    public static void afterAll() {
        DockerConfig.removeAllContainers();
        DockerConfig.removeAllNetworks();
    }

    @Test
    public void create_succeeds()
            throws DockerClientException, ImageNotFoundException, ContainerAlreadyExistsException,
            UserNotFoundException {
        final ContainerCreateRequestDto request = ContainerCreateRequestDto.builder()
                .repository(IMAGE_1_REPOSITORY)
                .tag(IMAGE_1_TAG)
                .name(CONTAINER_1_NAME)
                .build();
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);

        /* test */
        final Container container = containerService.create(request, principal);
        assertEquals(CONTAINER_1_NAME, container.getName());
        assertEquals(USER_1_USERNAME, container.getCreator().getUsername());
        assertEquals(USER_1_USERNAME, container.getOwner().getUsername());
        assertEquals(1, userRepository.findAll().size());
    }

    @Test
    public void create_conflictingNames_fails() {
        final ContainerCreateRequestDto request = ContainerCreateRequestDto.builder()
                .repository(IMAGE_1_REPOSITORY)
                .tag(IMAGE_1_TAG)
                .name(CONTAINER_1_NAME)
                .build();
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);

        /* mock */
        containerRepository.save(CONTAINER_1);

        /* test */
        assertThrows(ContainerAlreadyExistsException.class, () -> {
            containerService.create(request, principal);
        });
    }

    @Test
    public void remove_alreadyRemoved_fails() {

        /* test */
        assertThrows(ContainerNotFoundException.class, () -> {
            containerService.remove(CONTAINER_1_ID);
        });
    }

    @Test
    public void create_notFound_fails() {
        final ContainerCreateRequestDto request = ContainerCreateRequestDto.builder()
                .repository(IMAGE_2_REPOSITORY)
                .tag(IMAGE_2_TAG)
                .name(CONTAINER_3_NAME)
                .build();
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);

        /* test */
        assertThrows(ImageNotFoundException.class, () -> {
            containerService.create(request, principal);
        });
    }

    @Test
    public void findById_notFound_fails() {

        /* test */
        assertThrows(ContainerNotFoundException.class, () -> {
            containerService.find(CONTAINER_1_ID);
        });
    }

    @Test
    public void change_start_succeeds() throws DockerClientException, ContainerNotFoundException,
            ContainerAlreadyRunningException {

        /* mock */
        DockerConfig.createContainer(null, CONTAINER_1, CONTAINER_1_ENV);
        containerRepository.save(CONTAINER_1);

        /* test */
        containerService.start(CONTAINER_1_ID);
    }

    @Test
    public void change_stop_succeeds() throws DockerClientException, ContainerNotFoundException,
            ContainerAlreadyStoppedException, InterruptedException {

        /* mock */
        DockerConfig.createContainer(null, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
        containerRepository.save(CONTAINER_1);

        /* test */
        containerService.stop(CONTAINER_1_ID);
    }

    @Test
    public void change_startSavedButNotFound_fails() {

        /* mock */
        containerRepository.save(CONTAINER_1);

        /* test */
        assertThrows(ContainerNotFoundException.class, () -> {
            containerService.start(CONTAINER_1_ID);
        });
    }

    @Test
    public void change_removeSavedButNotFound_fails() {

        /* mock */
        containerRepository.save(CONTAINER_1);

        /* test */
        assertThrows(ContainerNotFoundException.class, () -> {
            containerService.remove(CONTAINER_1_ID);
        });
    }

    @Test
    public void getAll_succeeds() {

        /* mock */
        containerRepository.save(CONTAINER_1);
        containerRepository.save(CONTAINER_2);

        /* test */
        final List<Container> response = containerService.getAll();
        assertEquals(2, response.size());
    }

    @Test
    public void remove_succeeds() throws DockerClientException, ContainerStillRunningException,
            ContainerNotFoundException, ContainerAlreadyRemovedException, InterruptedException {

        /* mock */
        DockerConfig.createContainer(null, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
        DockerConfig.stopContainer(CONTAINER_1);
        containerRepository.save(CONTAINER_1);

        /* test */
        containerService.remove(CONTAINER_1_ID);
    }

    @Test
    public void remove_notFound_fails() {

        /* test */
        assertThrows(ContainerNotFoundException.class, () -> {
            containerService.remove(CONTAINER_1_ID);
        });
    }

    @Test
    public void remove_stillRunning_fails() throws InterruptedException {

        /* mock */
        DockerConfig.createContainer(null, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
        containerRepository.save(CONTAINER_1);

        /* test */
        assertThrows(ContainerStillRunningException.class, () -> {
            containerService.remove(CONTAINER_1_ID);
        });
    }

    @Test
    public void change_alreadyRunning_fails() throws InterruptedException {

        /* mock */
        DockerConfig.createContainer(null, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
        containerRepository.save(CONTAINER_1);

        /* test */
        assertThrows(ContainerAlreadyRunningException.class, () -> {
            containerService.start(CONTAINER_1_ID);
        });
    }

    @Test
    public void change_startNotFound_fails() {

        /* mock */
        DockerConfig.createContainer(null, CONTAINER_1, CONTAINER_1_ENV);

        /* test */
        assertThrows(ContainerNotFoundException.class, () -> {
            containerService.start(CONTAINER_1_ID);
        });
    }

    @Test
    public void change_alreadyStopped_fails() throws InterruptedException {

        /* mock */
        DockerConfig.createContainer(null, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
        DockerConfig.stopContainer(CONTAINER_1);
        containerRepository.save(CONTAINER_1);

        /* test */
        assertThrows(ContainerAlreadyStoppedException.class, () -> {
            containerService.stop(CONTAINER_1_ID);
        });
    }

    @Test
    public void change_stopNeverStarted_fails() {

        /* mock */
        DockerConfig.createContainer(null, CONTAINER_1, CONTAINER_1_ENV);
        containerRepository.save(CONTAINER_1);

        /* test */
        assertThrows(ContainerAlreadyStoppedException.class, () -> {
            containerService.stop(CONTAINER_1_ID);
        });
    }

    @Test
    public void change_stopSavedButNotFound_fails() {

        /* mock */
        containerRepository.save(CONTAINER_1);

        /* test */
        assertThrows(ContainerNotFoundException.class, () -> {
            containerService.stop(CONTAINER_1_ID);
        });
    }

    @Test
    public void inspect_succeeds() throws InterruptedException, DockerClientException, ContainerNotFoundException,
            ContainerNotRunningException {

        /* mock */
        DockerConfig.createContainer(null, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
        containerRepository.save(CONTAINER_1);

        /* test */
        final Container response = containerService.inspect(CONTAINER_1_ID);
        assertEquals(CONTAINER_1_ID, response.getId());
        assertEquals(CONTAINER_1_NAME, response.getName());
        assertEquals(CONTAINER_1_INTERNALNAME, response.getInternalName());
        assertEquals(CONTAINER_1_IP, response.getIpAddress());
    }

    @Test
    public void inspect_notFound_fails() {

        /* test */
        assertThrows(ContainerNotFoundException.class, () -> {
            containerService.inspect(CONTAINER_2_ID);
        });
    }

    @Test
    public void inspect_notRunning_fails() {

        /* mock */
        DockerConfig.createContainer(null, CONTAINER_1, CONTAINER_1_ENV);
        containerRepository.save(CONTAINER_1);

        /* test */
        assertThrows(ContainerNotRunningException.class, () -> {
            containerService.inspect(CONTAINER_1_ID);
        });
    }
}
