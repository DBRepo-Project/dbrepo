package at.tuwien.mvc;

import at.tuwien.api.database.query.ExecuteStatementDto;
import at.tuwien.api.database.query.ImportCsvDto;
import at.tuwien.api.database.query.QueryPersistDto;
import at.tuwien.api.database.table.TupleDeleteDto;
import at.tuwien.api.database.table.TupleDto;
import at.tuwien.api.database.table.TupleUpdateDto;
import at.tuwien.config.MetricsConfig;
import at.tuwien.endpoints.*;
import at.tuwien.listener.DefaultListener;
import at.tuwien.test.AbstractUnitTest;
import io.micrometer.observation.annotation.Observed;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;

import static at.tuwien.utils.RabbitMqUtils.buildMessage;
import static io.micrometer.observation.tck.TestObservationRegistryAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Log4j2
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@SpringBootTest
@Import(MetricsConfig.class)
@AutoConfigureObservability
public class PrometheusEndpointMvcTest extends AbstractUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestObservationRegistry registry;

    @Autowired
    private DefaultListener defaultListener;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private AccessEndpoint accessEndpoint; /* no metrics */

    @Autowired
    private DatabaseEndpoint databaseEndpoint; /* no metrics */

    @Autowired
    private SubsetEndpoint subsetEndpoint;

    @Autowired
    private TableEndpoint tableEndpoint;

    @Autowired
    private ViewEndpoint viewEndpoint;

    private static final Map<String, String> metrics = new TreeMap<>(); /* sorted */

    @TestConfiguration
    static class ObservationTestConfiguration {

        @Bean
        public TestObservationRegistry observationRegistry() {
            return TestObservationRegistry.create();
        }
    }

    @BeforeAll
    public static void beforeAll() {
        FileUtils.deleteQuietly(new File("../metrics.txt"));
    }

    @AfterAll
    public static void afterAll() throws IOException {
        saveObservedMetrics(metrics);
    }

    @Test
    public void prometheus_succeeds() throws Exception {

        /* test */
        this.mockMvc.perform(get("/actuator/prometheus"))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    public void prometheusDefaultListener_succeeds() {

        /* mock */
        try {
            defaultListener.onMessage(buildMessage("dbrepo.database", "{}", new HashMap<>()));
        } catch (Exception e) {
            /* ignore */
        }

        /* test */
        assertThat(registry)
                .hasObservationWithNameEqualTo("dbrepo_message_receive");
        generic_openApiDocs(DefaultListener.class);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"dbrepo_subset_list", "execute-query", "persist-query"})
    public void prometheusSubsetEndpoint_succeeds() {

        /* mock */
        try {
            subsetEndpoint.list(DATABASE_1_ID, null, USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            subsetEndpoint.create(DATABASE_1_ID, ExecuteStatementDto.builder().statement(QUERY_5_STATEMENT).build(), USER_1_PRINCIPAL, 0L, 10L, null);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            subsetEndpoint.getData(DATABASE_1_ID, QUERY_1_ID, USER_1_PRINCIPAL, httpServletRequest, 0L, 10L);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            subsetEndpoint.persist(DATABASE_1_ID, QUERY_1_ID, QueryPersistDto.builder().persist(true).build(), USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            subsetEndpoint.findById(DATABASE_1_ID, QUERY_1_ID, new MockHttpServletRequest(), null, USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }

        /* test */
        for (String metric : List.of("dbrepo_subset_list", "dbrepo_subset_create", "dbrepo_subset_data",
                "dbrepo_subset_persist", "dbrepo_subset_find")) {
            assertThat(registry)
                    .hasObservationWithNameEqualTo(metric);
        }
        generic_openApiDocs(SubsetEndpoint.class);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"insert-table-data", "delete-table-data"})
    public void prometheusTableEndpoint_succeeds() {

        /* mock */
        try {
            tableEndpoint.getData(DATABASE_1_ID, TABLE_1_ID, null, null, null);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            tableEndpoint.insertRawTuple(DATABASE_1_ID, TABLE_1_ID, TupleDto.builder().build(), USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            tableEndpoint.updateRawTuple(DATABASE_1_ID, TABLE_1_ID, TupleUpdateDto.builder().build(), USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            tableEndpoint.deleteRawTuple(DATABASE_1_ID, TABLE_1_ID, TupleDeleteDto.builder().build(), USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            tableEndpoint.getHistory(DATABASE_1_ID, TABLE_1_ID, null, USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            tableEndpoint.exportData(DATABASE_1_ID, TABLE_1_ID, null, USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            tableEndpoint.exportData(DATABASE_1_ID, TABLE_1_ID, null, USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            tableEndpoint.importDataset(DATABASE_1_ID, TABLE_1_ID, ImportCsvDto.builder().build(), USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }

        /* test */
        for (String metric : List.of("dbrepo_table_data_list", "dbrepo_table_data_create", "dbrepo_table_data_update",
                "dbrepo_table_data_delete", "dbrepo_table_data_history", "dbrepo_table_data_export",
                "dbrepo_table_data_import")) {
            assertThat(registry)
                    .hasObservationWithNameEqualTo(metric);
        }
        generic_openApiDocs(TableEndpoint.class);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"view-database-view-data"})
    public void prometheusViewEndpoint_succeeds() {

        /* mock */
        try {
            viewEndpoint.getData(DATABASE_1_ID, VIEW_1_ID, 0L, 10L, null, httpServletRequest, USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }

        /* test */
        assertThat(registry)
                .hasObservationWithNameEqualTo("dbrepo_view_data");
        generic_openApiDocs(ViewEndpoint.class);
    }

    private static void generic_openApiDocs(Class<?> endpoint) {
        final List<Method> methods = Arrays.stream(endpoint.getMethods())
                .filter(m -> m.getDeclaringClass().equals(endpoint))
                .toList();
        methods.forEach(m -> {
            final Observed observed = m.getDeclaredAnnotation(Observed.class);
            final Operation operation = m.getDeclaredAnnotation(Operation.class);
            if (observed != null) {
                assertNotNull(operation);
                metrics.put(observed.name(), operation.summary());
            }
        });
    }

}
