package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.amqp.CreateUserDto;
import at.tuwien.config.DockerConfig;
import at.tuwien.config.RabbitMqConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.dto.AmqpUserBriefDto;
import at.tuwien.exception.*;
import at.tuwien.repositories.ImageRepository;
import at.tuwien.repositories.UserRepository;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.Network;
import com.github.dockerjava.api.model.PortBinding;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

import static at.tuwien.config.DockerConfig.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class QueueServiceIntegrationTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    /* keep */
    @Autowired
    @Qualifier("junitRestTemplate")
    private RestTemplate restTemplate;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private QueueService queueService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RabbitMqConfig amqpConfig;

    @BeforeAll
    public static void beforeAll() {
        afterAll();
        DockerConfig.createAllNetworks();
    }

    @BeforeEach
    public void beforeEach() throws InterruptedException {
        afterEach();
        DockerConfig.createAllNetworks();
        /* create container */
        final PortBinding binding = PortBinding.parse("15672:15672");
        final CreateContainerResponse response1 = dockerClient.createContainerCmd(IMAGE_BROKER_IMAGE + ":" + IMAGE_BROKER_TAG)
                .withHostConfig(hostConfig.withNetworkMode("fda-public").withPortBindings(binding))
                .withName(CONTAINER_BROKER_NAME)
                .withIpv4Address(CONTAINER_BROKER_IP)
                .withHostName(CONTAINER_BROKER_INTERNAL_NAME)
                .withVolumes()
                .exec();
        CONTAINER_BROKER.setHash(response1.getId());
        startContainer(CONTAINER_BROKER);
        /* metadata database */
        imageRepository.save(IMAGE_1);
        userRepository.save(USER_1);
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
    public void updatePassword_succeeds() throws BrokerUserCreationException {
        final String USER_1_OLD_PASSWORD = "other";
        final CreateUserDto request = CreateUserDto.builder()
                .password(USER_1_PASSWORD)
                .tags("administrator")
                .build();

        /* mock */
        amqpConfig.addUser(USER_1_USERNAME, USER_1_OLD_PASSWORD, "administrator");
        amqpConfig.grantAccess(USER_1_USERNAME);
        queueService.modifyUserPassword(USER_1, request);

        /* test */
        final AmqpUserBriefDto response2 = amqpConfig.whoami(USER_1_USERNAME, USER_1_PASSWORD);
        assertEquals(USER_1_USERNAME, response2.getName());
    }

}
