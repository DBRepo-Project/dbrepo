package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.query.QueryResultDto;
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

import static at.tuwien.config.DockerConfig.dockerClient;
import static at.tuwien.config.DockerConfig.hostConfig;
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

    @Autowired
    private QueryService queryService;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private TableRepository tableRepository;

    @Autowired
    private TableColumnRepository tableColumnRepository;

    @Autowired
    private ConceptRepository conceptRepository;

    @Autowired
    private ContainerRepository containerRepository;

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
                //.withBinds(Bind.parse(bind), Bind.parse("/tmp:/tmp"))
                .exec();
        CONTAINER_1.setHash(response.getId());
        DockerConfig.startContainer(CONTAINER_1);

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
                        //.withBinds(Bind.parse(bind2), Bind.parse("/tmp:/tmp"))
                        .exec();
        CONTAINER_1.setHash(response.getId());
        CONTAINER_2.setHash(response2.getId());
        DockerConfig.startContainer(CONTAINER_1);
        DockerConfig.startContainer(CONTAINER_2);
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
    @Transactional
    public void beforeEach() {
        /* image */
        imageRepository.save(IMAGE_1);
        /* concepts */
        conceptRepository.save(CONCEPT_1);
        /* image dates */
        IMAGE_1.setDateFormats(List.of(IMAGE_DATE_1, IMAGE_DATE_2));
        imageRepository.save(IMAGE_1);
        containerRepository.save(CONTAINER_1);
        containerRepository.save(CONTAINER_2);
        containerRepository.save(CONTAINER_3);
        /* create databases */
        databaseRepository.save(DATABASE_1);
        databaseRepository.save(DATABASE_2);
        databaseRepository.save(DATABASE_3);
        /* create tables 1 */
        TABLE_1.setDatabase(DATABASE_1);
        tableRepository.save(TABLE_1);
        TABLE_1.setColumns(TABLE_1_COLUMNS);
        tableRepository.save(TABLE_1);
        TABLE_1_COLUMNS.forEach(column -> column.setTable(TABLE_1));
        /* create tables 2 */
        TABLE_2.setDatabase(DATABASE_1);
        tableRepository.save(TABLE_2);
        TABLE_2.setColumns(TABLE_2_COLUMNS);
        tableRepository.save(TABLE_2);
        TABLE_2_COLUMNS.forEach(column -> column.setTable(TABLE_2));
        /* create tables 3 */
        TABLE_3.setDatabase(DATABASE_3);
        tableRepository.save(TABLE_3);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = "RESEARCHER")
    public void findAll_succeeds() throws DatabaseNotFoundException, ImageNotSupportedException,
            TableMalformedException, TableNotFoundException, DatabaseConnectionException,
            PaginationException, ContainerNotFoundException, QueryMalformedException, UserNotFoundException {
        final Principal principal = SecurityContextHolder.getContext().getAuthentication();

        /* test */
        final QueryResultDto result = queryService.findAll(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, Instant.now(),
                null, null, principal);
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
    @WithMockUser(username = USER_1_USERNAME, roles = "RESEARCHER")
    public void selectAll_succeeds() throws TableNotFoundException, DatabaseConnectionException,
            DatabaseNotFoundException, ImageNotSupportedException, TableMalformedException, PaginationException,
            ContainerNotFoundException, QueryMalformedException, SQLException, UserNotFoundException {
        final Long page = 0L;
        final Long size = 10L;
        final Principal principal = SecurityContextHolder.getContext().getAuthentication();

        /* test */
        queryService.findAll(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, Instant.now(), page, size, principal);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = "RESEARCHER")
    public void selectAll_noTable_fails() {
        final Long page = 0L;
        final Long size = 10L;
        final Principal principal = SecurityContextHolder.getContext().getAuthentication();

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            queryService.findAll(CONTAINER_1_ID, DATABASE_1_ID, -1L, Instant.now(), page, size, principal);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = "RESEARCHER")
    public void selectAll_noDatabase_fails() {
        final Long page = 0L;
        final Long size = 10L;
        final Principal principal = SecurityContextHolder.getContext().getAuthentication();

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            queryService.findAll(CONTAINER_1_ID, -1L, TABLE_1_ID, Instant.now(), page, size, principal);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = "RESEARCHER")
    public void insert_columns_fails() {
        final TableCsvDto request = TableCsvDto.builder()
                .data(Map.of("key", "some_value"))
                .build();
        final Principal principal = SecurityContextHolder.getContext().getAuthentication();

        /* test */
        assertThrows(TableMalformedException.class, () -> {
            queryService.insert(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, request, principal);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = "RESEARCHER")
    public void findAll_timestampMissing_succeeds() throws TableNotFoundException, DatabaseConnectionException,
            TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException, PaginationException,
            ContainerNotFoundException, QueryMalformedException, UserNotFoundException {
        final Principal principal = SecurityContextHolder.getContext().getAuthentication();

        /* test */
        queryService.findAll(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, null, null, null, principal);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = "RESEARCHER")
    public void findAll_timestampBeforeCreation_succeeds() throws TableNotFoundException, DatabaseConnectionException,
            TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException, PaginationException,
            ContainerNotFoundException, QueryMalformedException, UserNotFoundException {
        final Instant timestamp = DATABASE_1_CREATED.minus(1, ChronoUnit.SECONDS);
        final Principal principal = SecurityContextHolder.getContext().getAuthentication();

        /* test */
        queryService.findAll(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, timestamp, null, null, principal);
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
