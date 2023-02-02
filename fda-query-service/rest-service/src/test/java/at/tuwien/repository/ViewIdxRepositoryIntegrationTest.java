package at.tuwien.repository;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.ViewCreateDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.database.View;
import at.tuwien.exception.*;
import at.tuwien.listener.impl.RabbitMqListenerImpl;
import at.tuwien.repository.elastic.ViewIdxRepository;
import at.tuwien.repository.jpa.*;
import at.tuwien.service.ViewService;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Network;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.junit.Rule;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.rules.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.util.*;

import static at.tuwien.config.DockerConfig.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class ViewIdxRepositoryIntegrationTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private Channel channel;

    @MockBean
    private RabbitMqListenerImpl rabbitMqListener;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private DatabaseRepository databaseRepository;

    @MockBean
    private ViewRepository viewRepository;

    @Autowired
    private ViewIdxRepository viewIdxRepository;

    @Autowired
    private ViewService viewService;

    @Rule
    public Timeout globalTimeout = Timeout.seconds(60);

    @BeforeAll
    public static void beforeAll() throws InterruptedException {
        afterAll();
        /* create network */
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
        /* create elastic search */
        final CreateContainerResponse response1 = dockerClient.createContainerCmd(IMAGE_ELASTIC_REPOSITORY + ":" + IMAGE_ELASTIC_TAG)
                .withHostConfig(hostConfig.withNetworkMode("fda-public"))
                .withName(CONTAINER_ELASTIC_NAME)
                .withIpv4Address(CONTAINER_ELASTIC_IP)
                .withHostName(CONTAINER_ELASTIC_INTERNAL_NAME)
                .withEnv(IMAGE_ELASTIC_ENV)
                .withCmd(IMAGE_ELASTIC_CMD)
                .exec();
        CONTAINER_ELASTIC.setHash(response1.getId());
        dockerClient.startContainerCmd(response1.getId()).exec();
        /* create container */
        final String bind = new File(
                "./src/test/resources/weather").toPath().toAbsolutePath() + ":/docker-entrypoint-initdb.d";
        log.trace("container bind {}", bind);
        final CreateContainerResponse response = dockerClient.createContainerCmd(IMAGE_1_REPOSITORY + ":" + IMAGE_1_TAG)
                .withHostConfig(hostConfig.withNetworkMode("fda-userdb").withBinds(Bind.parse(bind), Bind.parse("/tmp:/tmp")))
                .withName(CONTAINER_1_INTERNALNAME)
                .withHealthcheck(CONTAINER_1_HEALTHCHECK)
                .withIpv4Address(CONTAINER_1_IP)
                .withHostName(CONTAINER_1_INTERNALNAME)
                .withEnv(CONTAINER_1_ENV)
                .exec();
        CONTAINER_1.setHash(response.getId());
        startContainer(CONTAINER_1);
    }

    @AfterAll
    public static void afterAll() {
        /* stop containers and remove them */
        dockerClient.listContainersCmd()
                .withShowAll(true)
                .exec()
                .forEach(container -> {
                    log.info("Delete container {}", Arrays.asList(container.getNames()));
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
    public void save_succeeds() throws UserNotFoundException, DatabaseConnectionException, ViewMalformedException,
            QueryMalformedException, DatabaseNotFoundException {
        final ViewCreateDto request = ViewCreateDto.builder()
                .name(VIEW_1_NAME)
                .query(VIEW_1_QUERY)
                .isPublic(VIEW_1_PUBLIC)
                .build();

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));
        when(viewRepository.save(any(View.class)))
                .thenReturn(VIEW_1);

        /* test */
        viewService.create(CONTAINER_1_ID, DATABASE_1_ID, request, USER_1_PRINCIPAL);
        final Optional<ViewDto> response = viewIdxRepository.findById(VIEW_1_ID);
        assertTrue(response.isPresent());
        final ViewDto view = response.get();
        assertEquals(VIEW_1_ID, view.getId());
        assertEquals(VIEW_1_NAME, view.getName());
        assertEquals(VIEW_1_INTERNAL_NAME, view.getInternalName());
        assertEquals(VIEW_1_QUERY, view.getQuery());
        assertEquals(VIEW_1_CONTAINER_ID, view.getVdbid());
        assertEquals(VIEW_1_DATABASE_ID, view.getVdbid());
        assertEquals(VIEW_1_PUBLIC, view.getIsPublic());
    }

}
