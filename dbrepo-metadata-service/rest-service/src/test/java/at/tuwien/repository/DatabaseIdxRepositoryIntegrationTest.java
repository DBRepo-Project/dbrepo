package at.tuwien.repository;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.api.database.DatabaseDto;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.MariaDbContainerConfig;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.View;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.user.User;
import at.tuwien.mapper.DatabaseMapper;
import at.tuwien.repository.mdb.*;
import at.tuwien.repository.sdb.DatabaseIdxRepository;
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
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static java.time.temporal.ChronoUnit.HOURS;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Log4j2
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
@MockAmqp
public class DatabaseIdxRepositoryIntegrationTest extends BaseUnitTest {

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private DatabaseMapper databaseMapper;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private DatabaseIdxRepository databaseIdxRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LicenseRepository licenseRepository;

    @Rule
    public Timeout globalTimeout = Timeout.seconds(60);

    @Container
    private static MariaDBContainer<?> mariaDBContainer = MariaDbContainerConfig.getContainer();

    /**
     * @apiNote Must be the same image tag as version in pom.xml properties -> opensearch-rest-client.version
     */
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

    @BeforeEach
    public void beforeEach() throws SQLException {
        TABLE_1.setColumns(TABLE_1_COLUMNS);
        TABLE_2.setColumns(TABLE_2_COLUMNS);
        TABLE_3.setColumns(TABLE_3_COLUMNS);
        TABLE_4.setColumns(TABLE_4_COLUMNS);
        DATABASE_1.setAccesses(List.of(DATABASE_1_USER_1_READ_ACCESS));
        /* prevent multiple representations of the same entity */
        TABLE_1.setDatabase(null);
        TABLE_2.setDatabase(null);
        TABLE_3.setDatabase(null);
        TABLE_4.setDatabase(null);
        IDENTIFIER_1.setDatabase(null);
        IDENTIFIER_2.setDatabase(null);
        IDENTIFIER_3.setDatabase(null);
        IDENTIFIER_4.setDatabase(null);
        /* metadata database */
        imageRepository.save(IMAGE_1);
        licenseRepository.save(LICENSE_1);
        userRepository.saveAll(List.of(USER_1, USER_2, USER_3));
        containerRepository.save(CONTAINER_1);
        databaseRepository.save(DATABASE_1);
        /* data database */
        MariaDbConfig.dropAllDatabases(CONTAINER_1);
        MariaDbConfig.createInitDatabase(CONTAINER_1, DATABASE_1);
    }

    @Test
    @Transactional
    public void save_succeeds() {

        /* test */
        databaseIdxRepository.save(databaseMapper.databaseToDatabaseDto(DATABASE_1));
    }

    @Test
    @Transactional
    public void save_simpleDatabase_succeeds() {
        final Database request = Database.builder()
                .id(DATABASE_1_ID)
                .created(Instant.now().minus(1, HOURS))
                .lastModified(Instant.now())
                .isPublic(DATABASE_1_PUBLIC)
                .name(DATABASE_1_NAME)
                .description(DATABASE_1_DESCRIPTION)
                .cid(CONTAINER_1_ID)
                .container(null)
                .internalName(DATABASE_1_INTERNALNAME)
                .exchangeName(DATABASE_1_EXCHANGE)
                .created(DATABASE_1_CREATED)
                .lastModified(DATABASE_1_LAST_MODIFIED)
                .createdBy(DATABASE_1_CREATOR)
                .creator(null)
                .ownedBy(DATABASE_1_OWNER)
                .owner(null)
                .contactPerson(USER_1_ID)
                .contact(null)
                .tables(List.of())
                .views(List.of())
                .accesses(List.of())
                .build();

        /* test */
        final Database response = databaseRepository.save(request);
        databaseIdxRepository.save(databaseMapper.databaseToDatabaseDto(response));
    }

    @Test
    @Transactional
    public void save_databaseWithUsers_succeeds() {
        final Database request = Database.builder()
                .id(DATABASE_1_ID)
                .created(Instant.now().minus(1, HOURS))
                .lastModified(Instant.now())
                .isPublic(DATABASE_1_PUBLIC)
                .name(DATABASE_1_NAME)
                .description(DATABASE_1_DESCRIPTION)
                .cid(CONTAINER_1_ID)
                .container(null)
                .internalName(DATABASE_1_INTERNALNAME)
                .exchangeName(DATABASE_1_EXCHANGE)
                .created(DATABASE_1_CREATED)
                .lastModified(DATABASE_1_LAST_MODIFIED)
                .createdBy(DATABASE_1_CREATOR)
                .creator(USER_1)
                .ownedBy(DATABASE_1_OWNER)
                .owner(USER_1)
                .contactPerson(USER_1_ID)
                .contact(USER_1)
                .tables(List.of())
                .views(List.of())
                .accesses(List.of())
                .build();

        /* test */
        final Database response = databaseRepository.save(request);
        databaseIdxRepository.save(databaseMapper.databaseToDatabaseDto(response));
    }

    @Test
    @Transactional
    public void save_databaseWithIdentifier_succeeds() {
        final Database request = Database.builder()
                .id(DATABASE_1_ID)
                .created(Instant.now().minus(1, HOURS))
                .lastModified(Instant.now())
                .isPublic(DATABASE_1_PUBLIC)
                .name(DATABASE_1_NAME)
                .description(DATABASE_1_DESCRIPTION)
                .cid(CONTAINER_1_ID)
                .container(null)
                .internalName(DATABASE_1_INTERNALNAME)
                .exchangeName(DATABASE_1_EXCHANGE)
                .created(DATABASE_1_CREATED)
                .lastModified(DATABASE_1_LAST_MODIFIED)
                .createdBy(DATABASE_1_CREATOR)
                .identifiers(List.of(IDENTIFIER_1))
                .creator(USER_1)
                .ownedBy(DATABASE_1_OWNER)
                .owner(USER_1)
                .contactPerson(USER_1_ID)
                .contact(USER_1)
                .tables(List.of())
                .views(List.of())
                .accesses(List.of())
                .build();

        /* test */
        final Database response = databaseRepository.save(request);
        databaseIdxRepository.save(databaseMapper.databaseToDatabaseDto(response));
    }

    @Test
    @Transactional
    public void save_databaseWithContainerAndUsers_succeeds() {
        final Database request = Database.builder()
                .id(DATABASE_1_ID)
                .created(Instant.now().minus(1, HOURS))
                .lastModified(Instant.now())
                .isPublic(DATABASE_1_PUBLIC)
                .name(DATABASE_1_NAME)
                .description(DATABASE_1_DESCRIPTION)
                .cid(CONTAINER_1_ID)
                .container(CONTAINER_1)
                .internalName(DATABASE_1_INTERNALNAME)
                .exchangeName(DATABASE_1_EXCHANGE)
                .created(DATABASE_1_CREATED)
                .lastModified(DATABASE_1_LAST_MODIFIED)
                .createdBy(DATABASE_1_CREATOR)
                .creator(USER_1)
                .ownedBy(DATABASE_1_OWNER)
                .owner(USER_1)
                .contactPerson(USER_1_ID)
                .contact(USER_1)
                .tables(List.of())
                .views(List.of())
                .accesses(List.of())
                .build();

        /* test */
        final Database response = databaseRepository.save(request);
        databaseIdxRepository.save(databaseMapper.databaseToDatabaseDto(response));
    }

    @Test
    @Transactional
    public void save_databaseWithSimpleTable_succeeds() {
        final Database request = Database.builder()
                .id(DATABASE_1_ID)
                .created(Instant.now().minus(1, HOURS))
                .lastModified(Instant.now())
                .isPublic(DATABASE_1_PUBLIC)
                .name(DATABASE_1_NAME)
                .description(DATABASE_1_DESCRIPTION)
                .cid(CONTAINER_1_ID)
                .container(null)
                .internalName(DATABASE_1_INTERNALNAME)
                .exchangeName(DATABASE_1_EXCHANGE)
                .created(DATABASE_1_CREATED)
                .lastModified(DATABASE_1_LAST_MODIFIED)
                .createdBy(DATABASE_1_CREATOR)
                .creator(null)
                .ownedBy(DATABASE_1_OWNER)
                .owner(null)
                .contactPerson(USER_1_ID)
                .contact(null)
                .tables(List.of(Table.builder()
                        .id(TABLE_1_ID)
                        .tdbid(DATABASE_1_ID)
                        .database(null)
                        .created(TABLE_1_CREATED)
                        .internalName(TABLE_1_INTERNALNAME)
                        .isVersioned(TABLE_1_VERSIONED)
                        .description(TABLE_1_DESCRIPTION)
                        .name(TABLE_1_NAME)
                        .queueName(TABLE_1_QUEUE_NAME)
                        .routingKey(TABLE_1_ROUTING_KEY)
                        .columns(List.of())
                        .constraints(null)
                        .createdBy(USER_1_ID)
                        .creator(null)
                        .ownedBy(USER_1_ID)
                        .owner(null)
                        .lastModified(TABLE_1_LAST_MODIFIED)
                        .build()))
                .views(List.of())
                .accesses(List.of())
                .build();

        /* test */
        final Database response = databaseRepository.save(request);
        databaseIdxRepository.save(databaseMapper.databaseToDatabaseDto(response));
    }

    @Test
    @Transactional
    public void save_databaseWithSimpleTableWithUser_succeeds() {
        final Database request = Database.builder()
                .id(DATABASE_1_ID)
                .created(Instant.now().minus(1, HOURS))
                .lastModified(Instant.now())
                .isPublic(DATABASE_1_PUBLIC)
                .name(DATABASE_1_NAME)
                .description(DATABASE_1_DESCRIPTION)
                .cid(CONTAINER_1_ID)
                .container(null)
                .internalName(DATABASE_1_INTERNALNAME)
                .exchangeName(DATABASE_1_EXCHANGE)
                .created(DATABASE_1_CREATED)
                .lastModified(DATABASE_1_LAST_MODIFIED)
                .createdBy(DATABASE_1_CREATOR)
                .creator(USER_1)
                .ownedBy(USER_2_ID)
                .owner(USER_2)
                .contactPerson(USER_2_ID)
                .contact(USER_2)
                .tables(List.of(Table.builder()
                        .id(TABLE_1_ID)
                        .tdbid(DATABASE_1_ID)
                        .database(null)
                        .created(TABLE_1_CREATED)
                        .internalName(TABLE_1_INTERNALNAME)
                        .isVersioned(TABLE_1_VERSIONED)
                        .description(TABLE_1_DESCRIPTION)
                        .name(TABLE_1_NAME)
                        .queueName(TABLE_1_QUEUE_NAME)
                        .routingKey(TABLE_1_ROUTING_KEY)
                        .columns(List.of())
                        .constraints(null)
                        .createdBy(USER_1_ID)
                        .creator(USER_1)
                        .ownedBy(USER_2_ID)
                        .owner(USER_2)
                        .lastModified(TABLE_1_LAST_MODIFIED)
                        .build()))
                .views(List.of())
                .accesses(List.of())
                .build();

        /* test */
        final Database response = databaseRepository.save(request);
        databaseIdxRepository.save(databaseMapper.databaseToDatabaseDto(response));
    }

    @Test
    @Transactional
    public void save_databaseWithContainerAndUsersAndTable_succeeds() {
        final Database request = Database.builder()
                .id(DATABASE_1_ID)
                .created(Instant.now().minus(1, HOURS))
                .lastModified(Instant.now())
                .isPublic(DATABASE_1_PUBLIC)
                .name(DATABASE_1_NAME)
                .description(DATABASE_1_DESCRIPTION)
                .cid(CONTAINER_1_ID)
                .container(CONTAINER_1)
                .internalName(DATABASE_1_INTERNALNAME)
                .exchangeName(DATABASE_1_EXCHANGE)
                .created(DATABASE_1_CREATED)
                .lastModified(DATABASE_1_LAST_MODIFIED)
                .createdBy(DATABASE_1_CREATOR)
                .creator(USER_1)
                .ownedBy(DATABASE_1_OWNER)
                .owner(USER_1)
                .contactPerson(USER_1_ID)
                .contact(USER_1)
                .tables(List.of(_mapTable(TABLE_1_ID)))
                .views(List.of())
                .accesses(List.of())
                .build();

        /* test */
        final Database response = databaseRepository.save(request);
        databaseIdxRepository.save(databaseMapper.databaseToDatabaseDto(response));
    }

    @Test
    @Transactional
    public void save_databaseWithContainerAndUsersAndView_succeeds() {
        final Database request = Database.builder()
                .id(DATABASE_1_ID)
                .created(Instant.now().minus(1, HOURS))
                .lastModified(Instant.now())
                .isPublic(DATABASE_1_PUBLIC)
                .name(DATABASE_1_NAME)
                .description(DATABASE_1_DESCRIPTION)
                .cid(CONTAINER_1_ID)
                .container(CONTAINER_1)
                .internalName(DATABASE_1_INTERNALNAME)
                .exchangeName(DATABASE_1_EXCHANGE)
                .created(DATABASE_1_CREATED)
                .lastModified(DATABASE_1_LAST_MODIFIED)
                .createdBy(DATABASE_1_CREATOR)
                .creator(USER_1)
                .ownedBy(DATABASE_1_OWNER)
                .owner(USER_1)
                .contactPerson(USER_1_ID)
                .contact(USER_1)
                .tables(List.of())
                .views(List.of(_mapView(VIEW_1_ID)))
                .accesses(List.of())
                .build();

        /* test */
        final Database response = databaseRepository.save(request);
        databaseIdxRepository.save(databaseMapper.databaseToDatabaseDto(response));
    }

    public Table _mapTable(Long id) {
        final Optional<Table> optional = DATABASE_1.getTables().stream().filter(t -> t.getId().equals(id)).findFirst();
        assertTrue(optional.isPresent());
        return optional.get();
    }

    public View _mapView(Long id) {
        final Optional<View> optional = DATABASE_1.getViews().stream().filter(t -> t.getId().equals(id)).findFirst();
        assertTrue(optional.isPresent());
        return optional.get();
    }

}
