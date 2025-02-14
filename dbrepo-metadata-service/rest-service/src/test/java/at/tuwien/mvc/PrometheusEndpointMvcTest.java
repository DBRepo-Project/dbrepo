package at.tuwien.mvc;

import at.tuwien.api.container.CreateContainerDto;
import at.tuwien.api.database.DatabaseModifyImageDto;
import at.tuwien.api.database.DatabaseModifyVisibilityDto;
import at.tuwien.api.database.DatabaseTransferDto;
import at.tuwien.api.database.table.columns.concepts.ColumnSemanticsUpdateDto;
import at.tuwien.api.identifier.IdentifierTypeDto;
import at.tuwien.config.MetricsConfig;
import at.tuwien.endpoints.*;
import at.tuwien.test.AbstractUnitTest;
import io.micrometer.observation.annotation.Observed;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
    private AccessEndpoint accessEndpoint;

    @Autowired
    private ContainerEndpoint containerEndpoint;

    @Autowired
    private ConceptEndpoint conceptEndpoint;

    @Autowired
    private UnitEndpoint unitEndpoint;

    @Autowired
    private DatabaseEndpoint databaseEndpoint;

    @Autowired
    private IdentifierEndpoint identifierEndpoint;

    @Autowired
    private ImageEndpoint imageEndpoint;

    @Autowired
    private LicenseEndpoint licenseEndpoint;

    @Autowired
    private MessageEndpoint messageEndpoint;

    @Autowired
    private MetadataEndpoint metadataEndpoint;

    @Autowired
    private OntologyEndpoint ontologyEndpoint;

    @Autowired
    private TableEndpoint tableEndpoint;

    @Autowired
    private UserEndpoint userEndpoint;

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
        FileUtils.deleteQuietly(new File("../metrics.md"));
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
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-database-access", "update-database-access", "check-database-access", "delete-database-access"})
    public void prometheusAccessEndpoint_succeeds() {

        /* mock */
        try {
            accessEndpoint.create(DATABASE_1_ID, USER_1_ID, UPDATE_DATABASE_ACCESS_READ_DTO, USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            accessEndpoint.update(DATABASE_1_ID, USER_1_ID, UPDATE_DATABASE_ACCESS_READ_DTO, USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            accessEndpoint.find(DATABASE_1_ID, USER_1_ID, USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            accessEndpoint.revoke(DATABASE_1_ID, USER_1_ID, USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }

        /* test */
        for (String metric : List.of("dbrepo_access_give", "dbrepo_access_get", "dbrepo_access_modify", "dbrepo_access_get", "dbrepo_access_delete")) {
            assertThat(registry)
                    .hasObservationWithNameEqualTo(metric);
        }
        generic_openApiDocs(AccessEndpoint.class);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-container", "delete-container"})
    public void prometheusContainerEndpoint_succeeds() {

        /* mock */
        try {
            containerEndpoint.findAll(null);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            containerEndpoint.create(CreateContainerDto.builder().name(CONTAINER_1_NAME).imageId(IMAGE_1_ID).build());
        } catch (Exception e) {
            /* ignore */
        }
        try {
            containerEndpoint.findById(CONTAINER_1_ID, USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            containerEndpoint.delete(CONTAINER_1_ID);
        } catch (Exception e) {
            /* ignore */
        }

        /* test */
        for (String metric : List.of("dbrepo_container_findall", "dbrepo_container_create", "dbrepo_container_find", "dbrepo_container_delete")) {
            assertThat(registry)
                    .hasObservationWithNameEqualTo(metric);
        }
        generic_openApiDocs(ContainerEndpoint.class);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-database", "modify-database-visibility", "modify-database-owner", "delete-database", "modify-database-image"})
    public void prometheusDatabaseEndpoint_succeeds() {

        /* mock */
        try {
            databaseEndpoint.list(null, null);
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
            databaseEndpoint.transfer(DATABASE_1_ID, DatabaseTransferDto.builder().id(USER_2_ID).build(), USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            databaseEndpoint.findById(DATABASE_1_ID, USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            databaseEndpoint.modifyImage(DATABASE_1_ID, DatabaseModifyImageDto.builder().build(), USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            databaseEndpoint.findPreviewImage(DATABASE_1_ID);
        } catch (Exception e) {
            /* ignore */
        }

        /* test */
        for (String metric : List.of("dbrepo_database_findall", "dbrepo_database_create", "dbrepo_database_visibility", "dbrepo_database_transfer", "dbrepo_database_find", "dbrepo_database_image", "dbrepo_database_image_view")) {
            assertThat(registry)
                    .hasObservationWithNameEqualTo(metric);
        }
        generic_openApiDocs(DatabaseEndpoint.class);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier", "create-foreign-identifier", "publish-identifier"})
    public void prometheusIdentifierEndpoint_succeeds() {

        /* mock */
        try {
            identifierEndpoint.create(IDENTIFIER_1_CREATE_DTO, USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            identifierEndpoint.save(IDENTIFIER_1_ID, IDENTIFIER_1_SAVE_DTO, USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            identifierEndpoint.publish(IDENTIFIER_1_ID);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            identifierEndpoint.retrieve(USER_1_ORCID_URL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            identifierEndpoint.delete(IDENTIFIER_1_ID);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            identifierEndpoint.findAll(IdentifierTypeDto.DATABASE, null, DATABASE_1_ID, null, null, null, MediaType.APPLICATION_JSON_VALUE, null);
        } catch (Exception e) {
            /* ignore */
        }

        /* test */
        for (String metric : List.of("dbrepo_identifier_create", "dbrepo_identifier_retrieve",
                "dbrepo_identifier_list", "dbrepo_identifier_save",
                "dbrepo_identifier_publish")) {
            assertThat(registry)
                    .hasObservationWithNameEqualTo(metric);
        }
        generic_openApiDocs(IdentifierEndpoint.class);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-image", "modify-image", "delete-image"})
    public void prometheusImageEndpoint_succeeds() {

        /* mock */
        try {
            imageEndpoint.findAll();
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
            imageEndpoint.update(IMAGE_1_ID, IMAGE_1_CHANGE_DTO);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            imageEndpoint.delete(IMAGE_1_ID);
        } catch (Exception e) {
            /* ignore */
        }

        /* test */
        for (String metric : List.of("dbrepo_image_findall", "dbrepo_image_create",
                "dbrepo_image_find", "dbrepo_image_update", "dbrepo_image_delete")) {
            assertThat(registry)
                    .hasObservationWithNameEqualTo(metric);
        }
        generic_openApiDocs(ImageEndpoint.class);
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
                .hasObservationWithNameEqualTo("dbrepo_license_findall");
        generic_openApiDocs(LicenseEndpoint.class);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-maintenance-message", "update-maintenance-message", "delete-maintenance-message"})
    public void prometheusMaintenanceEndpoint_succeeds() {

        /* mock */
        try {
            messageEndpoint.list(null);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            messageEndpoint.find(BANNER_MESSAGE_1_ID);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            messageEndpoint.create(BANNER_MESSAGE_1_CREATE_DTO);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            messageEndpoint.update(BANNER_MESSAGE_1_ID, BANNER_MESSAGE_1_UPDATE_DTO);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            messageEndpoint.delete(BANNER_MESSAGE_1_ID);
        } catch (Exception e) {
            /* ignore */
        }

        /* test */
        for (String metric : List.of("dbrepo_maintenance_findall", "dbrepo_maintenance_find", "dbrepo_maintenance_create", "dbrepo_maintenance_update", "dbrepo_maintenance_delete")) {
            assertThat(registry)
                    .hasObservationWithNameEqualTo(metric);
        }
        generic_openApiDocs(MessageEndpoint.class);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void prometheusMetadataEndpoint_succeeds() {

        /* mock */
        try {
            metadataEndpoint.identify();
        } catch (Exception e) {
            /* ignore */
        }
        try {
            metadataEndpoint.listIdentifiers(null);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            metadataEndpoint.getRecord(null);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            metadataEndpoint.listMetadataFormats();
        } catch (Exception e) {
            /* ignore */
        }

        /* test */
        for (String metric : List.of("dbrepo_oai_identify", "dbrepo_oai_identifiers_list", "dbrepo_oai_record_get", "dbrepo_oai_metadataformats_list")) {
            assertThat(registry)
                    .hasObservationWithNameEqualTo(metric);
        }
        generic_openApiDocs(MetadataEndpoint.class);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-ontology", "update-ontology", "delete-ontology", "execute-semantic-query"})
    public void prometheusOntologyEndpoint_succeeds() {

        /* mock */
        try {
            ontologyEndpoint.findAll();
        } catch (Exception e) {
            /* ignore */
        }
        try {
            ontologyEndpoint.find(ONTOLOGY_1_ID);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            ontologyEndpoint.create(ONTOLOGY_1_CREATE_DTO, USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            ontologyEndpoint.update(ONTOLOGY_1_ID, ONTOLOGY_1_MODIFY_DTO);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            ontologyEndpoint.delete(ONTOLOGY_1_ID);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            ontologyEndpoint.find(ONTOLOGY_1_ID, "thing", null);
        } catch (Exception e) {
            /* ignore */
        }

        /* test */
        for (String metric : List.of("dbrepo_ontologies_findall", "dbrepo_ontologies_find", "dbrepo_ontologies_create", "dbrepo_ontologies_update", "dbrepo_ontologies_delete", "dbrepo_ontologies_entities_find")) {
            assertThat(registry)
                    .hasObservationWithNameEqualTo(metric);
        }
        generic_openApiDocs(OntologyEndpoint.class);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-semantic-concept", "admin"})
    public void prometheusConceptEndpoint_succeeds() {

        /* mock */
        try {
            conceptEndpoint.findAll();
        } catch (Exception e) {
            /* ignore */
        }

        /* test */
        for (String metric : List.of("dbrepo_semantic_concepts_findall")) {
            assertThat(registry)
                    .hasObservationWithNameEqualTo(metric);
        }
        generic_openApiDocs(ConceptEndpoint.class);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-semantic-unit", "admin"})
    public void prometheusUnitEndpoint_succeeds() {

        /* mock */
        try {
            unitEndpoint.findAll();
        } catch (Exception e) {
            /* ignore */
        }

        /* test */
        for (String metric : List.of("dbrepo_semantic_units_findall")) {
            assertThat(registry)
                    .hasObservationWithNameEqualTo(metric);
        }
        generic_openApiDocs(UnitEndpoint.class);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-table", "delete-table",
            "modify-table-column-semantics", "modify-foreign-table-column-semantics", "update-table-statistic",
            "table-semantic-analyse"})
    public void prometheusTableEndpoint_succeeds() {
        final ColumnSemanticsUpdateDto request = ColumnSemanticsUpdateDto.builder()
                .unitUri(UNIT_1_URI)
                .conceptUri(CONCEPT_1_URI)
                .build();

        /* mock */
        try {
            tableEndpoint.list(DATABASE_1_ID, USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            tableEndpoint.create(DATABASE_1_ID, TABLE_3_CREATE_DTO, USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            tableEndpoint.findById(DATABASE_1_ID, TABLE_1_ID, USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            tableEndpoint.delete(DATABASE_1_ID, TABLE_1_ID, USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            tableEndpoint.analyseTable(DATABASE_1_ID, TABLE_1_ID, USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            tableEndpoint.updateStatistic(DATABASE_1_ID, TABLE_1_ID, USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            tableEndpoint.analyseTableColumn(DATABASE_1_ID, TABLE_1_ID, TABLE_1_COLUMNS.get(0).getId(), USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            tableEndpoint.updateColumn(DATABASE_1_ID, TABLE_1_ID, TABLE_1_COLUMNS.get(3).getId(), request, USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }

        /* test */
        for (String metric : List.of("dbrepo_tables_findall", "dbrepo_table_create", "dbrepo_tables_find",
                "dbrepo_table_delete", "dbrepo_statistic_table_update", "dbrepo_semantic_table_analyse",
                "dbrepo_semantic_column_analyse", "dbrepo_semantics_column_save")) {
            assertThat(registry)
                    .hasObservationWithNameEqualTo(metric);
        }
        generic_openApiDocs(TableEndpoint.class);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"find-user", "modify-user-information", "modify-user-theme"})
    public void prometheusUserEndpoint_succeeds() {

        /* mock */
        try {
            userEndpoint.findAll(null);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            userEndpoint.find(USER_1_ID, USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            userEndpoint.modify(USER_1_ID, USER_1_UPDATE_DTO, USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }

        /* test */
        for (String metric : List.of("dbrepo_users_list", "dbrepo_user_find", "dbrepo_user_modify",
                "dbrepo_user_password_modify")) {
            assertThat(registry)
                    .hasObservationWithNameEqualTo(metric);
        }
        generic_openApiDocs(UserEndpoint.class);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-database-view", "delete-database-view"})
    public void prometheusViewEndpoint_succeeds() {

        /* mock */
        try {
            viewEndpoint.findAll(DATABASE_1_ID, USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            viewEndpoint.create(DATABASE_1_ID, VIEW_1_CREATE_DTO, USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            viewEndpoint.find(DATABASE_1_ID, VIEW_1_ID, USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            viewEndpoint.delete(DATABASE_1_ID, VIEW_1_ID, USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }

        /* test */
        for (String metric : List.of("dbrepo_views_findall", "dbrepo_view_create",
                "dbrepo_view_find", "dbrepo_view_delete")) {
            assertThat(registry)
                    .hasObservationWithNameEqualTo(metric);
        }
        generic_openApiDocs(ViewEndpoint.class);
    }

    private static void generic_openApiDocs(Class<?> endpoint) {
        final List<Method> methods = Arrays.stream(endpoint.getMethods())
                .filter(m -> m.getDeclaringClass().equals(endpoint))
                .toList();
        methods.forEach(m -> {
            metrics.put(m.getDeclaredAnnotation(Observed.class).name(), m.getDeclaredAnnotation(Operation.class).summary());
        });
    }

}
