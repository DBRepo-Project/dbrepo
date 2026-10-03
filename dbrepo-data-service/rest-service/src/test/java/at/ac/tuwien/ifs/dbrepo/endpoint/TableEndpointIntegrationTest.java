package at.ac.tuwien.ifs.dbrepo.endpoint;

import at.ac.tuwien.ifs.dbrepo.config.MariaDbContainerConfig;
import at.ac.tuwien.ifs.dbrepo.config.RedisContainerConfig;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.endpoints.TableEndpoint;
import at.ac.tuwien.ifs.dbrepo.gateway.KeycloakGateway;
import at.ac.tuwien.ifs.dbrepo.gateway.MetadataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.service.MetadataService;
import at.ac.tuwien.ifs.dbrepo.utils.MariaDbUtil;
import jakarta.servlet.http.HttpServletRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Slf4j
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@SpringBootTest
@Testcontainers
public class TableEndpointIntegrationTest extends BaseTest {

    @Autowired
    private TableEndpoint tableEndpoint;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MetadataServiceGateway metadataServiceGateway;

    @MockitoBean
    private MetadataService metadataService;

    @MockitoBean
    private KeycloakGateway keycloakGateway;

    @MockitoBean
    private HttpServletRequest httpServletRequest;

    @Container
    private static MariaDBContainer<?> mariaDBContainer = MariaDbContainerConfig.getContainer();

    @Container
    private static RedisContainerConfig.CustomRedisContainer redisContainer = RedisContainerConfig.getContainer();

    @BeforeEach
    public void beforeEach() throws SQLException {
        /* metadata database */
        MariaDbUtil.dropDatabase(CONTAINER_1_CACHE, DATABASE_1_INTERNAL_NAME);
        MariaDbUtil.createInitDatabase(DATABASE_1_CACHE);
    }

    @Test
    public void getData_succeeds() throws Exception {

        /* mock */
        when(metadataService.getTable(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_CACHE);
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);
        when(httpServletRequest.getMethod())
                .thenReturn("GET");

        /* test */
        final ResponseEntity<?> response = tableEndpoint.getData(DATABASE_1_ID, TABLE_1_ID, null, null, null, null, null, MediaType.APPLICATION_JSON_VALUE, httpServletRequest, USER_LOCAL_ADMIN_PRINCIPAL);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        final List<Map<String, Object>> body = objectMapper.readValue(write(response), new TypeReference<>() {
        });
        assertNotNull(body);
        assertEquals(3, body.size());
        assertEquals(List.of(1, 2, 3), body.stream().map(row -> (Integer) row.get("id")).toList());
        assertEquals(Map.of("id", 1, "date", "2008-12-01", "location", "Albury", "mintemp", 13.4, "rainfall", 0.6), body.get(0));
        assertEquals(Map.of("id", 2, "date", "2008-12-02", "location", "Albury", "mintemp", 7.4, "rainfall", 0.0), body.get(1));
        assertEquals(Map.of("id", 3, "date", "2008-12-03", "location", "Albury", "mintemp", 12.9, "rainfall", 0.0), body.get(2));
    }

    @Test
    public void getData_sortsByExplicitColumn() throws Exception {

        /* mock */
        when(metadataService.getTable(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_CACHE);
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);
        when(httpServletRequest.getMethod())
                .thenReturn("GET");

        /* test */
        final ResponseEntity<?> response = tableEndpoint.getData(DATABASE_1_ID, TABLE_1_ID, null, null, null, "date", "desc", MediaType.APPLICATION_JSON_VALUE, httpServletRequest, USER_LOCAL_ADMIN_PRINCIPAL);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        final List<Map<String, Object>> body = objectMapper.readValue(write(response), new TypeReference<>() {
        });
        assertNotNull(body);
        assertEquals(3, body.size());
        assertEquals(List.of(3, 2, 1), body.stream().map(row -> (Integer) row.get("id")).toList());
        assertEquals(Map.of("id", 3, "date", "2008-12-03", "location", "Albury", "mintemp", 12.9, "rainfall", 0.0), body.get(0));
        assertEquals(Map.of("id", 2, "date", "2008-12-02", "location", "Albury", "mintemp", 7.4, "rainfall", 0.0), body.get(1));
        assertEquals(Map.of("id", 1, "date", "2008-12-01", "location", "Albury", "mintemp", 13.4, "rainfall", 0.6), body.get(2));
    }

    @Test
    public void getData_csv_streamsResponse() throws Exception {

        /* mock */
        when(metadataService.getTable(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_CACHE);
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);

        /* test */
        final MvcResult result = mockMvc.perform(get("/api/v1/database/" + DATABASE_1_ID + "/table/" + TABLE_1_ID + "/data")
                        .accept("text/csv")
                        .with(authentication((Authentication) USER_LOCAL_ADMIN_PRINCIPAL)))
                .andExpect(request().asyncStarted())
                .andReturn();
        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"))
                .andExpect(header().string("X-Headers", "id,date,location,mintemp,rainfall"))
                .andExpect(content().string("id,date,location,mintemp,rainfall" + System.lineSeparator() +
                        "1,2008-12-01,Albury,13.4,0.6" + System.lineSeparator() +
                        "2,2008-12-02,Albury,7.4,0.0" + System.lineSeparator() +
                        "3,2008-12-03,Albury,12.9,0.0" + System.lineSeparator()));
    }

    private static byte[] write(ResponseEntity<?> response) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        ((StreamingResponseBody) response.getBody()).writeTo(out);
        return out.toByteArray();
    }

}
