package at.tuwien.mvc;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.database.AccessTypeDto;
import at.tuwien.api.database.DatabaseGiveAccessDto;
import at.tuwien.api.database.DatabaseModifyAccessDto;
import at.tuwien.config.MetricsConfig;
import at.tuwien.endpoints.AccessEndpoint;
import io.micrometer.observation.tck.TestObservationRegistry;
import lombok.SneakyThrows;
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

import java.util.List;

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
@MockAmqp
@MockOpensearch
public class PrometheusEndpointMvcTest extends BaseUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestObservationRegistry registry;

    @Autowired
    private AccessEndpoint accessEndpoint;

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
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-database-access", "update-database-access", "check-database-access", "delete-database-access"})
    public void prometheusAccessEndpoint_succeeds() throws Exception {

        /* mock */
        try {
            accessEndpoint.create(DATABASE_1_ID, USER_1_ID, DatabaseGiveAccessDto.builder().type(AccessTypeDto.READ).build(), USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            accessEndpoint.update(DATABASE_1_ID, USER_1_ID, DatabaseModifyAccessDto.builder().type(AccessTypeDto.READ).build(), USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            accessEndpoint.find(DATABASE_1_ID, USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            accessEndpoint.revoke(DATABASE_1_ID, USER_1_ID, USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }


        this.mockMvc.perform(get("/actuator/prometheus"))
                .andDo(print())
                .andExpect(status().isOk());
        /* test */
        for (String metric : List.of("dbr_access_give", "dbr_access_modify", "dbr_access_check", "dbr_access_delete")) {
            assertThat(registry)
                    .hasObservationWithNameEqualTo(metric);
        }
    }

}
