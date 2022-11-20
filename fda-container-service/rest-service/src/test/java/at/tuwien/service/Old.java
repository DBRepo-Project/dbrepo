package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.container.ContainerCreateRequestDto;
import at.tuwien.config.DockerUtil;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.container.Container;
import at.tuwien.exception.*;
import at.tuwien.repository.jpa.ContainerRepository;
import at.tuwien.repository.jpa.ImageRepository;
import at.tuwien.repository.jpa.UserRepository;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.Network;
import lombok.extern.log4j.Log4j2;
import org.apache.http.auth.BasicUserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class Old extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private ContainerRepository containerRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private DockerClient dockerClient;

    @Autowired
    private ContainerService containerService;

    @Autowired
    private DockerUtil dockerUtil;

    @Autowired
    private UserRepository userRepository;

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

        /* mock data */
        userRepository.save(USER_1);
        imageRepository.save(IMAGE_2);
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

    
//    public void create_succeeds()
//            throws DockerClientException, ImageNotFoundException, ContainerAlreadyExistsException,
//            UserNotFoundException {
//        final ContainerCreateRequestDto request = ContainerCreateRequestDto.builder()
//                .repository(IMAGE_1_REPOSITORY)
//                .tag(IMAGE_1_TAG)
//                .name(CONTAINER_1_NAME)
//                .build();
//        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);
//
//        /* mock */
//        when(containerRepository.findByInternalName(CONTAINER_1_INTERNALNAME))
//                .thenReturn(Optional.empty());
//        when(containerRepository.save(any(Container.class)))
//                .thenReturn(CONTAINER_1);
//
//        /* test */
//        final Container container = containerService.create(request, principal);
//        assertEquals(CONTAINER_1_NAME, container.getName());
//        assertEquals(1, userRepository.findAll().size());
//    }
//
//
//    public void create_conflictingNames_fails() {
//        final ContainerCreateRequestDto request = ContainerCreateRequestDto.builder()
//                .repository(IMAGE_1_REPOSITORY)
//                .tag(IMAGE_1_TAG)
//                .name(CONTAINER_1_NAME)
//                .build();
//        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);
//
//        /* mock */
//        when(containerRepository.findByInternalName(CONTAINER_1_INTERNALNAME))
//                .thenReturn(Optional.of(CONTAINER_1));
//
//        /* test */
//        assertThrows(ContainerAlreadyExistsException.class, () -> {
//            containerService.create(request, principal);
//        });
//    }
//
//
//    public void remove_alreadyRemoved_fails() {
//
//        /* mock */
//        when(containerRepository.findById(CONTAINER_1_ID))
//                .thenReturn(Optional.empty());
//
//        /* test */
//        assertThrows(ContainerNotFoundException.class, () -> {
//            containerService.remove(CONTAINER_1_ID);
//        });
//    }
//
//
//    public void create_notFound_fails() {
//        final ContainerCreateRequestDto request = ContainerCreateRequestDto.builder()
//                .repository(IMAGE_2_REPOSITORY)
//                .tag(IMAGE_2_TAG)
//                .name(CONTAINER_3_NAME)
//                .build();
//        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);
//
//        /* mock */
//
//        /* test */
//        assertThrows(ImageNotFoundException.class, () -> {
//            containerService.create(request, principal);
//        });
//    }
//
//
//
//    public void findById_notFound_fails() {
//
//        /* mock */
//
//        /* test */
//        assertThrows(ContainerNotFoundException.class, () -> {
//            containerService.find(CONTAINER_1_ID);
//        });
//    }
//
//
//    public void change_start_succeeds() throws DockerClientException, ContainerNotFoundException {
//
//        /* mock */
//        when(containerRepository.findById(CONTAINER_1_ID))
//                .thenReturn(Optional.of(CONTAINER_1));
//        dockerUtil.createContainer(CONTAINER_1);
//
//        /* test */
//        containerService.start(CONTAINER_1_ID);
//    }
//
//
//    public void change_stop_succeeds() throws DockerClientException, InterruptedException, ContainerNotFoundException {
//
//        /* mock */
//        when(containerRepository.findById(CONTAINER_1_ID))
//                .thenReturn(Optional.of(CONTAINER_1));
//        dockerUtil.createContainer(CONTAINER_1);
//        dockerUtil.startContainer(CONTAINER_1);
//
//        /* test */
//        containerService.stop(CONTAINER_1_ID);
//    }
//
//
//    public void change_startSavedButNotFound_fails() {
//
//        /* mock */
//        when(containerRepository.findById(CONTAINER_1_ID))
//                .thenReturn(Optional.of(CONTAINER_1));
//
//        /* test */
//        assertThrows(DockerClientException.class, () -> {
//            containerService.start(CONTAINER_1_ID);
//        });
//    }
//
//
//    public void change_removeSavedButNotFound_fails() {
//
//        /* mock */
//        when(containerRepository.findById(CONTAINER_1_ID))
//                .thenReturn(Optional.of(CONTAINER_1));
//
//        /* test */
//        assertThrows(DockerClientException.class, () -> {
//            containerService.remove(CONTAINER_1_ID);
//        });
//    }
//
//
//    public void getAll_succeeds() {
//
//        /* mock */
//        when(containerRepository.findAll())
//                .thenReturn(List.of(CONTAINER_1, CONTAINER_2));
//
//        /* test */
//        final List<Container> response = containerService.getAll();
//        assertEquals(2, response.size());
//    }
//
//
//    public void remove_succeeds() throws DockerClientException, ContainerStillRunningException, ContainerNotFoundException {
//
//        /* mock */
//        when(containerRepository.findById(CONTAINER_1_ID))
//                .thenReturn(Optional.of(CONTAINER_1));
//        dockerUtil.createContainer(CONTAINER_1);
//        dockerUtil.stopContainer(CONTAINER_1);
//
//        /* test */
//        containerService.remove(CONTAINER_1_ID);
//    }
//
//
//    public void remove_notFound_fails() {
//
//        /* test */
//        assertThrows(ContainerNotFoundException.class, () -> {
//            containerService.remove(CONTAINER_1_ID);
//        });
//    }
//
//
//    public void remove_stillRunning_fails() throws InterruptedException {
//
//        /* mock */
//        when(containerRepository.findById(CONTAINER_1_ID))
//                .thenReturn(Optional.of(CONTAINER_1));
//        dockerUtil.createContainer(CONTAINER_1);
//        dockerUtil.startContainer(CONTAINER_1);
//
//        /* test */
//        assertThrows(ContainerStillRunningException.class, () -> {
//            containerService.remove(CONTAINER_1_ID);
//        });
//    }
//
//
//    public void change_alreadyRunning_fails() throws InterruptedException {
//
//        /* mock */
//        when(containerRepository.findById(CONTAINER_1_ID))
//                .thenReturn(Optional.of(CONTAINER_1));
//        dockerUtil.createContainer(CONTAINER_1);
//        dockerUtil.startContainer(CONTAINER_1);
//
//        /* test */
//        assertThrows(DockerClientException.class, () -> {
//            containerService.start(CONTAINER_1_ID);
//        });
//    }
//
//
//    public void change_startNotFound_fails() {
//
//        /* mock */
//        when(containerRepository.findById(CONTAINER_1_ID))
//                .thenReturn(Optional.empty());
//        dockerUtil.createContainer(CONTAINER_1);
//
//        /* test */
//        assertThrows(ContainerNotFoundException.class, () -> {
//            containerService.start(CONTAINER_1_ID);
//        });
//    }
//
//
//    public void change_alreadyStopped_fails() throws InterruptedException {
//
//        /* mock */
//        when(containerRepository.findById(CONTAINER_1_ID))
//                .thenReturn(Optional.of(CONTAINER_1));
//        dockerUtil.createContainer(CONTAINER_1);
//        dockerUtil.startContainer(CONTAINER_1);
//        dockerUtil.stopContainer(CONTAINER_1);
//
//        /* test */
//        assertThrows(DockerClientException.class, () -> {
//            containerService.stop(CONTAINER_1_ID);
//        });
//    }
//
//
//    public void change_stopNeverStarted_fails() {
//
//        /* mock */
//        when(containerRepository.findById(CONTAINER_1_ID))
//                .thenReturn(Optional.of(CONTAINER_1));
//        dockerUtil.createContainer(CONTAINER_1);
//
//        /* test */
//        assertThrows(DockerClientException.class, () -> {
//            containerService.stop(CONTAINER_1_ID);
//        });
//    }
//
//
//    public void change_stopSavedButNotFound_fails() {
//
//        /* mock */
//        when(containerRepository.findById(CONTAINER_1_ID))
//                .thenReturn(Optional.of(CONTAINER_1));
//
//        /* test */
//        assertThrows(DockerClientException.class, () -> {
//            containerService.stop(CONTAINER_1_ID);
//        });
//    }
//
//
//    public void inspect_succeeds() throws InterruptedException, DockerClientException, ContainerNotFoundException,
//            ContainerNotRunningException {
//
//        /* mock */
//        when(containerRepository.findById(CONTAINER_1_ID))
//                .thenReturn(Optional.of(CONTAINER_1));
//        dockerUtil.createContainer(CONTAINER_1);
//        dockerUtil.startContainer(CONTAINER_1);
//
//        /* test */
//        final Container response = containerService.inspect(CONTAINER_1_ID);
//        assertEquals(CONTAINER_1_ID, response.getId());
//        assertEquals(CONTAINER_1_NAME, response.getName());
//        assertEquals(CONTAINER_1_INTERNALNAME, response.getInternalName());
//        assertEquals(CONTAINER_1_IP, response.getIpAddress());
//    }
//
//
//    public void inspect_notFound_fails() {
//
//        /* mock */
//
//        /* test */
//        assertThrows(ContainerNotFoundException.class, () -> {
//            containerService.inspect(CONTAINER_2_ID);
//        });
//    }
//
//
//    public void inspect_notRunning_fails() {
//
//        /* mock */
//        when(containerRepository.findById(CONTAINER_1_ID))
//                .thenReturn(Optional.of(CONTAINER_1));
//        dockerUtil.createContainer(CONTAINER_1);
//
//        /* test */
//        assertThrows(ContainerNotRunningException.class, () -> {
//            containerService.inspect(CONTAINER_1_ID);
//        });
//    }

}
