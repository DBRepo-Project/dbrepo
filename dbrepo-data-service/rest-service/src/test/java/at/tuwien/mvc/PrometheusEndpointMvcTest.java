package at.tuwien.mvc;

import at.tuwien.api.database.query.ExecuteStatementDto;
import at.tuwien.api.database.query.ImportDto;
import at.tuwien.api.database.query.QueryPersistDto;
import at.tuwien.api.database.table.TupleDeleteDto;
import at.tuwien.api.database.table.TupleDto;
import at.tuwien.api.database.table.TupleUpdateDto;
import at.tuwien.config.MetricsConfig;
import at.tuwien.endpoints.SubsetEndpoint;
import at.tuwien.endpoints.TableEndpoint;
import at.tuwien.endpoints.ViewEndpoint;
import at.tuwien.listener.DefaultListener;
import at.tuwien.test.AbstractUnitTest;
import io.micrometer.observation.tck.TestObservationRegistry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;

import static at.tuwien.utils.RabbitMqUtils.buildMessage;
import static io.micrometer.observation.tck.TestObservationRegistryAssert.assertThat;
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
    private SubsetEndpoint subsetEndpoint;

    @Autowired
    private TableEndpoint tableEndpoint;

    @Autowired
    private ViewEndpoint viewEndpoint;

    @TestConfiguration
    static class ObservationTestConfiguration {

        @Bean
        public TestObservationRegistry observationRegistry() {
            return TestObservationRegistry.create();
        }
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
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"execute-query", "persist-query"})
    public void prometheusSubsetEndpoint_succeeds() {

        /* mock */
        try {
            subsetEndpoint.list(DATABASE_1_ID, null, USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            subsetEndpoint.create(DATABASE_1_ID, ExecuteStatementDto.builder().statement(QUERY_5_STATEMENT).build(), USER_1_PRINCIPAL, httpServletRequest, null, 0L, 10L);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            subsetEndpoint.getData(DATABASE_1_ID, QUERY_1_ID, USER_1_PRINCIPAL, httpServletRequest, null, 0L, 10L);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            subsetEndpoint.persist(DATABASE_1_ID, QUERY_1_ID, QueryPersistDto.builder().persist(true).build(), USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            subsetEndpoint.findById(DATABASE_1_ID, QUERY_1_ID, "application/json", null, USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }

        /* test */
        for (String metric : List.of("dbrepo_subset_list", "dbrepo_subset_create", "dbrepo_subset_data",
                "dbrepo_subset_persist", "dbrepo_subset_find")) {
            assertThat(registry)
                    .hasObservationWithNameEqualTo(metric);
        }
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"insert-table-data", "delete-table-data"})
    public void prometheusTableEndpoint_succeeds() {

        /* mock */
        try {
            tableEndpoint.getData(DATABASE_1_ID, TABLE_1_ID, null, null, null, httpServletRequest, null);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            tableEndpoint.insertRawTuple(DATABASE_1_ID, TABLE_1_ID, TupleDto.builder().build(), USER_1_PRINCIPAL, TOKEN_ACCESS_TOKEN);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            tableEndpoint.updateRawTuple(DATABASE_1_ID, TABLE_1_ID, TupleUpdateDto.builder().build(), USER_1_PRINCIPAL, TOKEN_ACCESS_TOKEN);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            tableEndpoint.deleteRawTuple(DATABASE_1_ID, TABLE_1_ID, TupleDeleteDto.builder().build(), USER_1_PRINCIPAL, TOKEN_ACCESS_TOKEN);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            tableEndpoint.getHistory(DATABASE_1_ID, TABLE_1_ID, null, USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            tableEndpoint.exportDataset(DATABASE_1_ID, TABLE_1_ID, null, USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            tableEndpoint.importDataset(DATABASE_1_ID, TABLE_1_ID, ImportDto.builder().build(), USER_1_PRINCIPAL, TOKEN_ACCESS_TOKEN);
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
    }

}
