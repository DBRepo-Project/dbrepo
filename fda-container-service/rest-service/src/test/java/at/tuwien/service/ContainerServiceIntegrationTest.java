package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.container.ContainerCreateRequestDto;
import at.tuwien.config.DockerUtil;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.container.Container;
import at.tuwien.entities.container.image.ContainerImage;
import at.tuwien.exception.*;
import at.tuwien.repository.jpa.ContainerRepository;
import at.tuwien.repository.jpa.ImageRepository;
import at.tuwien.repository.jpa.UserRepository;
import at.tuwien.service.impl.ContainerServiceImpl;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.*;
import lombok.extern.log4j.Log4j2;
import org.apache.http.auth.BasicUserPrincipal;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class ContainerServiceIntegrationTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContainerServiceImpl containerService;

    @Autowired
    private HostConfig hostConfig;

    @Autowired
    private DockerClient dockerClient;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private DockerUtil dockerUtil;

    @BeforeEach
    public void beforeEach() {
        afterEach();
        /* create networks */
        dockerClient.createNetworkCmd()
                .withName("fda-userdb")
                .withIpam(new Network.Ipam()
                        .withConfig(new Network.Ipam.Config()
                                .withSubnet("172.28.0.0/16")))
                .withEnableIpv6(false)
                .exec();
        dockerClient.createNetworkCmd()
                .withName("fda-public")
                .withIpam(new Network.Ipam()
                        .withConfig(new Network.Ipam.Config()
                                .withSubnet("172.29.0.0/16")))
                .withEnableIpv6(false)
                .exec();

        /* create weather container */
        final CreateContainerResponse request = dockerClient.createContainerCmd(IMAGE_1_REPOSITORY + ":" + IMAGE_1_TAG)
                .withHostConfig(hostConfig.withNetworkMode("fda-userdb"))
                .withName(CONTAINER_1_INTERNALNAME)
                .withIpv4Address(CONTAINER_1_IP)
                .withHostName(CONTAINER_1_INTERNALNAME)
                .withEnv("MARIADB_USER=mariadb", "MARIADB_PASSWORD=mariadb", "MARIADB_ROOT_PASSWORD=mariadb", "MARIADB_DATABASE=weather")
                .exec();

        /* set hash */
        CONTAINER_1.setHash(request.getId());

        /* mock data */
        log.debug("save image {}", IMAGE_1);
        imageRepository.save(IMAGE_1);
        log.debug("save container {}", CONTAINER_1);
        containerRepository.save(CONTAINER_1);
        log.debug("save container {}", CONTAINER_2);
        containerRepository.save(CONTAINER_2);
    }

    @AfterEach
    public void afterEach() {
        /* stop containers and remove them */
        dockerClient.listContainersCmd()
                .withShowAll(true)
                .exec()
                .forEach(container -> {
                    log.info("Delete container {}", container.getNames()[0]);
                    try {
                        dockerClient.stopContainerCmd(container.getId()).exec();
                    } catch (NotModifiedException e) {
                        // ignore
                    }
                    dockerClient.removeContainerCmd(container.getId()).exec();
                });

        /* remove networks */
        dockerClient.listNetworksCmd()
                .exec()
                .stream()
                .filter(n -> n.getName().startsWith("fda"))
                .forEach(network -> {
                    log.info("Delete network {}", network.getName());
                    dockerClient.removeNetworkCmd(network.getId()).exec();
                });
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

        /* mock */
        userRepository.save(USER_1);

        /* test */
        final Container container = containerService.create(request, principal);
        assertEquals(CONTAINER_1_NAME, container.getName());
        assertEquals(1, userRepository.findAll().size());
    }

    @Test
    public void create_multiple_succeeds()
            throws DockerClientException, ImageNotFoundException, ContainerAlreadyExistsException,
            UserNotFoundException {
        final ContainerCreateRequestDto request1 = ContainerCreateRequestDto.builder()
                .repository(IMAGE_1_REPOSITORY)
                .tag(IMAGE_1_TAG)
                .name(CONTAINER_1_NAME)
                .build();
        final ContainerCreateRequestDto request2 = ContainerCreateRequestDto.builder()
                .repository(IMAGE_1_REPOSITORY)
                .tag(IMAGE_1_TAG)
                .name(CONTAINER_2_NAME)
                .build();
        final ContainerCreateRequestDto request3 = ContainerCreateRequestDto.builder()
                .repository(IMAGE_1_REPOSITORY)
                .tag(IMAGE_1_TAG)
                .name(CONTAINER_3_NAME)
                .build();
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);

        /* mock */
        userRepository.save(USER_1);

        /* test */
        final Container container1 = containerService.create(request1, principal);
        assertEquals(CONTAINER_1_NAME, container1.getName());
        assertEquals(1, userRepository.findAll().size());
        final Container container2 = containerService.create(request2, principal);
        assertEquals(CONTAINER_2_NAME, container2.getName());
        assertEquals(1, userRepository.findAll().size());
        final Container container3 = containerService.create(request3, principal);
        assertEquals(CONTAINER_3_NAME, container3.getName());
        assertEquals(1, userRepository.findAll().size());
    }

    @Test
    public void create_conflictingNames_fails()
            throws DockerClientException, ImageNotFoundException, ContainerAlreadyExistsException,
            UserNotFoundException {
        final ContainerCreateRequestDto request = ContainerCreateRequestDto.builder()
                .repository(IMAGE_1_REPOSITORY)
                .tag(IMAGE_1_TAG)
                .name(CONTAINER_1_NAME)
                .build();
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);

        /* mock */
        userRepository.save(USER_1);
        containerService.create(request, principal);

        /* test */
        assertThrows(ContainerAlreadyExistsException.class, () -> {
            containerService.create(request, principal);
        });
    }

    @Test
    public void remove_hashNotFound_fails() {
        final Container CONTAINER = Container.builder()
                .id(CONTAINER_3_ID)
                .name(CONTAINER_3_NAME)
                .internalName(CONTAINER_3_INTERNALNAME)
                .image(IMAGE_1)
                .hash("deadbeef")
                .created(CONTAINER_3_CREATED)
                .build();

        /* mock */
        final Container container = containerRepository.save(CONTAINER);
        log.debug("inserted container with id {}", container.getId());

        /* test */
        assertThrows(DockerClientException.class, () -> {
            containerService.remove(CONTAINER_3_ID);
        });
    }

    @Test
    public void remove_alreadyRemoved_fails() throws DockerClientException, ContainerStillRunningException,
            ContainerNotFoundException {

        /* mock */
        containerService.remove(CONTAINER_1_ID);
        final Container container = containerRepository.save(CONTAINER_1);
        log.debug("re-inserting container with id {}", container.getId());

        /* test */
        assertThrows(DockerClientException.class, () -> {
            containerService.remove(container.getId());
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
            containerService.find(CONTAINER_3_ID);
        });
    }

    @Test
    public void change_start_succeeds() throws DockerClientException, ContainerNotFoundException {

        /* mock */
        dockerUtil.stopContainer(CONTAINER_1);

        /* test */
        containerService.start(CONTAINER_1_ID);
    }

    @Test
    public void change_stop_succeeds() throws DockerClientException, InterruptedException, ContainerNotFoundException {

        /* mock */
        dockerUtil.startContainer(CONTAINER_1);

        /* test */
        containerService.stop(CONTAINER_1_ID);
    }

    @Test
    public void change_stop_notFoundDocker_fails() {

        /* test */
        assertThrows(DockerClientException.class, () -> {
            containerService.stop(CONTAINER_2_ID);
        });
    }

    @Test
    public void getAll_succeeds() {

        /* test */
        final List<Container> response = containerService.getAll();
        assertEquals(2, response.size());
    }

    @Test
    public void remove_succeeds() throws DockerClientException, ContainerStillRunningException, ContainerNotFoundException {

        /* mock */
        dockerUtil.stopContainer(CONTAINER_1);

        /* test */
        containerService.remove(CONTAINER_1_ID);
    }

    @Test
    public void remove_notFound_fails() {

        /* test */
        assertThrows(ContainerNotFoundException.class, () -> {
            containerService.remove(9999999L);
        });
    }

    @Test
    public void remove_stillRunning_fails() throws InterruptedException {

        /* mock */
        dockerUtil.startContainer(CONTAINER_1);

        /* test */
        assertThrows(ContainerStillRunningException.class, () -> {
            containerService.remove(CONTAINER_1_ID);
        });
    }

    @Test
    public void change_alreadyRunning_fails() throws InterruptedException {

        /* mock */
        dockerUtil.startContainer(CONTAINER_1);

        /* test */
        assertThrows(DockerClientException.class, () -> {
            containerService.start(CONTAINER_1_ID);
        });
    }

    @Test
    public void change_startNotFound_fails() {

        /* mock */
        containerRepository.save(CONTAINER_3);

        /* test */
        assertThrows(DockerClientException.class, () -> {
            containerService.start(CONTAINER_3_ID);
        });
    }

    @Test
    public void change_alreadyStopped_fails() {

        /* mock */
        dockerUtil.stopContainer(CONTAINER_1);

        /* test */
        assertThrows(DockerClientException.class, () -> {
            containerService.stop(CONTAINER_1_ID);
        });
    }

    @Test
    public void change_stoppedNotFound_fails() {

        /* mock */
        dockerUtil.stopContainer(CONTAINER_1);

        /* test */
        assertThrows(DockerClientException.class, () -> {
            containerService.stop(CONTAINER_1_ID);
        });
    }

    @Test
    public void inspect_succeeds() throws InterruptedException, DockerClientException, ContainerNotFoundException,
            ContainerNotRunningException {

        /* mock */
        dockerUtil.startContainer(CONTAINER_1);

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
        assertThrows(DockerClientException.class, () -> {
            containerService.inspect(CONTAINER_2_ID);
        });
    }

    @Test
    public void inspect_notRunning_fails() {

        /* mock */
        dockerUtil.stopContainer(CONTAINER_1);

        /* test */
        assertThrows(ContainerNotRunningException.class, () -> {
            containerService.inspect(CONTAINER_1_ID);
        });
    }

}
