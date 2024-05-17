package at.tuwien.service;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
        viewService.create(DATABASE_1_PRIVILEGED_DTO, VIEW_1_CREATE_DTO);
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
        assertEquals(List.of(Map.of("date", 0), Map.of("location", 1), Map.of("rainfall", 2), Map.of("mintemp", 3)), response.getHeaders());
        assertNotNull(response.getResult());
        assertEquals(3, response.getResult().size());
        /* row 0 */
        assertEquals(Instant.ofEpochSecond(1228089600), response.getResult().get(0).get("date"));
        assertEquals("Albury", response.getResult().get(0).get("location"));
        assertEquals(13.4, response.getResult().get(0).get("mintemp"));
        assertEquals(0.6, response.getResult().get(0).get("rainfall"));
        /* row 1 */
        assertEquals(Instant.ofEpochSecond(1228176000), response.getResult().get(1).get("date"));
        assertEquals("Albury", response.getResult().get(1).get("location"));
        assertEquals(7.4, response.getResult().get(1).get("mintemp"));
        assertEquals(0.0, response.getResult().get(1).get("rainfall"));
        /* row 2 */
        assertEquals(Instant.ofEpochSecond(1228262400), response.getResult().get(2).get("date"));
        assertEquals("Albury", response.getResult().get(2).get("location"));
        assertEquals(12.9, response.getResult().get(2).get("mintemp"));
        assertEquals(0.0, response.getResult().get(2).get("rainfall"));
    }

}
