package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.amqp.CreateUserDto;
import at.tuwien.config.RabbitMqConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.dto.AmqpUserBriefDto;
import at.tuwien.exception.*;
import at.tuwien.repositories.ImageRepository;
import at.tuwien.repositories.UserRepository;
import config.DockerConfig;
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
    public static void beforeAll() throws InterruptedException {
        afterAll();
        /* create networks */
        DockerConfig.createAllNetworks();
        DockerConfig.createContainer(null, CONTAINER_BROKER, CONTAINER_BROKER_ENV);
        DockerConfig.startContainer(CONTAINER_BROKER);
    }

    @BeforeEach
    public void beforeEach() {
        /* metadata database */
        imageRepository.save(IMAGE_1);
        userRepository.save(USER_1);
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
