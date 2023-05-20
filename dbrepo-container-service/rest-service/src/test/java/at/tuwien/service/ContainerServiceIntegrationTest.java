package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.container.ContainerCreateRequestDto;
import at.tuwien.api.container.ContainerDto;
import at.tuwien.config.DockerConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.container.Container;
import at.tuwien.exception.*;
import at.tuwien.repository.jpa.ContainerRepository;
import at.tuwien.repository.jpa.ImageRepository;
import at.tuwien.repository.jpa.RealmRepository;
import at.tuwien.repository.jpa.UserRepository;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

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

    @Autowired
    private RealmRepository realmRepository;

    @BeforeEach
    public void beforeEach() {
        afterEach();
        /* create networks */
        DockerConfig.createAllNetworks();
        /* mock data */
        realmRepository.save(REALM_DBREPO);
        userRepository.save(USER_1_SIMPLE);
        userRepository.save(USER_2_SIMPLE);
        imageRepository.save(IMAGE_1_SIMPLE);
    }

    @AfterEach
    public void afterEach() {
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

        /* test */
        final Container container = containerService.create(request, USER_1_PRINCIPAL);
        assertEquals(CONTAINER_1_NAME, container.getName());
        assertEquals(USER_1_USERNAME, container.getCreator().getUsername());
        assertEquals(USER_1_USERNAME, container.getOwner().getUsername());
    }

    @Test
    public void create_conflictingNames_fails() {
        final ContainerCreateRequestDto request = ContainerCreateRequestDto.builder()
                .repository(IMAGE_1_REPOSITORY)
                .tag(IMAGE_1_TAG)
                .name(CONTAINER_1_NAME)
                .build();

        /* mock */
        containerRepository.save(CONTAINER_1_SIMPLE);

        /* test */
        assertThrows(ContainerAlreadyExistsException.class, () -> {
            containerService.create(request, USER_1_PRINCIPAL);
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

        /* test */
        assertThrows(ImageNotFoundException.class, () -> {
            containerService.create(request, USER_1_PRINCIPAL);
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
        DockerConfig.createContainer(null, CONTAINER_1_SIMPLE, CONTAINER_1_ENV);
        containerRepository.save(CONTAINER_1_SIMPLE);

        /* test */
        containerService.start(CONTAINER_1_ID);
    }

    @Test
    public void change_stop_succeeds() throws DockerClientException, ContainerNotFoundException,
            ContainerAlreadyStoppedException, InterruptedException {

        /* mock */
        DockerConfig.createContainer(null, CONTAINER_1_SIMPLE, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1_SIMPLE);
        containerRepository.save(CONTAINER_1_SIMPLE);

        /* test */
        containerService.stop(CONTAINER_1_ID);
    }

    @Test
    public void change_startSavedButNotFound_fails() {

        /* mock */
        containerRepository.save(CONTAINER_1_SIMPLE);

        /* test */
        assertThrows(ContainerNotFoundException.class, () -> {
            containerService.start(CONTAINER_1_ID);
        });
    }

    @Test
    public void change_removeSavedButNotFound_fails() {

        /* mock */
        containerRepository.save(CONTAINER_1_SIMPLE);

        /* test */
        assertThrows(ContainerNotFoundException.class, () -> {
            containerService.remove(CONTAINER_1_ID);
        });
    }

    @Test
    public void getAll_succeeds() {

        /* mock */
        containerRepository.save(CONTAINER_1_SIMPLE);
        containerRepository.save(CONTAINER_2_SIMPLE);

        /* test */
        final List<Container> response = containerService.getAll(null);
        assertEquals(2, response.size());
    }

    @Test
    public void getAll_limit_succeeds() {

        /* mock */
        containerRepository.save(CONTAINER_1_SIMPLE);
        containerRepository.save(CONTAINER_2_SIMPLE);

        /* test */
        final List<Container> response = containerService.getAll(1);
        assertEquals(1, response.size());
    }

    @Test
    public void remove_succeeds() throws DockerClientException, ContainerStillRunningException,
            ContainerNotFoundException, ContainerAlreadyRemovedException, InterruptedException {

        /* mock */
        DockerConfig.createContainer(null, CONTAINER_1_SIMPLE, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1_SIMPLE);
        DockerConfig.stopContainer(CONTAINER_1_SIMPLE);
        containerRepository.save(CONTAINER_1_SIMPLE);

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
        DockerConfig.createContainer(null, CONTAINER_1_SIMPLE, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1_SIMPLE);
        containerRepository.save(CONTAINER_1_SIMPLE);

        /* test */
        assertThrows(ContainerStillRunningException.class, () -> {
            containerService.remove(CONTAINER_1_ID);
        });
    }

    @Test
    public void change_alreadyRunning_fails() throws InterruptedException {

        /* mock */
        DockerConfig.createContainer(null, CONTAINER_1_SIMPLE, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1_SIMPLE);
        containerRepository.save(CONTAINER_1_SIMPLE);

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
        DockerConfig.createContainer(null, CONTAINER_1_SIMPLE, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1_SIMPLE);
        DockerConfig.stopContainer(CONTAINER_1_SIMPLE);
        containerRepository.save(CONTAINER_1_SIMPLE);

        /* test */
        assertThrows(ContainerAlreadyStoppedException.class, () -> {
            containerService.stop(CONTAINER_1_ID);
        });
    }

    @Test
    public void change_stopNeverStarted_fails() {

        /* mock */
        DockerConfig.createContainer(null, CONTAINER_1_SIMPLE, CONTAINER_1_ENV);
        containerRepository.save(CONTAINER_1_SIMPLE);

        /* test */
        assertThrows(ContainerAlreadyStoppedException.class, () -> {
            containerService.stop(CONTAINER_1_ID);
        });
    }

    @Test
    public void change_stopSavedButNotFound_fails() {

        /* mock */
        containerRepository.save(CONTAINER_1_SIMPLE);

        /* test */
        assertThrows(ContainerNotFoundException.class, () -> {
            containerService.stop(CONTAINER_1_ID);
        });
    }

    @Test
    public void inspect_succeeds() throws InterruptedException, DockerClientException, ContainerNotFoundException,
            ContainerNotRunningException {

        /* mock */
        DockerConfig.createContainer(null, CONTAINER_1_SIMPLE, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1_SIMPLE);
        containerRepository.save(CONTAINER_1_SIMPLE);

        /* test */
        final ContainerDto response = containerService.inspect(CONTAINER_1_ID);
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
        DockerConfig.createContainer(null, CONTAINER_1_SIMPLE, CONTAINER_1_ENV);
        containerRepository.save(CONTAINER_1_SIMPLE);

        /* test */
        assertThrows(ContainerNotRunningException.class, () -> {
            containerService.inspect(CONTAINER_1_ID);
        });
    }

    @Test
    public void list_notRunning_succeeds() throws InterruptedException {

        /* mock */
        DockerConfig.createContainer(null, CONTAINER_1_SIMPLE, CONTAINER_1_ENV);
        DockerConfig.createContainer(null, CONTAINER_2_SIMPLE, CONTAINER_2_ENV);
        DockerConfig.startContainer(CONTAINER_2_SIMPLE);
        containerRepository.save(CONTAINER_1_SIMPLE);
        containerRepository.save(CONTAINER_2_SIMPLE);

        /* test */
        final List<com.github.dockerjava.api.model.Container> response = containerService.list();
        assertEquals(2, response.size());
    }
}
