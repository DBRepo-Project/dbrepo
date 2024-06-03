package at.tuwien.service;

import at.tuwien.entities.database.Database;
import at.tuwien.exception.*;
import at.tuwien.gateway.DataServiceGateway;
import at.tuwien.gateway.SearchServiceGateway;
import at.tuwien.repository.*;
import at.tuwien.test.AbstractUnitTest;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@Disabled("CI/CD")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
public class DatabaseServicePersistenceTest extends AbstractUnitTest {

    @MockBean
    private DataServiceGateway dataServiceGateway;

    @MockBean
    private SearchServiceGateway searchServiceGateway;

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @BeforeEach
    public void beforeEach() {
        genesis();
        /* metadata database */
        licenseRepository.save(LICENSE_1);
        containerRepository.save(CONTAINER_1);
        userRepository.saveAll(List.of(USER_1, USER_2, USER_3));
        databaseRepository.save(DATABASE_1);
    }

    @Test
    public void findById_succeeds() throws DatabaseNotFoundException {

        /* test */
        final Database response = databaseService.findById(DATABASE_1_ID);
        assertEquals(DATABASE_1_ID, response.getId());
        assertEquals(CONTAINER_1_ID, response.getCid());
        /* container */
        assertNotNull(response.getContainer());
        assertEquals(CONTAINER_1_ID, response.getContainer().getId());
        assertEquals(CONTAINER_1_NAME, response.getContainer().getName());
        assertEquals(CONTAINER_1_INTERNALNAME, response.getContainer().getInternalName());
        assertEquals(CONTAINER_1_HOST, response.getContainer().getHost());
        assertEquals(CONTAINER_1_PORT, response.getContainer().getPort());
        assertEquals(CONTAINER_1_UI_HOST, response.getContainer().getUiHost());
        assertEquals(CONTAINER_1_UI_PORT, response.getContainer().getUiPort());
        assertEquals(CONTAINER_1_UI_ADDITIONAL_FLAGS, response.getContainer().getUiAdditionalFlags());
        assertEquals(CONTAINER_1_SIDECAR_HOST, response.getContainer().getSidecarHost());
        assertEquals(CONTAINER_1_SIDECAR_PORT, response.getContainer().getSidecarPort());
        assertEquals(CONTAINER_1_PRIVILEGED_USERNAME, response.getContainer().getPrivilegedUsername());
        assertEquals(CONTAINER_1_PRIVILEGED_PASSWORD, response.getContainer().getPrivilegedPassword());
        assertNotNull(response.getContainer().getImage());
        assertEquals(IMAGE_1_NAME, response.getContainer().getImage().getName());
        assertEquals(IMAGE_1_VERSION, response.getContainer().getImage().getVersion());
        assertEquals(IMAGE_1_DIALECT, response.getContainer().getImage().getDialect());
        assertEquals(IMAGE_1_JDBC, response.getContainer().getImage().getJdbcMethod());
        assertEquals(IMAGE_1_DRIVER, response.getContainer().getImage().getDriverClass());
        assertEquals(IMAGE_1_REGISTRY, response.getContainer().getImage().getRegistry());
        assertEquals(IMAGE_1_PORT, response.getContainer().getImage().getDefaultPort());
        assertNotNull(response.getContainer().getImage().getDateFormats());
        assertEquals(4, response.getContainer().getImage().getDateFormats().size());
        /* creator */
        assertNotNull(response.getCreator());
        assertEquals(USER_1_ID, response.getCreator().getId());
        assertEquals(USER_1_USERNAME, response.getCreator().getUsername());
        assertEquals(USER_1_EMAIL, response.getCreator().getEmail());
        assertEquals(USER_1_THEME, response.getCreator().getTheme());
        assertEquals(USER_1_LANGUAGE, response.getCreator().getLanguage());
        assertNotNull(response.getCreator().getAccesses());
    }

    @Test
    public void updateTableMetadata_succeeds() throws SearchServiceException, MalformedException, ServiceException,
            QueryNotFoundException, DatabaseNotFoundException, ServiceConnectionException,
            SearchServiceConnectionException {

        /* mock */
        when(dataServiceGateway.getTableSchemas(DATABASE_1_ID))
                .thenReturn(List.of(TABLE_5_DTO));
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_1_DTO); /* ignored */

        /* test */
        final Database response = databaseService.updateTableMetadata(DATABASE_1);
        log.debug("");
    }

}
