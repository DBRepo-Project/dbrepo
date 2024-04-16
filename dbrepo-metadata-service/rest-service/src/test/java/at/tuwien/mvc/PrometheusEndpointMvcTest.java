package at.tuwien.mvc;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.container.ContainerCreateRequestDto;
import at.tuwien.api.database.*;
import at.tuwien.api.database.query.ExecuteStatementDto;
import at.tuwien.api.database.query.ImportDto;
import at.tuwien.api.database.query.QueryPersistDto;
import at.tuwien.api.database.table.TableCsvDeleteDto;
import at.tuwien.api.database.table.TableCsvDto;
import at.tuwien.api.database.table.TableCsvUpdateDto;
import at.tuwien.api.database.table.columns.concepts.ColumnSemanticsUpdateDto;
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
import org.springframework.security.test.context.support.WithAnonymousUser;
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

    @Autowired
    private MetadataEndpoint metadataEndpoint;

    @Autowired
    private OntologyEndpoint ontologyEndpoint;

    @Autowired
    private PersistenceEndpoint persistenceEndpoint;

    @Autowired
    private QueryEndpoint queryEndpoint;

    @Autowired
    private SemanticsEndpoint semanticsEndpoint;

    @Autowired
    private StoreEndpoint storeEndpoint;

    @Autowired
    private TableColumnEndpoint tableColumnEndpoint;

    @Autowired
    private TableDataEndpoint tableDataEndpoint;

    @Autowired
    private TableEndpoint tableEndpoint;

    @Autowired
    private TableHistoryEndpoint tableHistoryEndpoint;

    @Autowired
    private UserEndpoint userEndpoint;

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
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-database", "modify-database-visibility", "modify-database-owner", "delete-database", "modify-database-image"})
    public void prometheusDatabaseEndpoint_succeeds() {

        /* mock */
        try {
            databaseEndpoint.list(USER_1_PRINCIPAL, null);
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

        /* test */
        for (String metric : List.of("dbr_database_findall", "dbr_database_create", "dbr_database_visibility", "dbr_database_transfer", "dbr_database_find", "dbr_database_image")) {
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
        for (String metric : List.of("dbr_identifier_create", "dbr_identifier_retrieve")) {
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
            maintenanceEndpoint.list("");
        } catch (Exception e) {
            /* ignore */
        }
        try {
            maintenanceEndpoint.find(BANNER_MESSAGE_1_ID);
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
        for (String metric : List.of("dbr_maintenance_findall", "dbr_maintenance_find", "dbr_maintenance_create", "dbr_maintenance_update", "dbr_maintenance_delete")) {
            assertThat(registry)
                    .hasObservationWithNameEqualTo(metric);
        }
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
        for (String metric : List.of("dbr_oai_identify", "dbr_oai_identifiers_list", "dbr_oai_record_get", "dbr_oai_metadataformats_list")) {
            assertThat(registry)
                    .hasObservationWithNameEqualTo(metric);
        }
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
            ontologyEndpoint.update(ONTOLOGY_1_ID, ONTOLOGY_1_MODIFY_DTO, USER_1_PRINCIPAL);
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
        for (String metric : List.of("dbr_ontologies_findall", "dbr_ontologies_find", "dbr_ontologies_create", "dbr_ontologies_update", "dbr_ontologies_delete", "dbr_ontologies_entities_find")) {
            assertThat(registry)
                    .hasObservationWithNameEqualTo(metric);
        }
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"modify-identifier-metadata", "delete-identifier"})
    public void prometheusPersistenceEndpoint_succeeds() {

        /* mock */
        try {
            persistenceEndpoint.find(IDENTIFIER_1_ID, null, USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            persistenceEndpoint.delete(IDENTIFIER_1_ID);
        } catch (Exception e) {
            /* ignore */
        }

        /* test */
        for (String metric : List.of("dbr_pid_find", "dbr_pid_delete")) {
            assertThat(registry)
                    .hasObservationWithNameEqualTo(metric);
        }
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"execute-query"})
    public void prometheusQueryEndpoint_succeeds() {

        /* mock */
        try {
            queryEndpoint.execute(DATABASE_1_ID, ExecuteStatementDto.builder().statement("SELECT 1").build(), null, null, USER_1_PRINCIPAL, null, null);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            queryEndpoint.reExecute(DATABASE_1_ID, QUERY_1_ID, USER_1_PRINCIPAL, null, null, null, null, null);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            queryEndpoint.export(DATABASE_1_ID, QUERY_1_ID, null, USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }

        /* test */
        for (String metric : List.of("dbr_query_execute", "dbr_query_reexecute", "dbr_query_export")) {
            assertThat(registry)
                    .hasObservationWithNameEqualTo(metric);
        }
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-semantic-concept", "create-semantic-unit", "table-semantic-analyse"})
    public void prometheusSemanticsEndpoint_succeeds() {

        /* mock */
        try {
            semanticsEndpoint.findAllConcepts();
        } catch (Exception e) {
            /* ignore */
        }
        try {
            semanticsEndpoint.findAllUnits();
        } catch (Exception e) {
            /* ignore */
        }
        try {
            semanticsEndpoint.analyseTable(DATABASE_1_ID, TABLE_1_ID);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            semanticsEndpoint.analyseTableColumn(DATABASE_1_ID, TABLE_1_ID, TABLE_1_COLUMNS.get(0).getId());
        } catch (Exception e) {
            /* ignore */
        }

        /* test */
        for (String metric : List.of("dbr_semantic_concepts_findall", "dbr_semantic_units_findall", "dbr_semantic_table_analyse", "dbr_semantic_column_analyse")) {
            assertThat(registry)
                    .hasObservationWithNameEqualTo(metric);
        }
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"persist-query"})
    public void prometheusStoreEndpoint_succeeds() {

        /* mock */
        try {
            storeEndpoint.findAll(DATABASE_1_ID, true, USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            storeEndpoint.find(DATABASE_1_ID, QUERY_1_ID, USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            storeEndpoint.persist(DATABASE_1_ID, QUERY_1_ID, QueryPersistDto.builder().persist(true).build(), USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }

        /* test */
        for (String metric : List.of("dbr_queries_findall", "dbr_queries_find", "dbr_query_persist")) {
            assertThat(registry)
                    .hasObservationWithNameEqualTo(metric);
        }
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"modify-table-column-semantics", "modify-foreign-table-column-semantics"})
    public void prometheusTableColumnEndpoint_succeeds() {
        final ColumnSemanticsUpdateDto request = ColumnSemanticsUpdateDto.builder()
                .unitUri(UNIT_MILLIMETRE_URI)
                .conceptUri(COLUMN_CONCEPT_PRECIPITATION_URI)
                .build();

        /* mock */
        try {
            tableColumnEndpoint.update(DATABASE_1_ID, TABLE_1_ID, TABLE_1_COLUMNS.get(3).getId(), request, USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }

        /* test */
        assertThat(registry)
                .hasObservationWithNameEqualTo("dbr_semantics_column_save");
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"insert-table-data", "delete-table-data"})
    public void prometheusTableDataEndpoint_succeeds() {

        /* mock */
        try {
            tableDataEndpoint.insert(DATABASE_1_ID, TABLE_1_ID, TableCsvDto.builder().build(), USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            tableDataEndpoint.update(DATABASE_1_ID, TABLE_1_ID, TableCsvUpdateDto.builder().build(), USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            tableDataEndpoint.delete(DATABASE_1_ID, TABLE_1_ID, TableCsvDeleteDto.builder().build(), USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            tableDataEndpoint.importCsv(DATABASE_1_ID, TABLE_1_ID, ImportDto.builder().build(), USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            tableDataEndpoint.getAll(DATABASE_1_ID, TABLE_1_ID, USER_1_PRINCIPAL, null, null, null, null, null, null);
        } catch (Exception e) {
            /* ignore */
        }

        /* test */
        for (String metric : List.of("dbr_table_data_insert", "dbr_table_data_update", "dbr_table_data_delete", "dbr_table_data_import", "dbr_table_data_findall")) {
            assertThat(registry)
                    .hasObservationWithNameEqualTo(metric);
        }
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-table", "delete-table"})
    public void prometheusTableEndpoint_succeeds() {

        /* mock */
        try {
            tableEndpoint.list(DATABASE_1_ID, USER_1_PRINCIPAL, null);
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

        /* test */
        for (String metric : List.of("dbr_tables_findall", "dbr_table_create", "dbr_tables_find", "dbr_table_delete")) {
            assertThat(registry)
                    .hasObservationWithNameEqualTo(metric);
        }
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void prometheusTableHistoryEndpoint_succeeds() {

        /* mock */
        try {
            tableHistoryEndpoint.getAll(DATABASE_1_ID, TABLE_1_ID, USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }

        /* test */
        assertThat(registry)
                .hasObservationWithNameEqualTo("dbr_table_history_findall");
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"find-user", "modify-user-information", "modify-user-theme"})
    public void prometheusUserEndpoint_succeeds() {

        /* mock */
        try {
            userEndpoint.findAll();
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
        try {
            userEndpoint.theme(USER_1_ID, USER_1_THEME_SET_DTO, USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }
        try {
            userEndpoint.password(USER_1_ID, USER_1_PASSWORD_DTO, USER_1_PRINCIPAL);
        } catch (Exception e) {
            /* ignore */
        }

        /* test */
        for (String metric : List.of("dbr_users_findall", "dbr_user_find", "dbr_user_modify", "dbr_user_theme_modify", "dbr_user_password_modify")) {
            assertThat(registry)
                    .hasObservationWithNameEqualTo(metric);
        }
    }

    @Test
    @WithAnonymousUser
    public void prometheusUserEndpoint2_succeeds() {

        /* mock */
        try {
            userEndpoint.create(USER_1_SIGNUP_REQUEST_DTO);
        } catch (Exception e) {
            /* ignore */
        }

        /* test */
        assertThat(registry)
                .hasObservationWithNameEqualTo("dbr_user_create");
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
        try {
            viewEndpoint.data(DATABASE_1_ID, VIEW_1_ID, USER_1_PRINCIPAL, null, null, null);
        } catch (Exception e) {
            /* ignore */
        }

        /* test */
        for (String metric : List.of("dbr_views_findall", "dbr_view_create", "dbr_view_find", "dbr_view_delete", "dbr_view_data_findall")) {
            assertThat(registry)
                    .hasObservationWithNameEqualTo(metric);
        }
    }

}
