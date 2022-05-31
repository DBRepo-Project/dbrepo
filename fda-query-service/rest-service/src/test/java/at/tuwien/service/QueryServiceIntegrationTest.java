package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.query.ExecuteStatementDto;
import at.tuwien.api.database.query.ImportDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.database.table.TableCsvDto;
import at.tuwien.config.DockerConfig;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.exception.*;
import at.tuwien.repository.jpa.*;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Network;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
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

import javax.persistence.PersistenceException;
import java.io.File;
import java.math.BigInteger;
import java.sql.SQLException;
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
            PaginationException, ContainerNotFoundException {

        /* test */
        final QueryResultDto result = queryService.findAll(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, Instant.now(),
                null, null);
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

//    @Test
//    public void execute_succeeds()
//            throws DatabaseNotFoundException, ImageNotSupportedException, QueryMalformedException,
//            TableNotFoundException, QueryStoreException, ContainerNotFoundException,
//            TableMalformedException, ColumnParseException {
//        final ExecuteStatementDto request = ExecuteStatementDto.builder()
//                .statement(QUERY_1_STATEMENT)
//                .build();
//
//        /* test */
//        final QueryResultDto response = queryService.execute(CONTAINER_1_ID, DATABASE_1_ID, request, null, null);
//        assertEquals(3, response.getResult().size());
//    }
//
//    /**
//     * ref #135 https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/issues/135
//     */
//    @Test
//    public void execute_onlyNumber_succeeds() throws DatabaseNotFoundException, ImageNotSupportedException,
//            QueryMalformedException, TableNotFoundException, QueryStoreException, ContainerNotFoundException,
//            TableMalformedException, ColumnParseException {
//        final ExecuteStatementDto request = ExecuteStatementDto.builder()
//                .statement("SELECT `id` FROM `weather_aus`")
//                .build();
//
//        /* test */
//        final QueryResultDto response = queryService.execute(CONTAINER_1_ID, DATABASE_1_ID, request, null, null);
//        assertEquals(3, response.getResult().size());
//    }
//
//    @Test
//    public void execute_onlyString_succeeds() throws DatabaseNotFoundException, ImageNotSupportedException,
//            QueryMalformedException, TableNotFoundException, QueryStoreException, ContainerNotFoundException,
//            TableMalformedException, ColumnParseException {
//        final ExecuteStatementDto request = ExecuteStatementDto.builder()
//                .statement("SELECT `location` FROM `weather_aus`")
//                .build();
//
//        /* test */
//        final QueryResultDto response = queryService.execute(CONTAINER_1_ID, DATABASE_1_ID, request, null, null);
//        assertEquals(3, response.getResult().size());
//    }
//
//    @Test
//    public void execute_onlyDate_succeeds() throws DatabaseNotFoundException, ImageNotSupportedException,
//            QueryMalformedException, TableNotFoundException, QueryStoreException, ContainerNotFoundException,
//            TableMalformedException, ColumnParseException {
//        final ExecuteStatementDto request = ExecuteStatementDto.builder()
//                .statement("SELECT `date` FROM `weather_aus`")
//                .build();
//
//        /* test */
//        final QueryResultDto response = queryService.execute(CONTAINER_1_ID, DATABASE_1_ID, request, null, null);
//        assertEquals(3, response.getResult().size());
//    }
//
//    @Test
//    public void execute_join_succeeds() throws DatabaseNotFoundException, ImageNotSupportedException,
//            QueryMalformedException, TableNotFoundException, QueryStoreException, ContainerNotFoundException,
//            TableMalformedException, ColumnParseException {
//        final ExecuteStatementDto request = ExecuteStatementDto.builder()
//                .statement("SELECT `mintemp`, l.`lat`, l.`lng` FROM `weather_aus` w JOIN `weather_location` l ON " +
//                        "w.location = l.location")
//                .build();
//
//        /* test */
//        final QueryResultDto response = queryService.execute(CONTAINER_1_ID, DATABASE_1_ID, request, null, null);
//        assertEquals(3, response.getResultNumber());
//        assertEquals(13.4, response.getResult().get(0).get("MinTemp"));
//        assertEquals(-36.0653583, response.getResult().get(0).get("lat"));
//        assertEquals(146.9112214, response.getResult().get(0).get("lng"));
//        assertEquals(7.4, response.getResult().get(1).get("MinTemp"));
//        assertEquals(-36.0653583, response.getResult().get(1).get("lat"));
//        assertEquals(146.9112214, response.getResult().get(1).get("lng"));
//        assertEquals(12.9, response.getResult().get(2).get("MinTemp"));
//        assertEquals(-36.0653583, response.getResult().get(2).get("lat"));
//        assertEquals(146.9112214, response.getResult().get(2).get("lng"));
//    }
//
//    @Test
//    public void execute_joinNotEscaped_succeeds() throws DatabaseNotFoundException, ImageNotSupportedException,
//            QueryMalformedException, TableNotFoundException, QueryStoreException, ContainerNotFoundException,
//            TableMalformedException, ColumnParseException {
//        final ExecuteStatementDto request = ExecuteStatementDto.builder()
//                .statement("SELECT mintemp, l.lat, l.lng FROM weather_aus w JOIN weather_location l ON " +
//                        "w.location = l.location")
//                .build();
//
//        /* test */
//        final QueryResultDto response = queryService.execute(CONTAINER_1_ID, DATABASE_1_ID, request, null, null);
//        assertEquals(3, response.getResultNumber());
//        assertEquals(13.4, response.getResult().get(0).get("MinTemp"));
//        assertEquals(-36.0653583, response.getResult().get(0).get("lat"));
//        assertEquals(146.9112214, response.getResult().get(0).get("lng"));
//        assertEquals(7.4, response.getResult().get(1).get("MinTemp"));
//        assertEquals(-36.0653583, response.getResult().get(1).get("lat"));
//        assertEquals(146.9112214, response.getResult().get(1).get("lng"));
//        assertEquals(12.9, response.getResult().get(2).get("MinTemp"));
//        assertEquals(-36.0653583, response.getResult().get(2).get("lat"));
//        assertEquals(146.9112214, response.getResult().get(2).get("lng"));
//    }
//
//    @Test
//    public void execute_join2_succeeds() throws DatabaseNotFoundException, ImageNotSupportedException,
//            QueryMalformedException, TableNotFoundException, QueryStoreException, ContainerNotFoundException,
//            TableMalformedException, ColumnParseException {
//        final ExecuteStatementDto request = ExecuteStatementDto.builder()
//                .statement("SELECT z.id FROM zoo z INNER JOIN names n ON n.id = z.id")
//                .build();
//
//        /* test */
//        final QueryResultDto response = queryService.execute(CONTAINER_2_ID, DATABASE_2_ID, request, null, null);
//        assertEquals(5, response.getResultNumber());
//        assertEquals(BigInteger.valueOf(1L), response.getResult().get(0).get("id"));
//        assertEquals(BigInteger.valueOf(2L), response.getResult().get(1).get("id"));
//        assertEquals(BigInteger.valueOf(3L), response.getResult().get(2).get("id"));
//        assertEquals(BigInteger.valueOf(4L), response.getResult().get(3).get("id"));
//        assertEquals(BigInteger.valueOf(5L), response.getResult().get(4).get("id"));
//    }
//
//    @Test
//    public void execute_joinWithSemicolon_succeeds() throws DatabaseNotFoundException, ImageNotSupportedException,
//            QueryMalformedException, TableNotFoundException, QueryStoreException, ContainerNotFoundException,
//            TableMalformedException, ColumnParseException {
//        final ExecuteStatementDto request = ExecuteStatementDto.builder()
//                .statement("SELECT z.id FROM zoo z INNER JOIN names n ON n.id = z.id;")
//                .build();
//
//        /* test */
//        final QueryResultDto response = queryService.execute(CONTAINER_2_ID, DATABASE_2_ID, request, null, null);
//        assertEquals(5, response.getResultNumber());
//        assertEquals(BigInteger.valueOf(1L), response.getResult().get(0).get("id"));
//        assertEquals(BigInteger.valueOf(2L), response.getResult().get(1).get("id"));
//        assertEquals(BigInteger.valueOf(3L), response.getResult().get(2).get("id"));
//        assertEquals(BigInteger.valueOf(4L), response.getResult().get(3).get("id"));
//        assertEquals(BigInteger.valueOf(5L), response.getResult().get(4).get("id"));
//    }
//
//    @Test
//    public void execute_joinColumnNotExists_fails() {
//        final ExecuteStatementDto request = ExecuteStatementDto.builder()
//                .statement("SELECT z.id2 FROM zoo z INNER JOIN names n ON n.id = z.id")
//                .build();
//
//        /* test */
//        assertThrows(QueryMalformedException.class, () -> {
//            queryService.execute(CONTAINER_2_ID, DATABASE_2_ID, request, null, null);
//        });
//    }
//
//    @Test
//    public void execute_joinWithWhere_succeeds() throws DatabaseNotFoundException, ImageNotSupportedException,
//            QueryMalformedException, TableNotFoundException, QueryStoreException, ContainerNotFoundException,
//            TableMalformedException, ColumnParseException {
//        final ExecuteStatementDto request = ExecuteStatementDto.builder()
//                .statement("SELECT `mintemp`, l.`lat`, l.`lng` FROM `weather_aus` w JOIN `weather_location` l ON " +
//                        "w.location = l.location WHERE `mintemp` > 13")
//                .build();
//
//        /* test */
//        final QueryResultDto response = queryService.execute(CONTAINER_1_ID, DATABASE_1_ID, request, null, null);
//        assertEquals(1, response.getResultNumber());
//        assertEquals(13.4, response.getResult().get(0).get("MinTemp"));
//        assertEquals(-36.0653583, response.getResult().get(0).get("lat"));
//        assertEquals(146.9112214, response.getResult().get(0).get("lng"));
//    }
//
//    @Test
//    public void execute_modifyData_fails() {
//        final ExecuteStatementDto request = ExecuteStatementDto.builder()
//                .statement("DELETE FROM `weather_aus`;")
//                .build();
//
//        /* test */
//        assertThrows(QueryMalformedException.class, () -> {
//            queryService.execute(CONTAINER_1_ID, DATABASE_1_ID, request, 0L, 0L);
//        });
//    }
//
//    @Test
//    @Disabled
//    public void execute_databaseNotExists_fails() {
//        final ExecuteStatementDto request = ExecuteStatementDto.builder()
//                .statement(QUERY_1_STATEMENT)
//                .build();
//
//        /* test */
//        assertThrows(DatabaseNotFoundException.class, () -> {
//            //FIXME
//            queryService.execute(CONTAINER_1_ID, 9999L, request, 0L, 0L);
//        });
//    }
//
//    @Test
//    @Disabled
//    public void execute_tableNotFound_fails() {
//        final ExecuteStatementDto request = ExecuteStatementDto.builder()
//                .statement(QUERY_1_STATEMENT)
//                .build();
//
//        /* test */
//        assertThrows(PersistenceException.class, () -> {
//            //FIXME
//            queryService.execute(CONTAINER_1_ID, DATABASE_1_ID, request, 0L, 0L);
//        });
//    }
//
//    @Test
//    @Disabled
//    public void execute_columnNotFound_fails() {
//        final ExecuteStatementDto request = ExecuteStatementDto.builder()
//                .statement("SELECT `local` FROM `weather_aus`")
//                .build();
//
//        /* test */
//        assertThrows(PersistenceException.class, () -> {
//            //FIXME
//            queryService.execute(CONTAINER_1_ID, DATABASE_1_ID, request, 0L, 0L);
//        });
//    }
//
//    @Test
//    @Disabled
//    public void execute_statementNull_fails() {
//        final ExecuteStatementDto request = ExecuteStatementDto.builder()
//                .statement(null)
//                .build();
//
//        /* test */
//        assertThrows(QueryMalformedException.class, () -> {
//            //FIXME
//            queryService.execute(CONTAINER_1_ID, DATABASE_1_ID, request, 0L, 0L);
//        });
//    }
//
//    @Test
//    @Disabled
//    public void insert_succeeds() throws TableNotFoundException, DatabaseNotFoundException,
//            TableMalformedException, ImageNotSupportedException, SQLException, ContainerNotFoundException {
//        final ImportDto request = ImportDto.builder()
//                .location("/tmp/csv_12.csv")
//                .build();
//
//        /* test */
//        final Integer rows = queryService.insert(CONTAINER_1_ID, DATABASE_3_ID, TABLE_3_ID, request);
//        assertEquals(9999, rows);
//        final List<List<String>> response = MariaDbConfig.select(TABLE_3, 1);
//        assertEquals("1", response.get(0).get(0));
//        assertEquals("2", response.get(0).get(1));
//        assertEquals("1", response.get(0).get(2));
//        assertEquals("2017-01-15", response.get(0).get(3));
//        assertEquals("2076", response.get(0).get(4));
//        assertEquals("6", response.get(0).get(5));
//        assertEquals("1", response.get(0).get(6));
//        assertEquals("6030", response.get(0).get(7));
//        assertEquals("0", response.get(0).get(8));
//        assertEquals("DEP4", response.get(0).get(9));
//        assertEquals("2017-01-15", response.get(0).get(10));
//        assertEquals("17580", response.get(0).get(11));
//        assertEquals("17562", response.get(0).get(12));
//        assertEquals("17580", response.get(0).get(13));
//        assertEquals("17562", response.get(0).get(14));
//        assertEquals("2", response.get(0).get(15));
//        assertEquals("1357", response.get(0).get(16));
//        assertEquals("1", response.get(0).get(17));
//        assertEquals("KALK", response.get(0).get(18));
//        assertEquals("2017-01-15", response.get(0).get(19));
//        assertEquals("17622", response.get(0).get(20));
//        assertEquals("17647", response.get(0).get(21));
//        assertEquals("17622", response.get(0).get(22));
//        assertEquals("17664", response.get(0).get(23));
//        assertEquals("8538", response.get(0).get(24));
//        assertEquals("41253", response.get(0).get(25));
//        assertEquals("15", response.get(0).get(26));
//        assertEquals("2", response.get(0).get(27));
//        assertEquals("15", response.get(0).get(28));
//        assertEquals("DEP4 - KALK", response.get(0).get(29));
//        assertEquals("135780", response.get(0).get(30));
//        assertEquals("2251", response.get(0).get(31));
//        assertEquals("1906", response.get(0).get(32));
//        assertEquals("12462", response.get(0).get(33));
//        assertEquals("10563", response.get(0).get(34));
//
//    }
//
//    @Test
//    @Disabled
//    public void insert_large_succeeds() throws TableNotFoundException, DatabaseNotFoundException,
//            TableMalformedException, ImageNotSupportedException, SQLException, ContainerNotFoundException {
//        final ImportDto request = ImportDto.builder()
//                .location("/tmp/csv_13.csv")
//                .build();
//
//        /* test */
//        final Integer rows = queryService.insert(CONTAINER_1_ID, DATABASE_3_ID, TABLE_3_ID, request);
//        assertEquals(1397856, rows);
//        final List<List<String>> response = MariaDbConfig.select(TABLE_3, 1);
//        assertEquals("1", response.get(0).get(0));
//        assertEquals("2", response.get(0).get(1));
//        assertEquals("1", response.get(0).get(2));
//        assertEquals("2017-01-15", response.get(0).get(3));
//        assertEquals("2076", response.get(0).get(4));
//        assertEquals("6", response.get(0).get(5));
//        assertEquals("1", response.get(0).get(6));
//        assertEquals("6030", response.get(0).get(7));
//        assertEquals("0", response.get(0).get(8));
//        assertEquals("DEP4", response.get(0).get(9));
//        assertEquals("2017-01-15", response.get(0).get(10));
//        assertEquals("17580", response.get(0).get(11));
//        assertEquals("17562", response.get(0).get(12));
//        assertEquals("17580", response.get(0).get(13));
//        assertEquals("17562", response.get(0).get(14));
//        assertEquals("2", response.get(0).get(15));
//        assertEquals("1357", response.get(0).get(16));
//        assertEquals("1", response.get(0).get(17));
//        assertEquals("KALK", response.get(0).get(18));
//        assertEquals("2017-01-15", response.get(0).get(19));
//        assertEquals("17622", response.get(0).get(20));
//        assertEquals("17647", response.get(0).get(21));
//        assertEquals("17622", response.get(0).get(22));
//        assertEquals("17664", response.get(0).get(23));
//        assertEquals("8538", response.get(0).get(24));
//        assertEquals("41253", response.get(0).get(25));
//        assertEquals("15", response.get(0).get(26));
//        assertEquals("2", response.get(0).get(27));
//        assertEquals("15", response.get(0).get(28));
//        assertEquals("DEP4 - KALK", response.get(0).get(29));
//        assertEquals("135780", response.get(0).get(30));
//        assertEquals("2251", response.get(0).get(31));
//        assertEquals("1906", response.get(0).get(32));
//        assertEquals("12462", response.get(0).get(33));
//        assertEquals("10563", response.get(0).get(34));
//    }
//
//    @Test
//    @Disabled
//    public void insert_sensor_succeeds() throws TableNotFoundException, DatabaseNotFoundException,
//            TableMalformedException, ImageNotSupportedException, SQLException, ContainerNotFoundException {
//        final TableCsvDto request = TableCsvDto.builder()
//                .data(new HashMap<>() {{
//                    put("linie", 2);
//                    put("richtung", 1);
//                    put("betriebsdatum", "15.01.17");
//                    put("fahrzeug", 2076);
//                    put("kurs", 6);
//                    put("seq_von", 1);
//                    put("halt_diva_von", 6030);
//                    put("halt_punkt_diva_von", 0);
//                    put("halt_kurz_von1", "DEP4");
//                    put("datum_von", "15.01.17");
//                    put("soll_an_von", 17580);
//                    put("ist_an_von", 17562);
//                    put("soll_ab_von", 17580);
//                    put("ist_ab_von", 17562);
//                    put("seq_nach", 2);
//                    put("halt_diva_nach", 1357);
//                    put("halt_punkt_diva_nach", 1);
//                    put("halt_kurz_nach1", "KALK");
//                    put("datum_nach", "15.01.17");
//                    put("soll_an_nach", 17622);
//                    put("ist_an_nach1", 17647);
//                    put("soll_ab_nach", 17622);
//                    put("ist_ab_nach", 17664);
//                    put("fahrt_id", 8538);
//                    put("fahrweg_id", 41253);
//                    put("fw_no", 15);
//                    put("fw_typ", 2);
//                    put("fw_kurz", 15);
//                    put("fw_lang", "DEP4 - KALK");
//                    put("umlauf_von", 135780);
//                    put("halt_id_von", 2251);
//                    put("halt_id_nach", 1906);
//                    put("halt_punkt_id_von", 12462);
//                    put("halt_punkt_id_nach", 10563);
//                }})
//                .build();
//
//        /* test */
//        final Integer rows = queryService.insert(CONTAINER_1_ID, DATABASE_3_ID, TABLE_3_ID, request);
//        assertEquals(1, rows);
//        final List<List<String>> response = MariaDbConfig.select(TABLE_3, 1);
//        assertEquals("1", response.get(0).get(0));
//        assertEquals("2", response.get(0).get(1));
//        assertEquals("1", response.get(0).get(2));
//        assertEquals("2017-01-15", response.get(0).get(3));
//        assertEquals("2076", response.get(0).get(4));
//        assertEquals("6", response.get(0).get(5));
//        assertEquals("1", response.get(0).get(6));
//        assertEquals("6030", response.get(0).get(7));
//        assertEquals("0", response.get(0).get(8));
//        assertEquals("DEP4", response.get(0).get(9));
//        assertEquals("2017-01-15", response.get(0).get(10));
//        assertEquals("17580", response.get(0).get(11));
//        assertEquals("17562", response.get(0).get(12));
//        assertEquals("17580", response.get(0).get(13));
//        assertEquals("17562", response.get(0).get(14));
//        assertEquals("2", response.get(0).get(15));
//        assertEquals("1357", response.get(0).get(16));
//        assertEquals("1", response.get(0).get(17));
//        assertEquals("KALK", response.get(0).get(18));
//        assertEquals("2017-01-15", response.get(0).get(19));
//        assertEquals("17622", response.get(0).get(20));
//        assertEquals("17647", response.get(0).get(21));
//        assertEquals("17622", response.get(0).get(22));
//        assertEquals("17664", response.get(0).get(23));
//        assertEquals("8538", response.get(0).get(24));
//        assertEquals("41253", response.get(0).get(25));
//        assertEquals("15", response.get(0).get(26));
//        assertEquals("2", response.get(0).get(27));
//        assertEquals("15", response.get(0).get(28));
//        assertEquals("DEP4 - KALK", response.get(0).get(29));
//        assertEquals("135780", response.get(0).get(30));
//        assertEquals("2251", response.get(0).get(31));
//        assertEquals("1906", response.get(0).get(32));
//        assertEquals("12462", response.get(0).get(33));
//        assertEquals("10563", response.get(0).get(34));
//    }

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
