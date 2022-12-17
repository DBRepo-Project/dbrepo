package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.query.ExecuteStatementDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.database.query.QueryTypeDto;
import at.tuwien.api.database.table.TableCsvDto;
import at.tuwien.config.DockerConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.database.table.Table;
import at.tuwien.exception.*;
import at.tuwien.listener.impl.RabbitMqListenerImpl;
import at.tuwien.repository.jpa.*;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Network;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.junit.Rule;
import org.junit.rules.Timeout;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.io.File;
import java.math.BigInteger;
import java.security.Principal;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static at.tuwien.config.DockerConfig.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;


@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class QueryServiceIntegrationTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private Channel channel;

    @MockBean
    private RabbitMqListenerImpl rabbitMqListener;

    @MockBean
    private ImageRepository imageRepository;

    @MockBean
    private DatabaseRepository databaseRepository;

    @MockBean
    private TableRepository tableRepository;

    @MockBean
    private ContainerRepository containerRepository;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private QueryService queryService;

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
                .withEnv("MARIADB_USER=mariadb", "MARIADB_PASSWORD=mariadb", "MARIADB_ROOT_PASSWORD=mariadb",
                        "MARIADB_DATABASE=weather")
                .exec();
        CONTAINER_1.setHash(response.getId());
        startContainer(CONTAINER_1);

        /* create container */
        final String bind2 = new File(
                "./src/test/resources/zoo").toPath().toAbsolutePath() + ":/docker-entrypoint-initdb.d";
        log.trace("container bind {}", bind);
        final CreateContainerResponse response2 =
                dockerClient.createContainerCmd(IMAGE_1_REPOSITORY + ":" + IMAGE_1_TAG)
                        .withHostConfig(hostConfig.withNetworkMode("fda-userdb").withBinds(Bind.parse(bind2), Bind.parse("/tmp:/tmp")))
                        .withName(CONTAINER_2_INTERNALNAME)
                        .withIpv4Address(CONTAINER_2_IP)
                        .withHealthcheck(CONTAINER_2_HEALTHCHECK)
                        .withHostName(CONTAINER_2_INTERNALNAME)
                        .withEnv("MARIADB_USER=mariadb", "MARIADB_PASSWORD=mariadb", "MARIADB_ROOT_PASSWORD=mariadb",
                                "MARIADB_DATABASE=zoo")
                        .exec();
        CONTAINER_1.setHash(response.getId());
        CONTAINER_2.setHash(response2.getId());
        startContainer(CONTAINER_1);
        startContainer(CONTAINER_2);
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

    @BeforeEach
    public void beforeEach() {
        TABLE_1.setDatabase(DATABASE_1);
        TABLE_2.setDatabase(DATABASE_1);
        TABLE_3.setDatabase(DATABASE_1);
    }

    @Test
    public void findAll_succeeds() throws DatabaseNotFoundException, ImageNotSupportedException,
            TableMalformedException, TableNotFoundException, DatabaseConnectionException, PaginationException,
            ContainerNotFoundException, QueryMalformedException, UserNotFoundException {

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(tableRepository.find(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(Optional.of(TABLE_1));

        /* test */
        final QueryResultDto result = queryService.findAll(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, Instant.now(),
                null, null, USER_1_PRINCIPAL);
        assertEquals(3, result.getResult().size());
        assertEquals(BigInteger.valueOf(1L), result.getResult().get(0).get(COLUMN_1_1_INTERNAL_NAME));
        assertEquals(toInstant("2008-12-01"), result.getResult().get(0).get(COLUMN_1_2_INTERNAL_NAME));
        assertEquals("Albury", result.getResult().get(0).get(COLUMN_1_3_INTERNAL_NAME));
        assertEquals(13.4, result.getResult().get(0).get(COLUMN_1_4_INTERNAL_NAME));
        assertEquals(0.6, result.getResult().get(0).get(COLUMN_1_5_INTERNAL_NAME));
        assertEquals(BigInteger.valueOf(2L), result.getResult().get(1).get(COLUMN_1_1_INTERNAL_NAME));
        assertEquals(toInstant("2008-12-02"), result.getResult().get(1).get(COLUMN_1_2_INTERNAL_NAME));
        assertEquals("Albury", result.getResult().get(1).get(COLUMN_1_3_INTERNAL_NAME));
        assertEquals(7.4, result.getResult().get(1).get(COLUMN_1_4_INTERNAL_NAME));
        assertEquals(0.0, result.getResult().get(1).get(COLUMN_1_5_INTERNAL_NAME));
        assertEquals(BigInteger.valueOf(3L), result.getResult().get(2).get(COLUMN_1_1_INTERNAL_NAME));
        assertEquals(toInstant("2008-12-03"), result.getResult().get(2).get(COLUMN_1_2_INTERNAL_NAME));
        assertEquals("Albury", result.getResult().get(2).get(COLUMN_1_3_INTERNAL_NAME));
        assertEquals(12.9, result.getResult().get(2).get(COLUMN_1_4_INTERNAL_NAME));
        assertEquals(0.0, result.getResult().get(2).get(COLUMN_1_5_INTERNAL_NAME));
    }

    @Test
    public void selectAll_succeeds() throws TableNotFoundException, DatabaseConnectionException,
            DatabaseNotFoundException, ImageNotSupportedException, TableMalformedException, PaginationException,
            ContainerNotFoundException, QueryMalformedException, UserNotFoundException {
        final Long page = 0L;
        final Long size = 10L;

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(tableRepository.find(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(Optional.of(TABLE_1));

        /* test */
        queryService.findAll(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, Instant.now(), page, size, USER_1_PRINCIPAL);
    }

    @Test
    public void selectAll_noTable_fails() {
        final Long page = 0L;
        final Long size = 10L;

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(tableRepository.find(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(Optional.of(TABLE_1));

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            queryService.findAll(CONTAINER_1_ID, DATABASE_1_ID, -1L, Instant.now(), page, size, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void selectAll_noDatabase_fails() {
        final Long page = 0L;
        final Long size = 10L;

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(tableRepository.find(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(Optional.of(TABLE_1));

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            queryService.findAll(CONTAINER_1_ID, -1L, TABLE_1_ID, Instant.now(), page, size, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void insert_columns_fails() {
        final TableCsvDto request = TableCsvDto.builder()
                .data(Map.of("key", "some_value"))
                .build();

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(tableRepository.find(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(Optional.of(TABLE_1));

        /* test */
        assertThrows(TableMalformedException.class, () -> {
            queryService.insert(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, request, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void findAll_timestampMissing_succeeds() throws TableNotFoundException, DatabaseConnectionException,
            TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException, PaginationException,
            ContainerNotFoundException, QueryMalformedException, UserNotFoundException {

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(tableRepository.find(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(Optional.of(TABLE_1));

        /* test */
        queryService.findAll(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, null, null, null, USER_1_PRINCIPAL);
    }

    @Test
    public void findAll_timestampBeforeCreation_succeeds() throws TableNotFoundException, DatabaseConnectionException,
            TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException, PaginationException,
            ContainerNotFoundException, QueryMalformedException, UserNotFoundException {
        final Instant timestamp = DATABASE_1_CREATED.minus(1, ChronoUnit.SECONDS);

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(tableRepository.find(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(Optional.of(TABLE_1));

        /* test */
        queryService.findAll(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, timestamp, null, null, USER_1_PRINCIPAL);
    }

    @Test
    public void execute_succeeds() throws DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, ContainerNotFoundException, QueryMalformedException,
            UserNotFoundException, QueryStoreException, ColumnParseException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement("SELECT n.`firstname`, n.`lastname`, z.`animal_name`, z.`legs` FROM `likes` l JOIN `names` n ON l.`name_id` = n.`id` JOIN `mock_view` z ON z.`id` = l.`zoo_id`")
                .build();

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_2_ID, DATABASE_2_ID))
                .thenReturn(Optional.of(DATABASE_2));
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        final QueryResultDto response = queryService.execute(CONTAINER_2_ID, DATABASE_2_ID, request, QueryTypeDto.QUERY,
                USER_1_PRINCIPAL, 0L, 100L, null, null);
        assertEquals(4L, response.getResultNumber());
        assertNotNull(response.getResult());
        final List<Map<String, Object>> result = response.getResult();
        assertEquals(BigInteger.valueOf(4L), result.get(0).get("legs"));
        assertEquals("boar", result.get(0).get("animal_name"));
        assertEquals("Moritz", result.get(0).get("firstname"));
        assertEquals("Staudinger", result.get(0).get("lastname"));
        assertEquals(BigInteger.valueOf(4L), result.get(1).get("legs"));
        assertEquals("cavy", result.get(1).get("animal_name"));
        assertEquals("Moritz", result.get(1).get("firstname"));
        assertEquals("Staudinger", result.get(1).get("lastname"));
        assertEquals(BigInteger.valueOf(4L), result.get(2).get("legs"));
        assertEquals("bear", result.get(2).get("animal_name"));
        assertEquals("Eva", result.get(2).get("firstname"));
        assertEquals("Gergely", result.get(2).get("lastname"));
        assertEquals(BigInteger.valueOf(4L), result.get(3).get("legs"));
        assertEquals("bear", result.get(3).get("animal_name"));
        assertEquals("Cornelia", result.get(3).get("firstname"));
        assertEquals("Michlits", result.get(3).get("lastname"));
    }

    @SneakyThrows
    private static Instant toInstant(String str) {
        final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .parseCaseInsensitive() /* case insensitive to parse JAN and FEB */
                .appendPattern("yyyy-MM-dd")
                .toFormatter(Locale.ENGLISH);
        final LocalDate date = LocalDate.parse(str, formatter);
        return date.atStartOfDay(ZoneId.of("UTC"))
                .toInstant();
    }

}
