package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.api.user.UserPasswordDto;
import at.tuwien.config.RabbitMqConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.dto.AmqpUserBriefDto;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.repositories.ImageRepository;
import at.tuwien.repositories.UserRepository;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.Network;
import com.github.dockerjava.api.model.PortBinding;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class UserServiceIntegrationTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @Autowired
    private UserService userService;

    @Autowired
    @Qualifier("junitRestTemplate")
    private RestTemplate restTemplate;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RabbitMqConfig amqpConfig;

    @BeforeEach
    public void beforeEach() throws InterruptedException {
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
    public void create_succeeds() throws UserNameExistsException, RoleNotFoundException, UserEmailExistsException {
        final SignupRequestDto request = SignupRequestDto.builder()
                .username(USER_2_USERNAME)
                .password(USER_2_PASSWORD)
                .email(USER_2_EMAIL)
                .build();

        /* mock */

        /* test */
        final User response = userService.create(request);
        assertEquals(USER_2_USERNAME, response.getUsername());
        assertEquals(USER_2_EMAIL, response.getEmail());
    }

    @Test
    public void updatePassword_succeeds() throws UserNotFoundException, BrokerUserCreationException {
        final String USER_1_OLD_PASSWORD = "other";
        final UserPasswordDto request = UserPasswordDto.builder()
                .password(USER_1_PASSWORD)
                .build();

        /* mock */
        amqpConfig.addUser(USER_1_USERNAME, USER_1_OLD_PASSWORD, "administrator");
        amqpConfig.grantAccess(USER_1_USERNAME);

        /* test */
        final User response = userService.updatePassword(USER_1_ID, request);
        assertEquals(USER_1_USERNAME, response.getUsername());
        assertEquals(USER_1_EMAIL, response.getEmail());
        final AmqpUserBriefDto response2 = amqpConfig.whoami(USER_1_USERNAME, USER_1_PASSWORD);
        assertEquals(USER_1_USERNAME, response2.getName());
    }

}
