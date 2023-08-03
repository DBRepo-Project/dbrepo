package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.repository.mdb.*;
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;


@Log4j2
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class IdentifierServiceIntegrationTest extends BaseUnitTest {

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
    private IdentifierRepository identifierRepository;

    @Autowired
    private TableRepository tableRepository;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private IdentifierService identifierService;

    @Rule
    public Timeout globalTimeout = Timeout.seconds(60);

    @Container
    private static final OpensearchContainer opensearchContainer = new OpensearchContainer(DockerImageName.parse("opensearchproject/opensearch:2.8.0"));

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
        userRepository.save(USER_1);
        licenseRepository.save(LICENSE_1);
        imageRepository.save(IMAGE_1);
        containerRepository.save(CONTAINER_1);
        databaseRepository.save(DATABASE_1);
        identifierRepository.save(IDENTIFIER_1);
    }

    @Test
    public void findAll_succeeds() {

        /* test */
        final List<Identifier> identifiers = identifierService.findAll();
        assertEquals(1, identifiers.size());
        final Identifier identifier0 = identifiers.get(0);
        assertEquals(IDENTIFIER_1_ID, identifier0.getId());
        assertEquals(IDENTIFIER_1_DATABASE_ID, identifier0.getDatabaseId());
        assertEquals(IDENTIFIER_1_TYPE, identifier0.getType());
        assertEquals(IDENTIFIER_1_PUBLISHER, identifier0.getPublisher());
    }

    @Test
    public void findAll_multiple_succeeds() {

        /* mock */
        userRepository.save(USER_2);
        containerRepository.save(CONTAINER_2);
        databaseRepository.save(DATABASE_2);
        identifierRepository.save(IDENTIFIER_2);

        /* test */
        final List<Identifier> identifiers = identifierService.findAll();
        assertEquals(2, identifiers.size());
        final Identifier identifier0 = identifiers.get(0);
        assertEquals(IDENTIFIER_1_ID, identifier0.getId());
        assertEquals(IDENTIFIER_1_DATABASE_ID, identifier0.getDatabaseId());
        assertEquals(IDENTIFIER_1_TYPE, identifier0.getType());
        assertEquals(IDENTIFIER_1_PUBLISHER, identifier0.getPublisher());
        final Identifier identifier1 = identifiers.get(1);
        assertEquals(IDENTIFIER_2_ID, identifier1.getId());
        assertEquals(IDENTIFIER_2_DATABASE_ID, identifier1.getDatabaseId());
        assertEquals(IDENTIFIER_2_TYPE, identifier1.getType());
        assertEquals(IDENTIFIER_2_PUBLISHER, identifier1.getPublisher());
    }
}
