package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockListeners;
import at.tuwien.api.database.ViewCreateDto;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.MariaDbContainerConfig;
import at.tuwien.entities.database.View;
import at.tuwien.entities.database.ViewColumn;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.exception.*;
import at.tuwien.repository.mdb.*;
import lombok.extern.log4j.Log4j2;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.rules.Timeout;
import org.opensearch.testcontainers.OpensearchContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Log4j2
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@EnableAutoConfiguration(exclude = RabbitAutoConfiguration.class)
@SpringBootTest
@ExtendWith(SpringExtension.class)
@MockAmqp
@MockListeners
public class ViewServiceIntegrationTest extends BaseUnitTest {

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ViewService viewService;

    @Rule
    public Timeout globalTimeout = Timeout.seconds(60);

    @Container
    private static MariaDBContainer<?> mariaDBContainer = MariaDbContainerConfig.getContainer();

    @Container
    private static final OpensearchContainer opensearchContainer = new OpensearchContainer(DockerImageName.parse("opensearchproject/opensearch:2.10.0"));

    @DynamicPropertySource
    static void openSearchProperties(DynamicPropertyRegistry registry) {
        final int idx = opensearchContainer.getHttpHostAddress().lastIndexOf(':');
        registry.add("spring.opensearch.host", () -> "127.0.0.1");
        registry.add("spring.opensearch.port", () -> opensearchContainer.getHttpHostAddress().substring(idx + 1));
        registry.add("spring.opensearch.username", opensearchContainer::getUsername);
        registry.add("spring.opensearch.password", opensearchContainer::getPassword);
    }

    @BeforeAll
    public static void beforeAll() throws SQLException {
        MariaDbConfig.dropAllDatabases(CONTAINER_1);
        MariaDbConfig.createInitDatabase(CONTAINER_1, DATABASE_1);
    }

    @BeforeEach
    public void beforeEach() throws DatabaseUnchangedException, QueryMalformedException, ColumnParseException,
            DatabaseNotFoundException, TableMalformedException {
        genesis();
        /* metadata database */
        imageRepository.save(IMAGE_1);
        licenseRepository.save(LICENSE_1);
        userRepository.saveAll(List.of(USER_1, USER_2, USER_3));
        containerRepository.saveAll(List.of(CONTAINER_1, CONTAINER_2));
        databaseRepository.saveAll(List.of(DATABASE_1, DATABASE_2));
    }

    @Test
    public void create_viewJoinOnView_succeeds() throws DatabaseNotFoundException, UserNotFoundException,
            DatabaseConnectionException, ViewMalformedException, QueryMalformedException, SQLException {
        final ViewCreateDto request = ViewCreateDto.builder()
                .name("Debug")
                .query(VIEW_3_QUERY)
                .isPublic(true)
                .build();

        /* test */
        final View response = viewService.create(DATABASE_1_ID, request, USER_1_PRINCIPAL);
        assertEquals("Debug", response.getName());
        assertEquals("debug", response.getInternalName());
        assertEquals(VIEW_3_QUERY, response.getQuery());
        final List<Map<String, String>> resultSet = MariaDbConfig.selectQuery(DATABASE_1,
                "SELECT j.* FROM `debug` j", "mintemp", "rainfall", "date", "location");
        assertEquals("13.4", resultSet.get(0).get("mintemp"));
        assertEquals("0.6", resultSet.get(0).get("rainfall"));
        assertEquals("Albury", resultSet.get(0).get("location"));
        assertEquals("2008-12-01", resultSet.get(0).get("date"));
        assertEquals("7.4", resultSet.get(1).get("mintemp"));
        assertEquals("0", resultSet.get(1).get("rainfall"));
        assertEquals("Albury", resultSet.get(1).get("location"));
        assertEquals("2008-12-02", resultSet.get(1).get("date"));
        assertEquals("12.9", resultSet.get(2).get("mintemp"));
        assertEquals("0", resultSet.get(2).get("rainfall"));
        assertEquals("Albury", resultSet.get(2).get("location"));
        assertEquals("2008-12-03", resultSet.get(2).get("date"));
    }

    @Test
    public void create_succeeds() throws DatabaseNotFoundException, UserNotFoundException, DatabaseConnectionException,
            ViewMalformedException, QueryMalformedException, SQLException {
        final ViewCreateDto request = ViewCreateDto.builder()
                .name(VIEW_1_NAME)
                .query(VIEW_1_QUERY)
                .isPublic(VIEW_1_PUBLIC)
                .build();

        /* test */
        final View response = viewService.create(DATABASE_1_ID, request, USER_1_PRINCIPAL);
        assertEquals(VIEW_1_NAME, response.getName());
        assertEquals(VIEW_1_INTERNAL_NAME, response.getInternalName());
        assertEquals(VIEW_1_QUERY, response.getQuery());
        final List<Map<String, String>> resultSet = MariaDbConfig.selectQuery(DATABASE_1,
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
