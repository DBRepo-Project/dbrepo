package at.tuwien.service;

import at.tuwien.api.database.ViewColumnDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.MariaDbContainerConfig;
import at.tuwien.exception.DatabaseMalformedException;
import at.tuwien.exception.ViewMalformedException;
import at.tuwien.exception.ViewNotFoundException;
import at.tuwien.test.AbstractUnitTest;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
@Testcontainers
public class ViewServiceIntegrationTest extends AbstractUnitTest {

    @Autowired
    private ViewService viewService;

    @Container
    private static MariaDBContainer<?> mariaDBContainer = MariaDbContainerConfig.getContainer();

    @BeforeEach
    public void beforeEach() throws SQLException {
        genesis();
        /* metadata database */
        MariaDbConfig.dropDatabase(CONTAINER_1_PRIVILEGED_DTO, DATABASE_1_INTERNALNAME);
        MariaDbConfig.createInitDatabase(CONTAINER_1_PRIVILEGED_DTO, DATABASE_1_DTO);
    }

    @Test
    public void delete_succeeds() throws SQLException, ViewMalformedException {

        /* test */
        viewService.delete(DATABASE_1_PRIVILEGED_DTO, VIEW_1_INTERNAL_NAME);
    }

    @Test
    public void create_succeeds() throws SQLException, ViewMalformedException {

        /* test */
        final ViewDto response = viewService.create(DATABASE_1_PRIVILEGED_DTO, VIEW_1_CREATE_DTO);
        assertEquals(VIEW_1_NAME, response.getName());
        assertEquals(VIEW_1_INTERNAL_NAME, response.getInternalName());
        assertEquals(VIEW_1_QUERY, response.getQuery());
        assertNotNull(response.getQueryHash());
        assertEquals(DATABASE_1_PUBLIC, response.getIsPublic());
        final List<ViewColumnDto> columns = response.getColumns();
        assertEquals(VIEW_1_COLUMNS.size(), columns.size());
        ViewColumnDto ref = VIEW_1_COLUMNS_DTO.get(0);
        SchemaServiceIntegrationTest.assertViewColumn(columns.get(0), null, ref.getDatabaseId(), ref.getName(), ref.getInternalName(), ref.getColumnType(), ref.getSize(), ref.getD(), ref.getIsNullAllowed(), ref.getDescription());
        ref = VIEW_1_COLUMNS_DTO.get(1);
        SchemaServiceIntegrationTest.assertViewColumn(columns.get(1), null, ref.getDatabaseId(), ref.getName(), ref.getInternalName(), ref.getColumnType(), ref.getSize(), ref.getD(), ref.getIsNullAllowed(), ref.getDescription());
        ref = VIEW_1_COLUMNS_DTO.get(2);
        SchemaServiceIntegrationTest.assertViewColumn(columns.get(2), null, ref.getDatabaseId(), ref.getName(), ref.getInternalName(), ref.getColumnType(), ref.getSize(), ref.getD(), ref.getIsNullAllowed(), ref.getDescription());

    }

    @Test
    public void getSchemas_succeeds() throws SQLException, ViewNotFoundException, DatabaseMalformedException {

        /* test */
        final List<ViewDto> response = viewService.getSchemas(DATABASE_1_PRIVILEGED_DTO);
        final ViewDto view0 = response.get(0);
        assertEquals("not_in_metadata_db2", view0.getName());
        assertEquals("not_in_metadata_db2", view0.getInternalName());
        assertEquals(DATABASE_1_ID, view0.getVdbid());
        assertEquals(DATABASE_1_ID, view0.getDatabase().getId());
        assertEquals(DATABASE_1_OWNER, view0.getOwner().getId());
        assertFalse(view0.getIsInitialView());
        assertEquals(DATABASE_1_PUBLIC, view0.getIsPublic());
        assertTrue(view0.getQuery().length() >= 69);
        assertNotNull(view0.getQueryHash());
        assertEquals(4, view0.getColumns().size());
        final ViewColumnDto column0a = view0.getColumns().get(0);
        assertEquals("date", column0a.getInternalName());
        final ViewColumnDto column1a = view0.getColumns().get(1);
        assertEquals("location", column1a.getInternalName());
        final ViewColumnDto column2a = view0.getColumns().get(2);
        assertEquals("MinTemp", column2a.getInternalName());
        final ViewColumnDto column3a = view0.getColumns().get(3);
        assertEquals("Rainfall", column3a.getInternalName());
    }

}
