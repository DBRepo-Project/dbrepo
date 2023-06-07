package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.ViewCreateDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.config.IndexConfig;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.database.View;
import at.tuwien.exception.*;
import at.tuwien.gateway.BrokerServiceGateway;
import at.tuwien.listener.impl.RabbitMqListenerImpl;
import at.tuwien.repository.sdb.ViewIdxRepository;
import at.tuwien.repository.mdb.*;
import com.rabbitmq.client.Channel;
import at.tuwien.config.DockerConfig;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@EnableAutoConfiguration(exclude= RabbitAutoConfiguration.class)
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class ViewServiceIntegrationTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private Channel channel;

    @MockBean
    private IndexConfig indexInitializer;

    @MockBean
    private ViewIdxRepository viewIdxRepository;

    /* keep */
    @MockBean
    private RabbitMqListenerImpl rabbitMqListener;

    /* keep */
    @MockBean
    private BrokerServiceGateway brokerServiceGateway;

    @MockBean
    private DatabaseRepository databaseRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private ViewRepository viewRepository;

    @Autowired
    private ViewService viewService;

    final static String BIND_WEATHER = new File("../../dbrepo-metadata-db/test/src/test/resources/weather").toPath().toAbsolutePath() + ":/docker-entrypoint-initdb.d";

    @BeforeAll
    public static void beforeAll() throws InterruptedException {
        afterAll();
        /* create network */
        DockerConfig.createAllNetworks();
        /* create container */
        DockerConfig.createContainer(BIND_WEATHER, CONTAINER_1, CONTAINER_1_ENV);
        DockerConfig.startContainer(CONTAINER_1);
    }

    @AfterAll
    public static void afterAll() {
        DockerConfig.removeAllContainers();
        DockerConfig.removeAllNetworks();
    }

    @BeforeEach
    public void beforeEach() {
        DATABASE_1.setTables(List.of(TABLE_1, TABLE_2, TABLE_3, TABLE_7));
        DATABASE_1.setViews(List.of(VIEW_2, VIEW_3));
    }

    @Test
    public void create_viewJoinOnView_succeeds() throws DatabaseNotFoundException, UserNotFoundException,
            DatabaseConnectionException, ViewMalformedException, QueryMalformedException, SQLException {
        final ViewCreateDto request = ViewCreateDto.builder()
                .name("Debug")
                .query(VIEW_3_QUERY)
                .isPublic(true)
                .build();

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));
        when(viewRepository.save(any(View.class)))
                .thenReturn(VIEW_3);
        when(viewIdxRepository.save(any(ViewDto.class)))
                .thenReturn(VIEW_3_DTO);

        /* test */
        final View response = viewService.create(CONTAINER_1_ID, DATABASE_1_ID, request, USER_1_PRINCIPAL);
        assertEquals(VIEW_3_ID, response.getId());
        assertEquals(VIEW_3_NAME, response.getName());
        assertEquals(VIEW_3_INTERNAL_NAME, response.getInternalName());
        assertEquals(VIEW_3_QUERY, response.getQuery());
        final List<Map<String, String>> resultSet = MariaDbConfig.selectQuery(CONTAINER_1_INTERNALNAME, DATABASE_1_INTERNALNAME,
                "SELECT j.* FROM `debug` j", "mintemp", "rainfall", "date", "location");
        assertEquals("13.4", resultSet.get(0).get("mintemp"));
        assertEquals("0.6", resultSet.get(0).get("rainfall"));
        assertEquals("Albury", resultSet.get(0).get("location"));
        assertEquals("2008-12-01", resultSet.get(0).get("date"));
        assertEquals("7.4", resultSet.get(1).get("mintemp"));
        assertEquals("0", resultSet.get(1).get("rainfall"));
        assertEquals("Albury", resultSet.get(1).get("location"));
        assertEquals("2008-12-01", resultSet.get(1).get("date"));
        assertEquals("12.9", resultSet.get(2).get("mintemp"));
        assertEquals("0", resultSet.get(2).get("rainfall"));
        assertEquals("Albury", resultSet.get(2).get("location"));
        assertEquals("2008-12-01", resultSet.get(2).get("date"));
        /* more result checks omitted */
    }

    @Test
    public void create_succeeds() throws DatabaseNotFoundException, UserNotFoundException, DatabaseConnectionException,
            ViewMalformedException, QueryMalformedException, SQLException {
        final ViewCreateDto request = ViewCreateDto.builder()
                .name(VIEW_1_NAME)
                .query(VIEW_1_QUERY)
                .isPublic(VIEW_1_PUBLIC)
                .build();

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));
        when(viewRepository.save(any(View.class)))
                .thenReturn(VIEW_1);
        when(viewIdxRepository.save(any(ViewDto.class)))
                .thenReturn(VIEW_1_DTO);

        /* test */
        final View response = viewService.create(CONTAINER_1_ID, DATABASE_1_ID, request, USER_1_PRINCIPAL);
        assertEquals(VIEW_1_ID, response.getId());
        assertEquals(VIEW_1_NAME, response.getName());
        assertEquals(VIEW_1_INTERNAL_NAME, response.getInternalName());
        assertEquals(VIEW_1_QUERY, response.getQuery());
        final List<Map<String, String>> resultSet = MariaDbConfig.selectQuery(CONTAINER_1_INTERNALNAME, DATABASE_1_INTERNALNAME,
                "SELECT l.`location`, l.`lat`, l.`lng` FROM `weather_location` l ORDER BY l.`location` ASC", "location", "lat", "lng");
        assertEquals(3, resultSet.size());
        final Map<String, String> row0 = resultSet.get(0);
        assertEquals("Albury", row0.get("location"));
        assertEquals("-36.0653583", row0.get("lat"));
        assertEquals("146.9112214", row0.get("lng"));
        final Map<String, String> row1 = resultSet.get(1);
        assertEquals("Sydney", row1.get("location"));
        assertEquals("-33.847927", row1.get("lat"));
        assertEquals("150.6517942", row1.get("lng"));
        final Map<String, String> row2 = resultSet.get(2);
        assertEquals("Vienna", row2.get("location"));
        assertNull(row2.get("lat"));
        assertNull(row2.get("lng"));
    }

}
