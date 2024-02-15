package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockListeners;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.database.table.columns.concepts.ColumnSemanticsUpdateDto;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.MariaDbContainerConfig;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.entities.database.table.columns.TableColumnConcept;
import at.tuwien.exception.*;
import at.tuwien.repository.mdb.*;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@EnableAutoConfiguration(exclude = RabbitAutoConfiguration.class)
@SpringBootTest
@ExtendWith(SpringExtension.class)
@MockAmqp
@MockListeners
@MockOpensearch
public class TableColumnServiceIntegrationTest extends BaseUnitTest {

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TableColumnService tableColumnService;

    @Container
    private static MariaDBContainer<?> mariaDBContainer = MariaDbContainerConfig.getContainer();

    @BeforeEach
    public void beforeEach() throws SQLException {
        TABLE_1.setColumns(TABLE_1_COLUMNS);
        TABLE_1_FOREIGN_KEY_1.setReferences(List.of(TABLE_1_FOREIGN_KEY_REFERENCE));
        TABLE_1.setConstraints(TABLE_1_CONSTRAINTS);
        TABLE_2.setColumns(TABLE_2_COLUMNS);
        TABLE_2.setConstraints(TABLE_2_CONSTRAINTS);
        TABLE_3.setColumns(TABLE_3_COLUMNS);
        TABLE_3.setConstraints(TABLE_3_CONSTRAINTS);
        TABLE_4.setColumns(TABLE_4_COLUMNS);
        DATABASE_1.setAccesses(List.of());
        TABLE_1.setDatabase(DATABASE_1);
        TABLE_2.setDatabase(DATABASE_1);
        TABLE_3.setDatabase(DATABASE_1);
        TABLE_4.setDatabase(DATABASE_1);
        /* metadata database */
        imageRepository.save(IMAGE_1);
        licenseRepository.save(LICENSE_1);
        userRepository.saveAll(List.of(USER_1, USER_2));
        containerRepository.save(CONTAINER_1);
        databaseRepository.save(DATABASE_1);
        /* data stuff */
        MariaDbConfig.dropAllDatabases(CONTAINER_1);
        MariaDbConfig.createInitDatabase(CONTAINER_1, DATABASE_1);
    }

    @Test
    @Transactional
    public void update_succeeds() throws TableNotFoundException, TableMalformedException, DatabaseNotFoundException {
        final ColumnSemanticsUpdateDto request = ColumnSemanticsUpdateDto.builder()
                .conceptUri(COLUMN_CONCEPT_PRECIPITATION_URI)
                .build();

        /* test */
        final TableColumn response = tableColumnService.update(DATABASE_1_ID, TABLE_1_ID, TABLE_1_COLUMNS.get(0).getId(),
                request);
        assertNotNull(response.getConcept());
        final TableColumnConcept concept = response.getConcept();
        assertEquals(COLUMN_CONCEPT_PRECIPITATION_URI, concept.getUri());
    }

}
