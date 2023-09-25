package at.tuwien.repository;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.api.database.ViewCreateDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.MariaDbContainerConfig;
import at.tuwien.exception.*;
import at.tuwien.repository.mdb.*;
import at.tuwien.repository.sdb.ViewIdxRepository;
import at.tuwien.service.ViewService;
import lombok.extern.log4j.Log4j2;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.rules.Timeout;
import org.opensearch.testcontainers.OpensearchContainer;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Log4j2
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
@MockAmqp
public class ViewIdxRepositoryIntegrationTest extends BaseUnitTest {

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private TableRepository tableRepository;

    @Autowired
    private TableColumnRepository tableColumnRepository;

    @Autowired
    private ViewIdxRepository viewIdxRepository;

    @Autowired
    private ViewService viewService;

    @Autowired
    private UserRepository userRepository;

    @Rule
    public Timeout globalTimeout = Timeout.seconds(60);

    @Container
    private static MariaDBContainer<?> mariaDBContainer = MariaDbContainerConfig.getContainer();

    @Container
    private static final OpensearchContainer opensearchContainer = new OpensearchContainer(DockerImageName.parse("opensearchproject/opensearch:2.8.0"));

    @DynamicPropertySource
    static void openSearchProperties(DynamicPropertyRegistry registry) {
        final int idx = opensearchContainer.getHttpHostAddress().lastIndexOf(':');
        registry.add("spring.opensearch.host", () -> "127.0.0.1");
        registry.add("spring.opensearch.port", () -> opensearchContainer.getHttpHostAddress().substring(idx + 1));
        registry.add("spring.opensearch.username", opensearchContainer::getUsername);
        registry.add("spring.opensearch.password", opensearchContainer::getPassword);
    }

    @BeforeEach
    public void beforeEach() throws SQLException {
        MariaDbConfig.dropAllDatabases(CONTAINER_1);
        MariaDbConfig.createInitDatabase(CONTAINER_1, DATABASE_1);
        TABLE_1.setColumns(TABLE_1_COLUMNS);
        TABLE_2.setColumns(TABLE_2_COLUMNS);
        /* metadata database */
        imageRepository.save(IMAGE_1);
        userRepository.saveAll(List.of(USER_1, USER_2));
        containerRepository.save(CONTAINER_1);
        databaseRepository.save(DATABASE_1_SIMPLE);
        tableRepository.saveAll(List.of(TABLE_1_SIMPLE, TABLE_2_SIMPLE));
        tableColumnRepository.saveAll(TABLE_1_COLUMNS);
        tableColumnRepository.saveAll(TABLE_2_COLUMNS);
    }

    @Test
    public void save_succeeds() throws UserNotFoundException, DatabaseConnectionException, ViewMalformedException,
            QueryMalformedException, DatabaseNotFoundException {
        final ViewCreateDto request = ViewCreateDto.builder()
                .name(VIEW_1_NAME)
                .query(VIEW_1_QUERY)
                .isPublic(VIEW_1_PUBLIC)
                .build();

        /* test */
        viewService.create(DATABASE_1_ID, request, USER_1_PRINCIPAL);
        final Optional<ViewDto> response = viewIdxRepository.findById(VIEW_1_ID);
        assertTrue(response.isPresent());
        final ViewDto view = response.get();
        assertEquals(VIEW_1_ID, view.getId());
        assertEquals(VIEW_1_NAME, view.getName());
        assertEquals(VIEW_1_INTERNAL_NAME, view.getInternalName());
        assertEquals(VIEW_1_QUERY, view.getQuery());
        assertEquals(VIEW_1_DATABASE_ID, view.getVdbid());
        assertEquals(VIEW_1_PUBLIC, view.getIsPublic());
    }

}
