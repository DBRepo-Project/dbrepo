package at.tuwien.endpoints;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.database.ViewBriefDto;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.MariaDbContainerConfig;
import at.tuwien.exception.*;
import at.tuwien.repository.mdb.*;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@SpringBootTest
@Testcontainers
@ExtendWith(SpringExtension.class)
@MockAmqp
@MockOpensearch
public class ViewEndpointIntegrationTest extends BaseUnitTest {

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private ViewEndpoint viewEndpoint;

    @Container
    private static MariaDBContainer<?> mariaDBContainer = MariaDbContainerConfig.getContainer();

    @BeforeEach
    public void beforeEach() throws SQLException {
        TABLE_1.setColumns(TABLE_1_COLUMNS);
        TABLE_2.setColumns(TABLE_2_COLUMNS);
        TABLE_3.setColumns(TABLE_3_COLUMNS);
        TABLE_4.setColumns(TABLE_4_COLUMNS);
        /* metadata database */
        imageRepository.save(IMAGE_1);
        licenseRepository.save(LICENSE_1);
        userRepository.saveAll(List.of(USER_1, USER_2, USER_3));
        containerRepository.save(CONTAINER_1);
        databaseRepository.save(DATABASE_1);
        MariaDbConfig.dropAllDatabases(CONTAINER_1);
        MariaDbConfig.createInitDatabase(CONTAINER_1, DATABASE_1);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-database-view"})
    public void create_privateDatabasePublicView_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseConnectionException, ViewMalformedException, QueryMalformedException, DatabaseNotFoundException {

        /* test */
        final ResponseEntity<ViewBriefDto> response = viewEndpoint.create(DATABASE_1_ID, VIEW_1_CREATE_DTO, USER_1_PRINCIPAL);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        final ViewBriefDto body = response.getBody();
        assertNotNull(body);
        assertEquals(VIEW_1_NAME, body.getName());
        assertEquals(VIEW_1_INTERNAL_NAME, body.getInternalName());
        assertEquals(VIEW_1_QUERY, body.getQuery());
        assertEquals(VIEW_1_PUBLIC, body.getIsPublic());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-database-view"})
    public void count_privateDatabasePublicView_succeeds() throws UserNotFoundException, DatabaseConnectionException,
            QueryMalformedException, DatabaseNotFoundException, QueryStoreException, TableMalformedException,
            ImageNotSupportedException, ContainerNotFoundException, ViewNotFoundException, SQLException {
        final String request = "CREATE VIEW `" + VIEW_1_INTERNAL_NAME + "` AS (" + VIEW_1_QUERY + ");";

        /* mock */
        MariaDbConfig.execute(DATABASE_1, request);

        /* test */
        final ResponseEntity<Long> response = viewEndpoint.count(DATABASE_1_ID, VIEW_1_ID, USER_1_PRINCIPAL);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(3L, response.getBody());
    }

}
