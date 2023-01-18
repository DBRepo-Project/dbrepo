package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.*;
import at.tuwien.config.IndexInitializer;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.endpoints.DatabaseEndpoint;
import at.tuwien.entities.container.Container;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.exception.*;
import at.tuwien.repository.elastic.DatabaseidxRepository;
import at.tuwien.repository.jpa.ContainerRepository;
import at.tuwien.repository.jpa.DatabaseAccessRepository;
import at.tuwien.repository.jpa.DatabaseRepository;
import at.tuwien.repository.jpa.UserRepository;
import at.tuwien.service.MessageQueueService;
import at.tuwien.service.QueryStoreService;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Network;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.security.Principal;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static at.tuwien.config.DockerConfig.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class DatabaseEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private IndexInitializer indexInitializer;

    @MockBean
    private Channel channel;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private MessageQueueService messageQueueService;

    @MockBean
    private DatabaseidxRepository databaseidxRepository;

    @MockBean
    private DatabaseAccessRepository databaseAccessRepository;

    @MockBean
    private QueryStoreService queryStoreService;

    @MockBean
    private ContainerRepository containerRepository;

    @MockBean
    private DatabaseRepository databaseRepository;

    @Autowired
    private DatabaseEndpoint databaseEndpoint;

    @Timeout(value = 60)

    @BeforeAll
    public static void beforeAll() {
        afterAll();
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
    }

    public static void afterAll() {
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
        dockerClient.listNetworksCmd()
                .exec()
                .stream()
                .filter(n -> n.getName().startsWith("fda"))
                .forEach(network -> {
                    log.info("Delete network {}", network.getName());
                    dockerClient.removeNetworkCmd(network.getId()).exec();
                });
    }

    @AfterEach
    public void afterEach() {
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
    }

    protected void startDatabase(Container container, String... env) throws InterruptedException {
        dockerClient.listContainersCmd()
                .withShowAll(true)
                .exec()
                .forEach(c -> {
                    log.info("Delete container {}", Arrays.asList(c.getNames()));
                    try {
                        dockerClient.stopContainerCmd(c.getId()).exec();
                    } catch (NotModifiedException e) {
                        // ignore
                    }
                    dockerClient.removeContainerCmd(c.getId()).exec();
                });
        final String bind = new File("./src/test/resources/weather").toPath()
                .toAbsolutePath() + ":/docker-entrypoint-initdb.d";
        log.trace("container bind {}", bind);
        final CreateContainerResponse request = dockerClient.createContainerCmd(IMAGE_1_REPOSITORY + ":" + IMAGE_1_TAG)
                .withHostConfig(hostConfig.withNetworkMode("fda-userdb")
                        .withBinds(Bind.parse(bind)))
                .withName(container.getName())
                .withIpv4Address(container.getIpAddress())
                .withHostName(container.getInternalName())
                .withEnv(env)
                .exec();
        log.trace("start container with hostname={} and ip={}", container.getInternalName(), container.getIpAddress());
        dockerClient.startContainerCmd(request.getId())
                .exec();
        Thread.sleep(12 * 1000);
    }

    @Test
    public void create_anonymous_fails() {
        final DatabaseCreateDto request = DatabaseCreateDto.builder()
                .name(DATABASE_1_NAME)
                .isPublic(DATABASE_1_PUBLIC)
                .build();

        /* test */
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            create_generic(CONTAINER_1_ID, CONTAINER_1, DATABASE_1_ID, null, request, null);
        });
    }

    @Test
    @WithAnonymousUser
    public void create_anonymous2_fails() {
        final DatabaseCreateDto request = DatabaseCreateDto.builder()
                .name(DATABASE_1_NAME)
                .isPublic(DATABASE_1_PUBLIC)
                .build();

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            create_generic(CONTAINER_1_ID, CONTAINER_1, DATABASE_1_ID, null, request, null);
        });
    }

    @Test
    @Disabled
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void create_researcher_succeeds() throws UserNotFoundException, DatabaseNameExistsException,
            NotAllowedException, ContainerConnectionException, DatabaseMalformedException, QueryStoreException,
            DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException, ImageNotSupportedException,
            AmqpException, BrokerVirtualHostCreationException, ContainerNotFoundException, InterruptedException {
        final DatabaseCreateDto request = DatabaseCreateDto.builder()
                .name(DATABASE_1_NAME)
                .isPublic(DATABASE_1_PUBLIC)
                .build();

        /* mock */
        startDatabase(CONTAINER_1, CONTAINER_1_ENV);
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        create_generic(CONTAINER_2_ID, CONTAINER_2, DATABASE_2_ID, null, request, USER_1_PRINCIPAL);
    }

    @Test
    @Disabled
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void create_researcherExists_fails() throws InterruptedException {
        final DatabaseCreateDto request = DatabaseCreateDto.builder()
                .name(DATABASE_1_NAME)
                .isPublic(DATABASE_1_PUBLIC)
                .build();

        /* mock */
        startDatabase(CONTAINER_1, CONTAINER_1_ENV);
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        assertThrows(DatabaseMalformedException.class, () -> {
            create_generic(CONTAINER_1_ID, CONTAINER_1, DATABASE_1_ID, DATABASE_1, request, USER_1_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void create_researcherForeignContainer_fails() throws InterruptedException {
        final DatabaseCreateDto request = DatabaseCreateDto.builder()
                .name(DATABASE_1_NAME)
                .isPublic(DATABASE_1_PUBLIC)
                .build();

        /* mock */
        startDatabase(CONTAINER_2, CONTAINER_2_ENV);
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            create_generic(CONTAINER_2_ID, CONTAINER_2, DATABASE_1_ID, null, request, USER_1_PRINCIPAL);
        });
    }

    @Test
    @Disabled
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void create_developer_succeeds() throws UserNotFoundException, DatabaseNameExistsException,
            NotAllowedException, ContainerConnectionException, DatabaseMalformedException, QueryStoreException,
            DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException, ImageNotSupportedException,
            AmqpException, BrokerVirtualHostCreationException, ContainerNotFoundException, InterruptedException {
        final DatabaseCreateDto request = DatabaseCreateDto.builder()
                .name(DATABASE_2_NAME)
                .isPublic(DATABASE_2_PUBLIC)
                .build();

        /* mock */
        startDatabase(CONTAINER_2, CONTAINER_2_ENV);
        when(userRepository.findByUsername(USER_2_USERNAME))
                .thenReturn(Optional.of(USER_2));

        /* test */
        create_generic(CONTAINER_1_ID, CONTAINER_1, DATABASE_1_ID, null, request, USER_2_PRINCIPAL);
    }

    @Test
    @Disabled
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void create_developerForeignContainer_succeeds() throws UserNotFoundException, DatabaseNameExistsException,
            NotAllowedException, ContainerConnectionException, DatabaseMalformedException, QueryStoreException,
            DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException, ImageNotSupportedException,
            AmqpException, BrokerVirtualHostCreationException, ContainerNotFoundException, InterruptedException {
        final DatabaseCreateDto request = DatabaseCreateDto.builder()
                .name(DATABASE_2_NAME)
                .isPublic(DATABASE_2_PUBLIC)
                .build();

        /* mock */
        startDatabase(CONTAINER_1, CONTAINER_1_ENV);
        when(userRepository.findByUsername(USER_2_USERNAME))
                .thenReturn(Optional.of(USER_2));

        /* test */
        create_generic(CONTAINER_1_ID, CONTAINER_1, DATABASE_2_ID, null, request, USER_2_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void create_dataSteward_fails() {
        final DatabaseCreateDto request = DatabaseCreateDto.builder()
                .name(DATABASE_3_NAME)
                .isPublic(DATABASE_3_PUBLIC)
                .build();

        /* mock */
        when(userRepository.findByUsername(USER_3_USERNAME))
                .thenReturn(Optional.of(USER_3));

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            create_generic(CONTAINER_3_ID, CONTAINER_3, DATABASE_3_ID, null, request, USER_3_PRINCIPAL);
        });
    }

    @Test
    public void list_anonymousPublic_succeeds() {

        /* test */
        list_generic(CONTAINER_1_ID, CONTAINER_1, List.of(DATABASE_1), null);
    }

    @Test
    @WithAnonymousUser
    public void list_anonymous2Public_succeeds() {

        /* test */
        list_generic(CONTAINER_1_ID, CONTAINER_1, List.of(DATABASE_1), null);
    }

    @Test
    public void list_anonymousPrivate_succeeds() {

        /* test */
        list_generic(CONTAINER_1_ID, CONTAINER_1, List.of(DATABASE_2), null);
    }

    @Test
    @WithAnonymousUser
    public void list_anonymous2Private_succeeds() {

        /* test */
        list_generic(CONTAINER_1_ID, CONTAINER_1, List.of(DATABASE_2), null);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void list_researcherPublic_succeeds() {

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        list_generic(CONTAINER_1_ID, CONTAINER_1, List.of(DATABASE_1), USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void list_researcherPrivate_succeeds() {

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        list_generic(CONTAINER_2_ID, CONTAINER_2, List.of(DATABASE_2), USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void list_researcherPublicForeignContainer_succeeds() {

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        list_generic(CONTAINER_2_ID, CONTAINER_2, List.of(DATABASE_1), USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void list_researcherPrivateForeignContainer_succeeds() {

        /* mock */
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        list_generic(CONTAINER_2_ID, CONTAINER_2, List.of(DATABASE_2), USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void list_developerPublic_succeeds() {

        /* mock */
        when(userRepository.findByUsername(USER_2_USERNAME))
                .thenReturn(Optional.of(USER_2));

        /* test */
        list_generic(CONTAINER_2_ID, CONTAINER_2, List.of(DATABASE_1), USER_2_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void list_developerPrivate_succeeds() {

        /* mock */
        when(userRepository.findByUsername(USER_2_USERNAME))
                .thenReturn(Optional.of(USER_2));

        /* test */
        list_generic(CONTAINER_2_ID, CONTAINER_2, List.of(DATABASE_2), USER_2_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void list_developerPublicForeignContainer_succeeds() {

        /* mock */
        when(userRepository.findByUsername(USER_2_USERNAME))
                .thenReturn(Optional.of(USER_2));

        /* test */
        list_generic(CONTAINER_1_ID, CONTAINER_1, List.of(DATABASE_1), USER_2_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void list_developerPrivateForeignContainer_succeeds() {

        /* mock */
        when(userRepository.findByUsername(USER_2_USERNAME))
                .thenReturn(Optional.of(USER_2));

        /* test */
        list_generic(CONTAINER_1_ID, CONTAINER_1, List.of(DATABASE_2), USER_2_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void list_dataStewardPublic_succeeds() {

        /* mock */
        when(userRepository.findByUsername(USER_3_USERNAME))
                .thenReturn(Optional.of(USER_3));

        /* test */
        list_generic(CONTAINER_3_ID, CONTAINER_3, List.of(DATABASE_1), USER_3_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void list_dataStewardPrivate_succeeds() {

        /* mock */
        when(userRepository.findByUsername(USER_3_USERNAME))
                .thenReturn(Optional.of(USER_3));

        /* test */
        list_generic(CONTAINER_3_ID, CONTAINER_3, List.of(DATABASE_2), USER_3_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void list_dataStewardPublicForeignContainer_succeeds() {

        /* mock */
        when(userRepository.findByUsername(USER_3_USERNAME))
                .thenReturn(Optional.of(USER_3));

        /* test */
        list_generic(CONTAINER_2_ID, CONTAINER_2, List.of(DATABASE_1), USER_3_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void list_dataStewardPrivateForeignContainer_succeeds() {

        /* mock */
        when(userRepository.findByUsername(USER_3_USERNAME))
                .thenReturn(Optional.of(USER_3));

        /* test */
        list_generic(CONTAINER_2_ID, CONTAINER_2, List.of(DATABASE_2), USER_3_PRINCIPAL);
    }

    @Test
    public void transfer_anonymous_fails() {
        final DatabaseTransferDto request = DatabaseTransferDto.builder()
                .isPublic(true)
                .build();

        /* test */
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            transfer_generic(CONTAINER_1_ID, CONTAINER_1, DATABASE_1_ID, DATABASE_1, request, null);
        });
    }

    @Test
    @WithAnonymousUser
    public void transfer_anonymous2_fails() {
        final DatabaseTransferDto request = DatabaseTransferDto.builder()
                .isPublic(true)
                .build();

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            transfer_generic(CONTAINER_1_ID, CONTAINER_1, DATABASE_1_ID, DATABASE_1, request, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void transfer_researcher_succeeds() throws NotAllowedException, DatabaseNotFoundException {
        final DatabaseTransferDto request = DatabaseTransferDto.builder()
                .isPublic(true)
                .build();

        /* test */
        transfer_generic(CONTAINER_1_ID, CONTAINER_1, DATABASE_1_ID, DATABASE_1, request, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void transfer_researcherForeignDatabase_fails() {
        final DatabaseTransferDto request = DatabaseTransferDto.builder()
                .isPublic(true)
                .build();

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            transfer_generic(CONTAINER_2_ID, CONTAINER_2, DATABASE_2_ID, DATABASE_2, request, USER_1_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void transfer_developer_succeeds() throws NotAllowedException, DatabaseNotFoundException {
        final DatabaseTransferDto request = DatabaseTransferDto.builder()
                .isPublic(true)
                .build();

        /* test */
        transfer_generic(CONTAINER_2_ID, CONTAINER_2, DATABASE_2_ID, DATABASE_2, request, USER_2_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void transfer_developerForeignDatabase_succeeds() throws NotAllowedException, DatabaseNotFoundException {
        final DatabaseTransferDto request = DatabaseTransferDto.builder()
                .isPublic(true)
                .build();

        /* test */
        transfer_generic(CONTAINER_1_ID, CONTAINER_1, DATABASE_1_ID, DATABASE_1, request, USER_2_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void transfer_dataSteward_fails() {
        final DatabaseTransferDto request = DatabaseTransferDto.builder()
                .isPublic(true)
                .build();

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            transfer_generic(CONTAINER_3_ID, CONTAINER_3, DATABASE_3_ID, DATABASE_3, request, USER_3_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void transfer_dataStewardForeignDatabase_fails() {
        final DatabaseTransferDto request = DatabaseTransferDto.builder()
                .isPublic(true)
                .build();

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            transfer_generic(CONTAINER_2_ID, CONTAINER_2, DATABASE_2_ID, DATABASE_2, request, USER_3_PRINCIPAL);
        });
    }

    @Test
    public void findById_anonymous_succeeds() throws AccessDeniedException, DatabaseNotFoundException {

        /* test */
        findById_generic(CONTAINER_1_ID, CONTAINER_1, DATABASE_1_ID, DATABASE_1, null);
    }

    @Test
    @WithAnonymousUser
    public void findById_anonymous2_succeeds() throws AccessDeniedException, DatabaseNotFoundException {

        /* test */
        findById_generic(CONTAINER_1_ID, CONTAINER_1, DATABASE_1_ID, DATABASE_1, null);
    }

    @Test
    public void findById_anonymousNotFound_fails() {

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            findById_generic(CONTAINER_1_ID, CONTAINER_1, DATABASE_1_ID, null, null);
        });
    }

    @Test
    @WithAnonymousUser
    public void findById_anonymous2NotFound_fails() {

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            findById_generic(CONTAINER_1_ID, CONTAINER_1, DATABASE_1_ID, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void findById_researcherPublic_succeeds() throws AccessDeniedException, DatabaseNotFoundException {

        /* test */
        findById_generic(CONTAINER_1_ID, CONTAINER_1, DATABASE_1_ID, DATABASE_1, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void findById_researcherPublicForeignDatabase_succeeds() throws AccessDeniedException,
            DatabaseNotFoundException {

        /* test */
        findById_generic(CONTAINER_3_ID, CONTAINER_3, DATABASE_3_ID, DATABASE_3, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void findById_researcherPrivate_succeeds() throws AccessDeniedException, DatabaseNotFoundException {

        /* test */
        findById_generic(CONTAINER_2_ID, CONTAINER_2, DATABASE_2_ID, DATABASE_2, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void findById_researcherPrivateForeignDatabase_succeeds() throws AccessDeniedException,
            DatabaseNotFoundException {

        /* test */
        findById_generic(CONTAINER_2_ID, CONTAINER_2, DATABASE_2_ID, DATABASE_2, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void findById_developerPublic_succeeds() throws AccessDeniedException, DatabaseNotFoundException,
            InterruptedException {

        /* test */
        startDatabase(CONTAINER_1, CONTAINER_1_ENV);
        findById_generic(CONTAINER_1_ID, CONTAINER_1, DATABASE_1_ID, DATABASE_1, USER_2_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void findById_developerPublicForeignDatabase_succeeds() throws AccessDeniedException, DatabaseNotFoundException {

        /* test */
        findById_generic(CONTAINER_3_ID, CONTAINER_3, DATABASE_3_ID, DATABASE_3, USER_2_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void findById_developerPrivate_succeeds() throws AccessDeniedException, DatabaseNotFoundException {

        /* test */
        findById_generic(CONTAINER_2_ID, CONTAINER_2, DATABASE_2_ID, DATABASE_2, USER_2_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void findById_developerPrivateForeignDatabase_succeeds() throws AccessDeniedException,
            DatabaseNotFoundException, InterruptedException {

        /* mock */
        startDatabase(CONTAINER_2, CONTAINER_2_ENV);

        /* test */
        findById_generic(CONTAINER_2_ID, CONTAINER_2, DATABASE_2_ID, DATABASE_2, USER_2_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void findById_dataStewardPublic_succeeds() throws AccessDeniedException, DatabaseNotFoundException {

        /* test */
        findById_generic(CONTAINER_3_ID, CONTAINER_3, DATABASE_3_ID, DATABASE_3, USER_3_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void findById_dataStewardPublicForeignDatabase_succeeds() throws AccessDeniedException,
            DatabaseNotFoundException {

        /* test */
        findById_generic(CONTAINER_1_ID, CONTAINER_1, DATABASE_1_ID, DATABASE_1, USER_3_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void findById_dataStewardPrivate_succeeds() throws AccessDeniedException, DatabaseNotFoundException,
            InterruptedException {

        /* mock */
        startDatabase(CONTAINER_2, CONTAINER_2_ENV);

        /* test */
        findById_generic(CONTAINER_2_ID, CONTAINER_2, DATABASE_2_ID, DATABASE_2, USER_3_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void findById_dataStewardPrivateForeignDatabase_succeeds() throws AccessDeniedException,
            DatabaseNotFoundException {

        /* test */
        findById_generic(CONTAINER_2_ID, CONTAINER_2, DATABASE_2_ID, DATABASE_2, USER_3_PRINCIPAL);
    }

    @Test
    public void delete_anonymous_fails() {

        /* test */
        assertThrows(AuthenticationCredentialsNotFoundException.class, () -> {
            delete_generic(CONTAINER_1_ID, CONTAINER_1, DATABASE_1_ID, DATABASE_1, null, null);
        });
    }

    @Test
    @WithAnonymousUser
    public void delete_anonymous2_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            delete_generic(CONTAINER_1_ID, CONTAINER_1, DATABASE_1_ID, DATABASE_1, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void delete_researcher_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            delete_generic(CONTAINER_1_ID, CONTAINER_1, DATABASE_1_ID, DATABASE_1, USER_1_USERNAME, USER_1_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void delete_researcherForeignDatabase_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            delete_generic(CONTAINER_2_ID, CONTAINER_2, DATABASE_2_ID, DATABASE_2, USER_1_USERNAME, USER_1_PRINCIPAL);
        });
    }

    @Test
    @Disabled
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void delete_developer_succeeds() throws UserNotFoundException, DatabaseConnectionException,
            QueryMalformedException, DatabaseNotFoundException, ImageNotSupportedException, AmqpException,
            BrokerVirtualHostCreationException, ContainerNotFoundException, DatabaseMalformedException,
            InterruptedException, SQLException {

        /* mock */
        startDatabase(CONTAINER_2, CONTAINER_2_ENV);
        MariaDbConfig.mockQuery(CONTAINER_1_INTERNALNAME, "CREATE DATABASE `" + DATABASE_2_INTERNALNAME + "`", "root", "mariadb");

        /* test */
        delete_generic(CONTAINER_2_ID, CONTAINER_2, DATABASE_2_ID, DATABASE_2, USER_2_USERNAME, USER_2_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void delete_developerForeignDatabase_succeeds() throws UserNotFoundException, DatabaseConnectionException,
            QueryMalformedException, DatabaseNotFoundException, ImageNotSupportedException, AmqpException,
            BrokerVirtualHostCreationException, ContainerNotFoundException, DatabaseMalformedException,
            InterruptedException, SQLException {

        /* mock */
        startDatabase(CONTAINER_1, CONTAINER_1_ENV);
        MariaDbConfig.mockQuery(CONTAINER_1_INTERNALNAME, "CREATE DATABASE `" + DATABASE_1_INTERNALNAME + "`", "root", "mariadb");

        /* test */
        delete_generic(CONTAINER_1_ID, CONTAINER_1, DATABASE_1_ID, DATABASE_1, USER_2_USERNAME, USER_2_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void delete_dataSteward_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            delete_generic(CONTAINER_3_ID, CONTAINER_3, DATABASE_3_ID, DATABASE_3, USER_3_USERNAME, USER_3_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void delete_dataStewardForeignDatabase_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            delete_generic(CONTAINER_1_ID, CONTAINER_1, DATABASE_1_ID, DATABASE_1, USER_3_USERNAME, USER_3_PRINCIPAL);
        });
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    public void list_generic(Long containerId, Container container, List<Database> databases, Principal principal) {

        /* mock */
        when(containerRepository.findById(containerId))
                .thenReturn(Optional.of(container));
        when(databaseRepository.findAll(containerId))
                .thenReturn(databases);

        /* test */
        final ResponseEntity<List<DatabaseBriefDto>> response = databaseEndpoint.list(containerId, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        final List<DatabaseBriefDto> body = response.getBody();
        assertEquals(databases.size(), body.size());
    }

    public void create_generic(Long containerId, Container container, Long databaseId, Database database,
                               DatabaseCreateDto data, Principal principal) throws UserNotFoundException,
            DatabaseNameExistsException, NotAllowedException, ContainerConnectionException, DatabaseMalformedException,
            QueryStoreException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, AmqpException, BrokerVirtualHostCreationException, ContainerNotFoundException {

        /* mock */
        when(containerRepository.findById(containerId))
                .thenReturn(Optional.of(container));
        if (database != null) {
            when(databaseRepository.findById(databaseId))
                    .thenReturn(Optional.of(database));
        } else {
            when(databaseRepository.findById(databaseId))
                    .thenReturn(Optional.empty());
        }
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(database);
        doNothing()
                .when(messageQueueService)
                .createExchange(database, principal);
        doNothing()
                .when(queryStoreService)
                .create(containerId, databaseId, principal);
        doNothing()
                .when(messageQueueService)
                .updatePermissions(principal);
        when(databaseAccessRepository.save(any(DatabaseAccess.class)))
                .thenReturn(DATABASE_1_WRITE_ALL_ACCESS);

        /* test */
        final ResponseEntity<DatabaseBriefDto> response = databaseEndpoint.create(containerId, data, principal);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    public void transfer_generic(Long containerId, Container container, Long databaseId, Database database,
                                 DatabaseTransferDto data, Principal principal) throws NotAllowedException,
            DatabaseNotFoundException {

        /* mock */
        when(containerRepository.findById(containerId))
                .thenReturn(Optional.of(container));
        when(databaseRepository.findById(databaseId))
                .thenReturn(Optional.of(database));
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(database);
        when(databaseidxRepository.save(any(Database.class)))
                .thenReturn(database);

        /* test */
        final ResponseEntity<DatabaseDto> response = databaseEndpoint.transfer(containerId, databaseId, data, principal);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    public void findById_generic(Long containerId, Container container, Long databaseId, Database database,
                                 Principal principal) throws DatabaseNotFoundException, AccessDeniedException {

        /* mock */
        if (container != null) {
            when(containerRepository.findById(containerId))
                    .thenReturn(Optional.of(container));
        } else {
            when(containerRepository.findById(containerId))
                    .thenReturn(Optional.empty());
        }
        if (database != null) {
            when(databaseRepository.findById(databaseId))
                    .thenReturn(Optional.of(database));
        } else {
            when(databaseRepository.findById(databaseId))
                    .thenReturn(Optional.empty());
        }

        /* test */
        final ResponseEntity<DatabaseDto> response = databaseEndpoint.findById(containerId, databaseId, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    public void delete_generic(Long containerId, Container container, Long databaseId, Database database,
                               String username, Principal principal) throws DatabaseNotFoundException,
            UserNotFoundException, DatabaseConnectionException, QueryMalformedException, ImageNotSupportedException,
            AmqpException, BrokerVirtualHostCreationException, ContainerNotFoundException, DatabaseMalformedException {

        /* mock */
        when(containerRepository.findById(containerId))
                .thenReturn(Optional.of(container));
        when(databaseRepository.findById(databaseId))
                .thenReturn(Optional.of(database));
        if (username != null) {
            when(databaseRepository.findPublicOrMine(containerId, databaseId, username))
                    .thenReturn(Optional.of(database));
        }

        /* test */
        final ResponseEntity<?> response = databaseEndpoint.delete(containerId, databaseId, principal);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNull(response.getBody());
    }
}
