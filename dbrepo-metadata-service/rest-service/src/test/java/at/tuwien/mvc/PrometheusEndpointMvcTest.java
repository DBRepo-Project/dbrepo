package at.tuwien.mvc;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.container.ContainerCreateRequestDto;
import at.tuwien.api.database.*;
import at.tuwien.config.MetricsConfig;
import at.tuwien.endpoints.*;
import io.micrometer.observation.tck.TestObservationRegistry;
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

    @Autowired
    private ContainerEndpoint containerEndpoint;

    @Autowired
    private DatabaseEndpoint databaseEndpoint;

    @Autowired
    private ExportEndpoint exportEndpoint;

    @Autowired
    private IdentifierEndpoint identifierEndpoint;

    @Autowired
    private ImageEndpoint imageEndpoint;

    @Autowired
    private LicenseEndpoint licenseEndpoint;

    @Autowired
    private MaintenanceEndpoint maintenanceEndpoint;

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
    public void prometheusAccessEndpoint_succeeds() {

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

        /* test */
        for (String metric : List.of("dbr_access_give", "dbr_access_modify", "dbr_access_check", "dbr_access_delete")) {
            assertThat(registry)
                    .hasObservationWithNameEqualTo(metric);
        }
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-container", "delete-container"})
    public void prometheusContainerEndpoint_succeeds() {

        /* mock */
        try {
            containerEndpoint.findAll(USER_1_PRINCIPAL, null);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            containerEndpoint.create(ContainerCreateRequestDto.builder().name(CONTAINER_1_NAME).imageId(IMAGE_1_ID).build(), USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            containerEndpoint.findById(CONTAINER_1_ID);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            containerEndpoint.delete(CONTAINER_1_ID, USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }

        /* test */
        for (String metric : List.of("dbr_container_findall", "dbr_container_create", "dbr_container_find", "dbr_container_delete")) {
            assertThat(registry)
                    .hasObservationWithNameEqualTo(metric);
        }
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-database", "modify-database-visibility", "modify-database-owner", "delete-database"})
    public void prometheusDatabaseEndpoint_succeeds() {

        /* mock */
        try {
            databaseEndpoint.list(USER_1_PRINCIPAL, null);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            databaseEndpoint.count(USER_1_PRINCIPAL, null);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            databaseEndpoint.create(DATABASE_1_CREATE, USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            databaseEndpoint.visibility(DATABASE_1_ID, DatabaseModifyVisibilityDto.builder().isPublic(true).build(), USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            databaseEndpoint.transfer(DATABASE_1_ID, DatabaseTransferDto.builder().username(USER_2_USERNAME).build(), USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            databaseEndpoint.findById(DATABASE_1_ID, USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            databaseEndpoint.delete(DATABASE_1_ID, USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }

        /* test */
        for (String metric : List.of("dbr_database_findall", "dbr_database_count", "dbr_database_create", "dbr_database_visibility", "dbr_database_transfer", "dbr_database_find", "dbr_database_delete")) {
            assertThat(registry)
                    .hasObservationWithNameEqualTo(metric);
        }
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void prometheusExportEndpoint_succeeds() {

        /* mock */
        try {
            exportEndpoint.export(DATABASE_1_ID, TABLE_1_ID, null, USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }

        /* test */
        for (String metric : List.of("dbr_table_export")) {
            assertThat(registry)
                    .hasObservationWithNameEqualTo(metric);
        }
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier", "create-foreign-identifier"})
    public void prometheusIdentifierEndpoint_succeeds() {

        /* mock */
        try {
            identifierEndpoint.list(DATABASE_1_ID, null, null, IDENTIFIER_1_TYPE_DTO);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            identifierEndpoint.create(IDENTIFIER_1_DTO_REQUEST, USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            identifierEndpoint.retrieve(USER_1_ORCID_URL);
        } catch (Exception e) {
            /* ignore */
        }

        /* test */
        for (String metric : List.of("dbr_identifier_findall", "dbr_identifier_create", "dbr_identifier_retrieve")) {
            assertThat(registry)
                    .hasObservationWithNameEqualTo(metric);
        }
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-image", "modify-image", "delete-image"})
    public void prometheusImageEndpoint_succeeds() {

        /* mock */
        try {
            imageEndpoint.findAll(USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            imageEndpoint.create(IMAGE_1_CREATE_DTO, USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            imageEndpoint.findById(IMAGE_1_ID);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            imageEndpoint.update(IMAGE_1_ID, IMAGE_1_CHANGE_DTO, USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            imageEndpoint.delete(IMAGE_1_ID, USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }

        /* test */
        for (String metric : List.of("dbr_image_findall", "dbr_image_create", "dbr_image_find", "dbr_image_update", "dbr_image_delete")) {
            assertThat(registry)
                    .hasObservationWithNameEqualTo(metric);
        }
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void prometheusLicenseEndpoint_succeeds() {

        /* mock */
        try {
            licenseEndpoint.list();
        } catch (Exception e) {
            /* ignore */
        }

        /* test */
        assertThat(registry)
                .hasObservationWithNameEqualTo("dbr_license_findall");
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-maintenance-message", "update-maintenance-message", "delete-maintenance-message"})
    public void prometheusMaintenanceEndpoint_succeeds() {

        /* mock */
        try {
            maintenanceEndpoint.list();
        } catch (Exception e) {
            /* ignore */
        }
        try {
            maintenanceEndpoint.find(BANNER_MESSAGE_1_ID);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            maintenanceEndpoint.active();
        } catch (Exception e) {
            /* ignore */
        }
        try {
            maintenanceEndpoint.create(BANNER_MESSAGE_1_CREATE_DTO);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            maintenanceEndpoint.update(BANNER_MESSAGE_1_ID, BANNER_MESSAGE_1_UPDATE_DTO);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            maintenanceEndpoint.delete(BANNER_MESSAGE_1_ID);
        } catch (Exception e) {
            /* ignore */
        }

        /* test */
        for (String metric : List.of("dbr_maintenance_findall", "dbr_maintenance_find", "dbr_maintenance_findactive", "dbr_maintenance_create", "dbr_maintenance_update", "dbr_maintenance_delete")) {
            assertThat(registry)
                    .hasObservationWithNameEqualTo(metric);
        }
    }

}
