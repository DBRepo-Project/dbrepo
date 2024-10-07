package at.tuwien.service;

import at.tuwien.api.database.ViewColumnDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.MariaDbContainerConfig;
import at.tuwien.exception.*;
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
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        viewService.delete(VIEW_1_PRIVILEGED_DTO);
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
    public void data_succeeds() throws SQLException, ViewMalformedException {

        /* test */
        final QueryResultDto response = viewService.data(VIEW_2_PRIVILEGED_DTO, Instant.now(), 0L, 10L);
        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals(VIEW_2_ID, response.getId());
        assertNotNull(response.getHeaders());
        assertEquals(4, response.getHeaders().size());
        assertEquals(List.of(Map.of("date", 0), Map.of("loc", 1), Map.of("mintemp", 2), Map.of("rainfall", 3)), response.getHeaders());
        assertNotNull(response.getResult());
        assertEquals(3, response.getResult().size());
        /* row 0 */
        assertEquals(Instant.ofEpochSecond(1228089600), response.getResult().get(0).get("date"));
        assertEquals("Albury", response.getResult().get(0).get("loc"));
        assertEquals(13.4, response.getResult().get(0).get("mintemp"));
        assertEquals(0.6, response.getResult().get(0).get("rainfall"));
        /* row 1 */
        assertEquals(Instant.ofEpochSecond(1228176000), response.getResult().get(1).get("date"));
        assertEquals("Albury", response.getResult().get(1).get("loc"));
        assertEquals(7.4, response.getResult().get(1).get("mintemp"));
        assertEquals(0.0, response.getResult().get(1).get("rainfall"));
        /* row 2 */
        assertEquals(Instant.ofEpochSecond(1228262400), response.getResult().get(2).get("date"));
        assertEquals("Albury", response.getResult().get(2).get("loc"));
        assertEquals(12.9, response.getResult().get(2).get("mintemp"));
        assertEquals(0.0, response.getResult().get(2).get("rainfall"));
    }

    @Test
    public void getSchemas_succeeds() throws ViewMalformedException, SQLException, ViewNotFoundException,
            DatabaseMalformedException, ViewSchemaException {

        /* test */
        final List<ViewDto> response = viewService.getSchemas(DATABASE_1_PRIVILEGED_DTO);
        final ViewDto view0 = response.get(0);
        assertEquals("not_in_metadata_db2", view0.getName());
        assertEquals("not_in_metadata_db2", view0.getInternalName());
        assertEquals(DATABASE_1_ID, view0.getVdbid());
        assertEquals(DATABASE_1_ID, view0.getDatabase().getId());
        assertEquals(DATABASE_1_OWNER, view0.getCreatedBy());
        assertEquals(DATABASE_1_OWNER, view0.getCreator().getId());
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
