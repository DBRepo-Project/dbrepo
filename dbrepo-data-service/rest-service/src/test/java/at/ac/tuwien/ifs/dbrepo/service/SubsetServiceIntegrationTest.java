package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.database.query.*;
import at.ac.tuwien.ifs.dbrepo.core.api.identifier.IdentifierBriefDto;
import at.ac.tuwien.ifs.dbrepo.config.MariaDbConfig;
import at.ac.tuwien.ifs.dbrepo.config.MariaDbContainerConfig;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.gateway.MetadataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest
@ExtendWith(SpringExtension.class)
@Testcontainers
public class SubsetServiceIntegrationTest extends BaseTest {

    @Autowired
    private SubsetService subsetService;

    @MockitoBean
    private MetadataServiceGateway metadataServiceGateway;

    public static Stream<Arguments> create_arguments() {
        return Stream.of(
                Arguments.arguments("singleQuote", "' DROP TABLE `weather_location`; --"),
                Arguments.arguments("singleQuoteEscaped", "\' DROP TABLE `weather_location`; --"),
                Arguments.arguments("doubleQuote", "\" DROP TABLE `weather_location`; --")
        );
    }

    @Container
    private static MariaDBContainer<?> mariaDBContainer = MariaDbContainerConfig.getContainer();

    @BeforeEach
    public void beforeEach() throws SQLException {
        /* metadata database */
        MariaDbConfig.dropDatabase(CONTAINER_1_PRIVILEGED_DTO, DATABASE_1_INTERNAL_NAME);
        MariaDbConfig.createInitDatabase(DATABASE_1_PRIVILEGED_DTO);
    }

    @Test
    public void findAll_succeeds()  throws SQLException, QueryNotFoundException,
            RemoteUnavailableException, MetadataServiceException, DatabaseNotFoundException, InterruptedException,
            UserNotFoundException {

        /* test */
        final List<QueryDto> response = findAll_generic(null);
        assertEquals(2, response.size());
        assertNotNull(response.get(0).getId());
        assertNotNull(response.get(1).getId());
    }

    @Test
    public void findAll_onlyPersisted_succeeds()  throws SQLException, QueryNotFoundException,
            RemoteUnavailableException, MetadataServiceException, DatabaseNotFoundException, InterruptedException,
            UserNotFoundException {

        /* test */
        final List<QueryDto> response = findAll_generic(true);
        assertEquals(1, response.size());
        assertNotNull(response.get(0).getId());
    }

    @Test
    public void findAll_onlyNonPersisted_succeeds() throws SQLException, QueryNotFoundException,
            RemoteUnavailableException, MetadataServiceException, DatabaseNotFoundException, InterruptedException,
            UserNotFoundException {

        /* test */
        final List<QueryDto> response = findAll_generic(false);
        assertEquals(1, response.size());
        assertNotNull(response.get(0).getId());
    }

    @Test
    public void findById_succeeds() throws SQLException, QueryNotFoundException, UserNotFoundException,
            RemoteUnavailableException, MetadataServiceException, DatabaseNotFoundException, InterruptedException {

        /* mock */
        final UUID queryId = MariaDbConfig.insertQueryStore(DATABASE_1_PRIVILEGED_DTO, QUERY_1_DTO, USER_1_ID);

        /* test */
        findById_generic(queryId);
    }

    @Test
    public void findById_fails() {

        /* test */
        assertThrows(QueryNotFoundException.class, () -> {
            findById_generic(UUID.randomUUID());
        });
    }

    @Test
    public void persist_succeeds() throws SQLException, QueryStorePersistException, QueryNotFoundException,
            UserNotFoundException, RemoteUnavailableException, MetadataServiceException, DatabaseNotFoundException,
            InterruptedException {

        /* mock */
        final UUID queryId2 = MariaDbConfig.insertQueryStore(DATABASE_1_PRIVILEGED_DTO, QUERY_2_DTO, USER_1_ID);
        when(metadataServiceGateway.getUserById(USER_1_ID))
                .thenReturn(USER_1_DTO);

        /* test */
        persist_generic(queryId2, List.of(IDENTIFIER_5_BRIEF_DTO), true);
        final QueryDto response = subsetService.findById(DATABASE_1_PRIVILEGED_DTO, queryId2);
        assertEquals(queryId2, response.getId());
        assertTrue(response.getIsPersisted());
    }

    @Test
    public void persist_unPersist_succeeds() throws SQLException, QueryStorePersistException, QueryNotFoundException,
            UserNotFoundException, RemoteUnavailableException, MetadataServiceException, DatabaseNotFoundException,
            InterruptedException {

        /* mock */
        final UUID queryId1 = MariaDbConfig.insertQueryStore(DATABASE_1_PRIVILEGED_DTO, QUERY_1_DTO, USER_1_ID);
        when(metadataServiceGateway.getUserById(USER_1_ID))
                .thenReturn(USER_1_DTO);

        /* test */
        persist_generic(queryId1, List.of(IDENTIFIER_2_BRIEF_DTO), false);
        final QueryDto response = subsetService.findById(DATABASE_1_PRIVILEGED_DTO, queryId1);
        assertEquals(queryId1, response.getId());
        assertFalse(response.getIsPersisted());
    }

    @Test
    public void getData_succeeds() throws QueryMalformedException, TableNotFoundException {
        final List<List<String>> expected = List.of(
                List.of("1", "2008-12-01", "Albury", "13.4", "0.6"),
                List.of("2", "2008-12-02", "Albury", "7.4", "0.0"),
                List.of("3", "2008-12-03", "Albury", "12.9", "0.0"));


        /* test */
        final Dataset<Row> response = subsetService.getData(DATABASE_1_PRIVILEGED_DTO, QUERY_1_STATEMENT);
        assertNotNull(response);
        final List<List<String>> mapped = response.collectAsList()
                .stream()
                .map(row -> {
                    final List<String> map = new LinkedList<>();
                    for (int i = 0; i < response.columns().length; i++) {
                        map.add(row.get(i) != null ? String.valueOf(row.get(i)) : "");
                    }
                    return map;
                })
                .toList();
        assertEquals(expected, mapped);
    }

    @Test
    public void getData_notFound_fails() {

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            subsetService.getData(DATABASE_1_PRIVILEGED_DTO, "SELECT 1 FROM i_do_not_exist");
        });
    }

    @Test
    public void reExecuteCount_succeeds() throws SQLException, QueryMalformedException {

        /* test */
        final Long response = subsetService.reExecuteCount(DATABASE_1_PRIVILEGED_DTO, QUERY_1_DTO);
        assertNotNull(response);
    }

    @Test
    public void reExecuteCount_malformed_fails() {
        final QueryDto request = QueryDto.builder()
                .execution(Instant.now())
                .databaseId(DATABASE_1_ID)
                .query("SELECT") // <<<
                .build();

        /* test */
        assertThrows(QueryMalformedException.class, () -> {
            subsetService.reExecuteCount(DATABASE_1_PRIVILEGED_DTO, request);
        });
    }

    @Test
    public void executeCountNonPersistent_succeeds() throws SQLException, QueryMalformedException {

        /* test */
        final Long response = subsetService.executeCountNonPersistent(DATABASE_1_PRIVILEGED_DTO, QUERY_1_STATEMENT, QUERY_1_CREATED);
        assertNotNull(response);
    }

    @Test
    public void executeCountNonPersistent_malformed_fails() {

        /* test */
        assertThrows(QueryMalformedException.class, () -> {
            subsetService.executeCountNonPersistent(DATABASE_1_PRIVILEGED_DTO, "SELECT", QUERY_1_CREATED);
        });
    }

    @Test
    public void executeCountNonPersistent_illegalQuery_fails() {

        /* test */
        assertThrows(QueryMalformedException.class, () -> {
            subsetService.executeCountNonPersistent(DATABASE_1_PRIVILEGED_DTO, "DROP DATABASE `weather`", QUERY_1_CREATED);
        });
    }

    @Test
    public void storeQuery_succeeds() throws SQLException, QueryStoreInsertException, ViewMalformedException {

        /* test */
        final UUID response = subsetService.storeQuery(DATABASE_1_PRIVILEGED_DTO, QUERY_1_STATEMENT, QUERY_1_CREATED, USER_1_ID);
        assertNotNull(response);
    }

    @Test
    public void create_succeeds() throws SQLException, QueryStoreInsertException, ViewMalformedException,
            TableNotFoundException, QueryMalformedException, ImageNotFoundException, ViewNotFoundException {

        /* test */
        final UUID response = subsetService.create(DATABASE_1_PRIVILEGED_DTO, QUERY_1_SUBSET_DTO, QUERY_1_CREATED, USER_1_ID);
        assertNotNull(response);
    }

    @ParameterizedTest
    @MethodSource("create_arguments")
    public void create_illegalQuery_succeeds(String name, String injection) throws TableNotFoundException,
            QueryStoreInsertException, ViewMalformedException, SQLException, QueryMalformedException,
            ImageNotFoundException, ViewNotFoundException {
        final SubsetDto request = SubsetDto.builder()
                .datasourceId(TABLE_1_ID)
                .datasourceType(DatasourceType.TABLE)
                .columns(new LinkedList<>(List.of(COLUMN_1_1_ID, COLUMN_1_2_ID, COLUMN_1_3_ID, COLUMN_1_4_ID, COLUMN_1_5_ID)))
                .filter(new LinkedList<>(List.of(FilterDto.builder()
                        .type(FilterTypeDto.WHERE)
                        .columnId(COLUMN_1_1_ID)
                        .operatorId(IMAGE_1_OPERATORS_2_ID)
                        .value(injection)
                        .build())))
                .order(new LinkedList<>(List.of(OrderDto.builder()
                        .columnId(COLUMN_1_1_ID)
                        .direction(OrderTypeDto.ASC)
                        .build())))
                .build();

        /* test */
        subsetService.create(DATABASE_1_PRIVILEGED_DTO, request, QUERY_1_CREATED, USER_1_ID);
        assertEquals(1, MariaDbConfig.selectQuery(DATABASE_1_PRIVILEGED_DTO, "SELECT 1 WHERE EXISTS (SELECT * FROM `weather_location`)", Set.of()).size());
    }

    @Test
    public void storeQuery_fails() {

        /* test */
        assertThrows(QueryStoreInsertException.class, () -> {
            subsetService.storeQuery(DATABASE_1_PRIVILEGED_DTO, "DROP DATABASE `weather`", QUERY_1_CREATED, USER_1_ID);
        });
    }

    protected void findById_generic(UUID queryId) throws RemoteUnavailableException, SQLException,
            UserNotFoundException, QueryNotFoundException, MetadataServiceException, DatabaseNotFoundException,
            InterruptedException {

        /* pre-condition */
        Thread.sleep(1000) /* wait for test container some more */;

        /* mock */
        when(metadataServiceGateway.getIdentifiers(DATABASE_1_ID, queryId))
                .thenReturn(List.of(IDENTIFIER_2_BRIEF_DTO));
        when(metadataServiceGateway.getUserById(USER_1_ID))
                .thenReturn(USER_1_DTO);

        /* test */
        final QueryDto response = subsetService.findById(DATABASE_1_PRIVILEGED_DTO, queryId);
        assertEquals(queryId, response.getId());
    }

    protected List<QueryDto> findAll_generic(Boolean filterPersisted) throws SQLException, QueryNotFoundException,
            RemoteUnavailableException, MetadataServiceException, DatabaseNotFoundException,
            InterruptedException, UserNotFoundException {

        /* pre-condition */
        Thread.sleep(1000) /* wait for test container some more */;

        /* mock */
        MariaDbConfig.insertQueryStore(DATABASE_1_PRIVILEGED_DTO, QUERY_1_DTO, USER_1_ID);
        MariaDbConfig.insertQueryStore(DATABASE_1_PRIVILEGED_DTO, QUERY_2_DTO, USER_1_ID);
        when(metadataServiceGateway.getIdentifiers(DATABASE_1_ID, null))
                .thenReturn(List.of(IDENTIFIER_2_BRIEF_DTO, IDENTIFIER_5_BRIEF_DTO));

        /* test */
        return subsetService.findAll(DATABASE_1_PRIVILEGED_DTO, filterPersisted);
    }

    protected void persist_generic(UUID queryId, List<IdentifierBriefDto> identifiers, Boolean persist)
            throws RemoteUnavailableException, SQLException, QueryStorePersistException, MetadataServiceException,
            DatabaseNotFoundException, InterruptedException {

        /* pre-condition */
        Thread.sleep(1000) /* wait for test container some more */;

        /* mock */
        when(metadataServiceGateway.getIdentifiers(DATABASE_1_ID, queryId))
                .thenReturn(identifiers);

        /* test */
        subsetService.persist(DATABASE_1_PRIVILEGED_DTO, queryId, persist);
    }

}
