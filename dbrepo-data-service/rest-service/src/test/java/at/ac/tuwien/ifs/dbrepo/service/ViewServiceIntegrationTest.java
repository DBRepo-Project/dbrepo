package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.config.PostgresContainerConfig;
import at.ac.tuwien.ifs.dbrepo.core.api.database.ViewColumnDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.ViewDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseMalformedException;
import at.ac.tuwien.ifs.dbrepo.core.exception.ViewMalformedException;
import at.ac.tuwien.ifs.dbrepo.core.exception.ViewNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.utils.MariaDbUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
@ExtendWith(SpringExtension.class)
@Testcontainers
public class ViewServiceIntegrationTest extends BaseTest {

    @Autowired
    private ViewService viewService;

    @Container
    private static PostgresContainerConfig.CustomPostgresContainer postgresContainer = PostgresContainerConfig.getContainer();

    @BeforeEach
    public void beforeEach() throws SQLException {
        /* metadata database */
        MariaDbUtil.dropDatabase(CONTAINER_1_CACHE, DATABASE_1_INTERNAL_NAME);
        MariaDbUtil.createInitDatabase(DATABASE_1_CACHE);
    }

    @Test
    public void delete_succeeds() throws SQLException, ViewMalformedException {

        /* test */
        viewService.delete(DATABASE_1_CACHE, VIEW_1_CACHE);
    }

    @Test
    public void createView_succeeds() throws SQLException, ViewMalformedException {

        /* test */
        viewService.create(DATABASE_1_CACHE, VIEW_1_NAME, VIEW_1_QUERY);
    }

    @Test
    public void exploreViews_succeeds() throws SQLException, ViewNotFoundException, DatabaseMalformedException {

        /* test */
        final List<ViewDto> response = viewService.explore(DATABASE_1_CACHE);
        final ViewDto view0 = response.get(0);
        assertEquals("not_in_metadata_db2", view0.getName());
        assertEquals("not_in_metadata_db2", view0.getInternalName());
        assertEquals(DATABASE_1_ID, view0.getDatabaseId());
        assertEquals(USER_1_USERNAME, view0.getOwner().getUsername());
        assertFalse(view0.getIsInitialView());
        assertEquals(DATABASE_1_PUBLIC, view0.getIsPublic());
        assertEquals(DATABASE_1_SCHEMA_PUBLIC, view0.getIsSchemaPublic());
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

    @Test
    public void inspectView_succeeds() throws SQLException, ViewNotFoundException {

        /* test */
        final ViewDto response = viewService.inspect(DATABASE_1_CACHE, "not_in_metadata_db2");
        assertEquals("not_in_metadata_db2", response.getInternalName());
        assertEquals("not_in_metadata_db2", response.getName());
        assertEquals(DATABASE_1_ID, response.getDatabaseId());
        assertEquals(USER_1_USERNAME, response.getOwner().getUsername());
        assertFalse(response.getIsInitialView());
        assertEquals(DATABASE_1_PUBLIC, response.getIsPublic());
        assertTrue(response.getQuery().length() >= 69);
        assertNotNull(response.getQueryHash());
        assertEquals(4, response.getColumns().size());
        final ViewColumnDto column0 = response.getColumns().get(0);
        assertNotNull(column0.getName());
        assertEquals("date", column0.getInternalName());
        assertEquals(DATABASE_1_ID, column0.getDatabaseId());
        final ViewColumnDto column1 = response.getColumns().get(1);
        assertNotNull(column1.getName());
        assertEquals("location", column1.getInternalName());
        assertEquals(DATABASE_1_ID, column1.getDatabaseId());
        final ViewColumnDto column2 = response.getColumns().get(2);
        assertNotNull(column2.getName());
        assertEquals("MinTemp", column2.getInternalName());
        assertEquals(DATABASE_1_ID, column2.getDatabaseId());
        final ViewColumnDto column3 = response.getColumns().get(3);
        assertNotNull(column3.getName());
        assertEquals("Rainfall", column3.getInternalName());
        assertEquals(DATABASE_1_ID, column3.getDatabaseId());
    }

}
