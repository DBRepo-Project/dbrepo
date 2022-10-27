package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.config.DockerConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.exception.*;
import at.tuwien.repository.jpa.*;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Network;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.http.auth.BasicUserPrincipal;
import org.junit.Rule;
import org.junit.rules.Timeout;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.math.BigInteger;
import java.security.Principal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.*;

import static at.tuwien.config.DockerConfig.dockerClient;
import static at.tuwien.config.DockerConfig.hostConfig;
import static org.junit.jupiter.api.Assertions.*;


@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class QueryServiceIntegrationTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @Autowired
    private QueryService queryService;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private TableRepository tableRepository;

    @Autowired
    private ConceptRepository conceptRepository;

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
                .withHostConfig(hostConfig.withNetworkMode("fda-userdb"))
                .withName(CONTAINER_1_INTERNALNAME)
                .withIpv4Address(CONTAINER_1_IP)
                .withHostName(CONTAINER_1_INTERNALNAME)
                .withEnv("MARIADB_USER=mariadb", "MARIADB_PASSWORD=mariadb", "MARIADB_ROOT_PASSWORD=mariadb",
                        "MARIADB_DATABASE=weather")
                .withBinds(Bind.parse(bind), Bind.parse("/tmp:/tmp"))
                .exec();
        CONTAINER_1.setHash(response.getId());
        DockerConfig.startContainer(CONTAINER_1);

        /* create container */
        final String bind2 = new File(
                "./src/test/resources/zoo").toPath().toAbsolutePath() + ":/docker-entrypoint-initdb.d";
        log.trace("container bind {}", bind);
        final CreateContainerResponse response2 =
                dockerClient.createContainerCmd(IMAGE_1_REPOSITORY + ":" + IMAGE_1_TAG)
                        .withHostConfig(hostConfig.withNetworkMode("fda-userdb"))
                        .withName(CONTAINER_2_INTERNALNAME)
                        .withIpv4Address(CONTAINER_2_IP)
                        .withHostName(CONTAINER_2_INTERNALNAME)
                        .withEnv("MARIADB_USER=mariadb", "MARIADB_PASSWORD=mariadb", "MARIADB_ROOT_PASSWORD=mariadb",
                                "MARIADB_DATABASE=zoo")
                        .withBinds(Bind.parse(bind2), Bind.parse("/tmp:/tmp"))
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
        /* create databases */
        databaseRepository.save(DATABASE_1);
        databaseRepository.save(DATABASE_2);
        databaseRepository.save(DATABASE_3);
        /* create tables */
        TABLE_1.setDatabase(DATABASE_1);
        tableRepository.save(TABLE_1);
        TABLE_2.setDatabase(DATABASE_1);
        tableRepository.save(TABLE_2);
        TABLE_3.setDatabase(DATABASE_3);
        tableRepository.save(TABLE_3);
        TABLE_4.setDatabase(DATABASE_2);
        tableRepository.save(TABLE_4);
        TABLE_5.setDatabase(DATABASE_2);
        tableRepository.save(TABLE_5);
        /* create columns */
        TABLE_1.setColumns(TABLE_1_COLUMNS);
        tableRepository.save(TABLE_1);
        TABLE_2.setColumns(TABLE_2_COLUMNS);
        tableRepository.save(TABLE_2);
        TABLE_4.setColumns(TABLE_4_COLUMNS);
        tableRepository.save(TABLE_4);
        TABLE_5.setColumns(TABLE_5_COLUMNS);
        tableRepository.save(TABLE_5);
    }

    @Test
    public void findAll_succeeds() throws DatabaseNotFoundException, ImageNotSupportedException,
            TableMalformedException, TableNotFoundException, DatabaseConnectionException,
            PaginationException, ContainerNotFoundException, QueryMalformedException, UserNotFoundException {
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);

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
