package at.tuwien.app;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.DatabaseDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.api.database.table.columns.concepts.ConceptDto;
import at.tuwien.api.database.table.columns.concepts.UnitDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.api.user.UserDto;
import at.tuwien.config.IndexConfig;
import at.tuwien.entities.database.View;
import at.tuwien.repository.mdb.*;
import at.tuwien.repository.sdb.*;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;


@Log4j2
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class SearchSyncComponentTest extends BaseUnitTest {

    @Autowired
    private IndexConfig indexConfig;

    @Autowired
    private RealmRepository realmRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private TableRepository tableRepository;

    @Autowired
    private ViewRepository viewRepository;

    @Autowired
    private ConceptRepository conceptRepository;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private IdentifierRepository identifierRepository;

    @Autowired
    private ConceptIdxRepository conceptIdxRepository;

    @Autowired
    private DatabaseIdxRepository databaseIdxRepository;

    @Autowired
    private IdentifierIdxRepository identifierIdxRepository;

    @Autowired
    private TableColumnIdxRepository tableColumnIdxRepository;

    @Autowired
    private TableIdxRepository tableIdxRepository;

    @Autowired
    private UnitIdxRepository unitIdxRepository;

    @Autowired
    private UserIdxRepository userIdxRepository;

    @Autowired
    private ViewIdxRepository viewIdxRepository;

    @Rule
    public Timeout globalTimeout = Timeout.seconds(60);

    @Container
    private static final OpensearchContainer opensearchContainer = new OpensearchContainer(DockerImageName.parse("opensearchproject/opensearch:2"));

    @DynamicPropertySource
    static void elasticsearchProperties(DynamicPropertyRegistry registry) {
        final int idx = opensearchContainer.getHttpHostAddress().lastIndexOf(':');
        registry.add("spring.opensearch.host", () -> "127.0.0.1");
        registry.add("spring.opensearch.port", () -> opensearchContainer.getHttpHostAddress().substring(idx + 1));
        registry.add("spring.opensearch.username", opensearchContainer::getUsername);
        registry.add("spring.opensearch.password", opensearchContainer::getPassword);
    }

    @BeforeEach
    public void beforeEach() {
        realmRepository.save(REALM_DBREPO);
        licenseRepository.save(LICENSE_1);
        userRepository.save(USER_1);
        imageRepository.save(IMAGE_1);
        containerRepository.save(CONTAINER_1);
        databaseRepository.save(DATABASE_1);
        identifierRepository.save(IDENTIFIER_1);
        tableRepository.save(TABLE_1);
        tableRepository.save(TABLE_2);
        viewRepository.save(VIEW_1);
        conceptRepository.save(CONCEPT_1);
        unitRepository.save(UNIT_1);
        /* call a second time */
        indexConfig.initIndex();
    }

    @Test
    public void initIndex_succeeds() {

        /* test */
        final List<ConceptDto> concepts = StreamSupport.stream(conceptIdxRepository.findAll().spliterator(), false)
                .toList();
        assertEquals(1, concepts.size());
        final List<DatabaseDto> databases = StreamSupport.stream(databaseIdxRepository.findAll().spliterator(), false)
                .toList();
        assertEquals(1, databases.size());
        final List<IdentifierDto> identifiers = StreamSupport.stream(identifierIdxRepository.findAll().spliterator(), false)
                .toList();
        assertEquals(1, identifiers.size());
        final List<ColumnDto> columns = StreamSupport.stream(tableColumnIdxRepository.findAll().spliterator(), false)
                .toList();
        assertEquals(TABLE_1_COLUMNS.size() + TABLE_2_COLUMNS.size(), columns.size());
        final List<TableDto> tables = StreamSupport.stream(tableIdxRepository.findAll().spliterator(), false)
                .toList();
        assertEquals(2, tables.size());
        final List<UnitDto> units = StreamSupport.stream(unitIdxRepository.findAll().spliterator(), false)
                .toList();
        assertEquals(1, units.size());
        final List<UserDto> users = StreamSupport.stream(userIdxRepository.findAll().spliterator(), false)
                .toList();
        assertEquals(1, users.size());
        final List<ViewDto> views = StreamSupport.stream(viewIdxRepository.findAll().spliterator(), false)
                .toList();
        assertEquals(1, views.size());
    }
}
