package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.api.user.RoleTypeDto;
import at.tuwien.api.user.UserPasswordDto;
import at.tuwien.api.user.UserRolesDto;
import at.tuwien.config.AuthenticationConfig;
import at.tuwien.config.DockerConfig;
import at.tuwien.config.RabbitMqConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.user.RoleType;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.repositories.ImageRepository;
import at.tuwien.repositories.UserRepository;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.PortBinding;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static at.tuwien.config.DockerConfig.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class UserServiceIntegrationTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private AuthenticationConfig authenticationConfig;

    @Autowired
    private UserService userService;

    /* keep */
    @Autowired
    @Qualifier("junitRestTemplate")
    private RestTemplate restTemplate;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RabbitMqConfig amqpConfig;

    @BeforeAll
    public static void beforeAll() {
        afterAll();
        DockerConfig.createAllNetworks();
    }

    @AfterAll
    public static void afterAll() {
        DockerConfig.removeAllContainers();
        DockerConfig.removeAllNetworks();
    }

    @BeforeEach
    public void beforeEach() {
        afterAll();
        DockerConfig.createAllNetworks();
        /* metadata database */
        imageRepository.save(IMAGE_1);
        userRepository.save(USER_1);
    }

    @AfterEach
    public void afterEach() {
        DockerConfig.removeAllContainers();
        DockerConfig.removeAllNetworks();
    }

    @Test
    public void create_succeeds() throws UserNameExistsException, RoleNotFoundException, UserEmailExistsException,
            InterruptedException {
        final SignupRequestDto request = SignupRequestDto.builder()
                .username(USER_2_USERNAME)
                .password(USER_2_PASSWORD)
                .email(USER_2_EMAIL)
                .build();

        /* mock */
        final CreateContainerResponse response1 = dockerClient.createContainerCmd(IMAGE_BROKER_IMAGE + ":" + IMAGE_BROKER_TAG)
                .withHostConfig(hostConfig.withNetworkMode("fda-public").withPortBindings(PortBinding.parse("15672:15672")))
                .withName(CONTAINER_BROKER_NAME)
                .withIpv4Address(CONTAINER_BROKER_IP)
                .withHostName(CONTAINER_BROKER_INTERNAL_NAME)
                .withVolumes()
                .exec();
        CONTAINER_BROKER.setHash(response1.getId());
        startContainer(CONTAINER_BROKER);
        when(authenticationConfig.getDefaultRoles())
                .thenReturn(new RoleType[]{RoleType.ROLE_RESEARCHER});
        when(authenticationConfig.getSuperUsers())
                .thenReturn(new String[]{});

        /* test */
        final User response = userService.create(request);
        assertEquals(USER_2_USERNAME, response.getUsername());
        assertEquals(USER_2_EMAIL, response.getEmail());
    }

    @Test
    public void updatePassword_succeeds() throws UserNotFoundException, BrokerUserCreationException,
            InterruptedException {
        final String USER_1_OLD_PASSWORD = "other";
        final UserPasswordDto request = UserPasswordDto.builder()
                .password(USER_1_PASSWORD)
                .build();

        /* mock */
        final CreateContainerResponse response1 = dockerClient.createContainerCmd(IMAGE_BROKER_IMAGE + ":" + IMAGE_BROKER_TAG)
                .withHostConfig(hostConfig.withNetworkMode("fda-public").withPortBindings(PortBinding.parse("15672:15672")))
                .withName(CONTAINER_BROKER_NAME)
                .withIpv4Address(CONTAINER_BROKER_IP)
                .withHostName(CONTAINER_BROKER_INTERNAL_NAME)
                .withVolumes()
                .exec();
        CONTAINER_BROKER.setHash(response1.getId());
        startContainer(CONTAINER_BROKER);
        amqpConfig.addUser(USER_1_USERNAME, USER_1_OLD_PASSWORD, "administrator");
        amqpConfig.grantAccess(USER_1_USERNAME);
        when(authenticationConfig.getDefaultRoles())
                .thenReturn(new RoleType[]{RoleType.ROLE_RESEARCHER});
        when(authenticationConfig.getSuperUsers())
                .thenReturn(new String[]{});

        /* test */
        final User response = userService.updatePassword(USER_1_ID, request);
        assertEquals(USER_1_USERNAME, response.getUsername());
        assertEquals(USER_1_EMAIL, response.getEmail());
    }

    @Test
    public void create_isSuperUser_succeeds() throws UserNameExistsException, RoleNotFoundException,
            UserEmailExistsException {
        final SignupRequestDto request = SignupRequestDto.builder()
                .username(USER_2_USERNAME)
                .password(USER_2_PASSWORD)
                .email(USER_2_EMAIL)
                .build();

        /* mock */
        when(authenticationConfig.getDefaultRoles())
                .thenReturn(new RoleType[]{RoleType.ROLE_RESEARCHER});
        when(authenticationConfig.getSuperUsers())
                .thenReturn(new String[]{USER_2_USERNAME});

        /* test */
        final User response = userService.create(request);
        assertEquals(USER_2_USERNAME, response.getUsername());
        assertEquals(USER_2_EMAIL, response.getEmail());
        assertEquals(List.of(RoleType.ROLE_RESEARCHER, RoleType.ROLE_DEVELOPER, RoleType.ROLE_DATA_STEWARD), response.getRoles());
    }

    @Test
    public void create_noRole_succeeds() throws UserNameExistsException, RoleNotFoundException,
            UserEmailExistsException {
        final SignupRequestDto request = SignupRequestDto.builder()
                .username(USER_2_USERNAME)
                .password(USER_2_PASSWORD)
                .email(USER_2_EMAIL)
                .build();

        /* mock */
        when(authenticationConfig.getDefaultRoles())
                .thenReturn(new RoleType[]{});
        when(authenticationConfig.getSuperUsers())
                .thenReturn(new String[]{});

        /* test */
        final User response = userService.create(request);
        assertEquals(USER_2_USERNAME, response.getUsername());
        assertEquals(USER_2_EMAIL, response.getEmail());
        assertEquals(List.of(), response.getRoles());
    }

    @Test
    public void findByUsernameOrEmail_username_succeeds() throws UserNotFoundException {

        /* test */
        final User response = userService.findByUsernameOrEmail(USER_1_USERNAME, null);
        assertEquals(USER_1_ID, response.getId());
        assertEquals(USER_1_USERNAME, response.getUsername());
        assertEquals(USER_1_EMAIL, response.getEmail());
    }

    @Test
    public void findByUsernameOrEmail_email_succeeds() throws UserNotFoundException {

        /* test */
        final User response = userService.findByUsernameOrEmail(null, USER_1_EMAIL);
        assertEquals(USER_1_ID, response.getId());
        assertEquals(USER_1_USERNAME, response.getUsername());
        assertEquals(USER_1_EMAIL, response.getEmail());
    }

    @Test
    public void findByUsernameOrEmail_fails() {

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            userService.findByUsernameOrEmail(USER_2_USERNAME, USER_2_EMAIL);
        });
    }

    @Test
    public void updateRoles_notUnique_fails() {
        final UserRolesDto request = UserRolesDto.builder()
                .roles(List.of(RoleTypeDto.ROLE_RESEARCHER, RoleTypeDto.ROLE_RESEARCHER))
                .build();

        /* test */
        assertThrows(DataIntegrityViolationException.class, () -> {
            userService.updateRoles(USER_1_ID, request);
        });
    }

}
