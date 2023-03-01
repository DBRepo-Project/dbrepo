package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.SortType;
import at.tuwien.api.database.query.ImportDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.database.table.TableCsvDto;
import at.tuwien.config.IndexConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.database.table.Table;
import at.tuwien.exception.*;
import at.tuwien.listener.impl.RabbitMqListenerImpl;
import at.tuwien.service.AccessService;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.TableService;
import at.tuwien.service.impl.QueryServiceImpl;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.Principal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class TableDataEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private Channel channel;

    @MockBean
    private RabbitMqListenerImpl rabbitMqListener;

    @MockBean
    private QueryServiceImpl queryService;

    @MockBean
    private DatabaseService databaseService;

    @MockBean
    private IndexConfig indexInitializer;

    @MockBean
    private AccessService accessService;

    @MockBean
    private TableService tableService;

    @Autowired
    private TableDataEndpoint dataEndpoint;

    @Test
    public void import_publicAnonymous_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_import(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, TABLE_8_ID, TABLE_8, null,
                    null, null);
        });
    }

    @Test
    public void import_publicResearcherRead_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_import(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, TABLE_8_ID, TABLE_8, USER_1_USERNAME,
                    DATABASE_3_RESEARCHER_READ_ACCESS, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void import_publicResearcherWriteOwn_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_import(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, TABLE_8_ID, TABLE_8, USER_1_USERNAME,
                    DATABASE_3_RESEARCHER_WRITE_OWN_ACCESS, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void import_publicResearcherWriteAll_succeeds() throws UserNotFoundException, TableNotFoundException,
            NotAllowedException, TableMalformedException, DatabaseConnectionException, QueryMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, ContainerNotFoundException {

        /* test */
        generic_import(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, TABLE_8_ID, TABLE_8, USER_1_USERNAME,
                DATABASE_3_RESEARCHER_WRITE_ALL_ACCESS, USER_1_PRINCIPAL);
    }
    
    @Test
    public void import_publicDeveloperRead_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_import(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, TABLE_8_ID, TABLE_8, USER_2_USERNAME,
                    DATABASE_3_DEVELOPER_READ_ACCESS, USER_2_PRINCIPAL);
        });
    }

    @Test
    public void import_publicDeveloperWriteOwn_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_import(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, TABLE_8_ID, TABLE_8, USER_2_USERNAME,
                    DATABASE_3_DEVELOPER_WRITE_OWN_ACCESS, USER_2_PRINCIPAL);
        });
    }

    @Test
    public void import_publicDeveloperWriteAll_succeeds() throws UserNotFoundException, TableNotFoundException,
            NotAllowedException, TableMalformedException, DatabaseConnectionException, QueryMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, ContainerNotFoundException {

        /* test */
        generic_import(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, TABLE_8_ID, TABLE_8, USER_2_USERNAME,
                DATABASE_3_DEVELOPER_WRITE_ALL_ACCESS, USER_2_PRINCIPAL);
    }

    @Test
    public void import_publicDataStewardRead_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_import(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, TABLE_8_ID, TABLE_8, USER_3_USERNAME,
                    DATABASE_3_DATA_STEWARD_READ_ACCESS, USER_3_PRINCIPAL);
        });
    }

    @Test
    public void import_publicDataStewardWriteOwn_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_import(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, TABLE_8_ID, TABLE_8, USER_3_USERNAME,
                    DATABASE_3_DATA_STEWARD_WRITE_OWN_ACCESS, USER_3_PRINCIPAL);
        });
    }

    @Test
    public void import_publicDataStewardWriteAll_succeeds() throws UserNotFoundException, TableNotFoundException,
            NotAllowedException, TableMalformedException, DatabaseConnectionException, QueryMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, ContainerNotFoundException {

        /* test */
        generic_import(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, TABLE_8_ID, TABLE_8, USER_3_USERNAME,
                DATABASE_3_DATA_STEWARD_WRITE_ALL_ACCESS, USER_3_PRINCIPAL);
    }

    @Test
    public void insert_publicAnonymous_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_insert(CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3, TABLE_8, USER_1_USERNAME,
                    null, TABLE_8_CSV_DTO, null);
        });
    }

    @Test
    public void insert_publicResearcherRead_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_insert(CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3, TABLE_8, USER_1_USERNAME,
                    DATABASE_3_RESEARCHER_READ_ACCESS, TABLE_8_CSV_DTO, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void insert_publicResearcherWriteOwn_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_insert(CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3, TABLE_8, USER_1_USERNAME,
                    DATABASE_3_RESEARCHER_WRITE_OWN_ACCESS, TABLE_1_CSV_DTO, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void insert_publicResearcher_WriteAll_succeeds() throws UserNotFoundException, TableNotFoundException, NotAllowedException,
            TableMalformedException, DatabaseConnectionException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException {

        /* test */
        generic_insert(CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3, TABLE_8, USER_1_USERNAME,
                DATABASE_3_RESEARCHER_WRITE_ALL_ACCESS, TABLE_1_CSV_DTO, USER_1_PRINCIPAL);
    }

    @Test
    public void insert_publicDeveloperRead_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_insert(CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3, TABLE_8, USER_2_USERNAME,
                    DATABASE_3_DEVELOPER_READ_ACCESS, TABLE_8_CSV_DTO, USER_2_PRINCIPAL);
        });
    }

    @Test
    public void insert_publicDeveloperWriteOwn_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_insert(CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3, TABLE_8, USER_2_USERNAME,
                    DATABASE_3_DEVELOPER_WRITE_OWN_ACCESS, TABLE_8_CSV_DTO, USER_2_PRINCIPAL);
        });
    }

    @Test
    public void insert_publicDeveloper_WriteAll_succeeds() throws UserNotFoundException, TableNotFoundException, NotAllowedException,
            TableMalformedException, DatabaseConnectionException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException {

        /* test */
        generic_insert(CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3, TABLE_8, USER_2_USERNAME,
                DATABASE_3_DEVELOPER_WRITE_ALL_ACCESS, TABLE_8_CSV_DTO, USER_2_PRINCIPAL);
    }

    @Test
    public void insert_publicDataStewardRead_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_insert(CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3, TABLE_8, USER_3_USERNAME,
                    DATABASE_3_DATA_STEWARD_READ_ACCESS, TABLE_8_CSV_DTO, USER_3_PRINCIPAL);
        });
    }

    @Test
    public void insert_publicDataStewardWriteOwn_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_insert(CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3, TABLE_8, USER_3_USERNAME,
                    DATABASE_3_DATA_STEWARD_WRITE_OWN_ACCESS, TABLE_8_CSV_DTO, USER_3_PRINCIPAL);
        });
    }

    @Test
    public void insert_publicDataSteward_WriteAll_succeeds() throws UserNotFoundException, TableNotFoundException, NotAllowedException,
            TableMalformedException, DatabaseConnectionException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException {

        /* test */
        generic_insert(CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3, TABLE_8, USER_3_USERNAME,
                DATABASE_3_DATA_STEWARD_WRITE_ALL_ACCESS, TABLE_8_CSV_DTO, USER_3_PRINCIPAL);
    }

    @Test
    public void getAll_publicAnonymous_succeeds() throws TableNotFoundException, DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, PaginationException, ContainerNotFoundException,
            QueryStoreException, NotAllowedException, QueryMalformedException, SortException, UserNotFoundException {

        /* test */
        generic_getAll(CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3, TABLE_8, null,
                null, null, null, null, null, null, null);
    }

    @Test
    public void getAll_publicResearcherRead_succeeds() throws TableNotFoundException, DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, PaginationException, ContainerNotFoundException,
            QueryStoreException, NotAllowedException, QueryMalformedException, SortException, UserNotFoundException {

        /* test */
        generic_getAll(CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3, TABLE_8, USER_1_USERNAME,
                DATABASE_3_RESEARCHER_READ_ACCESS, USER_1_PRINCIPAL, null, null, null, null, null);
    }

    @Test
    public void getAll_publicResearcherWriteOwn_succeeds() throws TableNotFoundException, DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, PaginationException, ContainerNotFoundException,
            QueryStoreException, NotAllowedException, QueryMalformedException, SortException, UserNotFoundException {

        /* test */
        generic_getAll(CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3, TABLE_8, USER_1_USERNAME,
                DATABASE_3_RESEARCHER_WRITE_OWN_ACCESS, USER_1_PRINCIPAL, null, null, null, null, null);
    }

    @Test
    public void getAll_publicResearcherWriteAll_succeeds() throws TableNotFoundException, DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, PaginationException, ContainerNotFoundException,
            QueryStoreException, NotAllowedException, QueryMalformedException, SortException, UserNotFoundException {

        /* test */
        generic_getAll(CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3, TABLE_8, USER_1_USERNAME,
                DATABASE_3_RESEARCHER_WRITE_ALL_ACCESS, USER_1_PRINCIPAL, null, null, null, null, null);
    }

    @Test
    public void getAll_publicDeveloperRead_succeeds() throws TableNotFoundException, DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, PaginationException, ContainerNotFoundException,
            QueryStoreException, NotAllowedException, QueryMalformedException, SortException, UserNotFoundException {

        /* test */
        generic_getAll(CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3, TABLE_8, USER_2_USERNAME,
                DATABASE_3_DEVELOPER_READ_ACCESS, USER_2_PRINCIPAL, null, null, null, null, null);
    }

    @Test
    public void getAll_publicDeveloperWriteOwn_succeeds() throws TableNotFoundException, DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, PaginationException, ContainerNotFoundException,
            QueryStoreException, NotAllowedException, QueryMalformedException, SortException, UserNotFoundException {

        /* test */
        generic_getAll(CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3, TABLE_8, USER_2_USERNAME,
                DATABASE_3_DEVELOPER_WRITE_OWN_ACCESS, USER_2_PRINCIPAL, null, null, null, null, null);
    }

    @Test
    public void getAll_publicDeveloperWriteAll_succeeds() throws TableNotFoundException, DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, PaginationException, ContainerNotFoundException,
            QueryStoreException, NotAllowedException, QueryMalformedException, SortException, UserNotFoundException {

        /* test */
        generic_getAll(CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3, TABLE_8, USER_2_USERNAME,
                DATABASE_3_DEVELOPER_WRITE_ALL_ACCESS, USER_2_PRINCIPAL, null, null, null, null, null);
    }

    @Test
    public void getAll_publicDataStewardRead_succeeds() throws TableNotFoundException, DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, PaginationException, ContainerNotFoundException,
            QueryStoreException, NotAllowedException, QueryMalformedException, SortException, UserNotFoundException {

        /* test */
        generic_getAll(CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3, TABLE_8, USER_3_USERNAME,
                DATABASE_3_DATA_STEWARD_READ_ACCESS, USER_3_PRINCIPAL, null, null, null, null, null);
    }

    @Test
    public void getAll_publicDataStewardWriteOwn_succeeds() throws TableNotFoundException, DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, PaginationException, ContainerNotFoundException,
            QueryStoreException, NotAllowedException, QueryMalformedException, SortException, UserNotFoundException {

        /* test */
        generic_getAll(CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3, TABLE_8, USER_3_USERNAME,
                DATABASE_3_DATA_STEWARD_WRITE_OWN_ACCESS, USER_3_PRINCIPAL, null, null, null, null, null);
    }

    @Test
    public void getAll_publicDataStewardWriteAll_succeeds() throws TableNotFoundException, DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, PaginationException, ContainerNotFoundException,
            QueryStoreException, NotAllowedException, QueryMalformedException, SortException, UserNotFoundException {

        /* test */
        generic_getAll(CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3, TABLE_8, USER_3_USERNAME,
                DATABASE_3_DATA_STEWARD_WRITE_ALL_ACCESS, USER_3_PRINCIPAL, null, null, null, null, null);
    }

    @Test
    public void getAll_publicAnonymousPageNull_fails() {
        final Long page = null;
        final Long size = 1L;

        /* test */
        assertThrows(PaginationException.class, () -> {
            generic_getAll(CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3, TABLE_8, null,
                    null, null, null, page, size, null, null);
        });
    }

    @Test
    public void getAll_publicAnonymousSizeNull_fails() {
        final Long page = 1L;
        final Long size = null;

        /* test */
        assertThrows(PaginationException.class, () -> {
            generic_getAll(CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3, TABLE_8, null,
                    null, null, null, page, size, null, null);
        });
    }

    @Test
    public void getAll_publicAnonymousPageNegative_fails() {
        final Long page = -1L;
        final Long size = 1L;

        /* test */
        assertThrows(PaginationException.class, () -> {
            generic_getAll(CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3, TABLE_8, null,
                    null, null, null, page, size, null, null);
        });
    }

    @Test
    public void getAll_publicAnonymousSizeZero_fails() {
        final Long page = 0L;
        final Long size = 0L;

        /* test */
        assertThrows(PaginationException.class, () -> {
            generic_getAll(CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3, TABLE_8, null,
                    null, null, null, page, size, null, null);
        });
    }

    @Test
    public void getAll_publicAnonymousSizeNegative_fails() {
        final Long page = 0L;
        final Long size = -1L;

        /* test */
        assertThrows(PaginationException.class, () -> {
            generic_getAll(CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3, TABLE_8, null,
                    null, null, null, page, size, null, null);
        });
    }

    /* ################################################################################################### */
    /* ## PRIVATE DATABASES                                                                             ## */
    /* ################################################################################################### */

    @Test
    public void import_privateAnonymous_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_import(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, TABLE_4_ID, TABLE_4, null,
                    null, null);
        });
    }

    @Test
    public void import_privateResearcherRead_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_import(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, TABLE_4_ID, TABLE_4, USER_1_USERNAME,
                    DATABASE_2_RESEARCHER_READ_ACCESS, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void import_privateResearcherWriteOwn_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_import(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, TABLE_4_ID, TABLE_4, USER_1_USERNAME,
                    DATABASE_2_RESEARCHER_WRITE_OWN_ACCESS, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void import_privateResearcherWriteAll_succeeds() throws UserNotFoundException, TableNotFoundException,
            NotAllowedException, TableMalformedException, DatabaseConnectionException, QueryMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, ContainerNotFoundException {

        /* test */
        generic_import(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, TABLE_4_ID, TABLE_4, USER_1_USERNAME,
                DATABASE_2_RESEARCHER_WRITE_ALL_ACCESS, USER_1_PRINCIPAL);
    }

    @Test
    public void import_privateDeveloperRead_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_import(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, TABLE_4_ID, TABLE_4, USER_2_USERNAME,
                    DATABASE_2_DEVELOPER_READ_ACCESS, USER_2_PRINCIPAL);
        });
    }

    @Test
    public void import_privateDeveloperWriteOwn_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_import(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, TABLE_4_ID, TABLE_4, USER_2_USERNAME,
                    DATABASE_2_DEVELOPER_WRITE_OWN_ACCESS, USER_2_PRINCIPAL);
        });
    }

    @Test
    public void import_privateDeveloperWriteAll_succeeds() throws UserNotFoundException, TableNotFoundException,
            NotAllowedException, TableMalformedException, DatabaseConnectionException, QueryMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, ContainerNotFoundException {

        /* test */
        generic_import(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, TABLE_4_ID, TABLE_4, USER_2_USERNAME,
                DATABASE_2_DEVELOPER_WRITE_ALL_ACCESS, USER_2_PRINCIPAL);
    }

    @Test
    public void import_privateDataStewardRead_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_import(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, TABLE_4_ID, TABLE_4, USER_3_USERNAME,
                    DATABASE_2_DATA_STEWARD_READ_ACCESS, USER_3_PRINCIPAL);
        });
    }

    @Test
    public void import_privateDataStewardWriteOwn_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_import(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, TABLE_4_ID, TABLE_4, USER_3_USERNAME,
                    DATABASE_2_DATA_STEWARD_WRITE_OWN_ACCESS, USER_3_PRINCIPAL);
        });
    }

    @Test
    public void import_privateDataStewardWriteAll_succeeds() throws UserNotFoundException, TableNotFoundException,
            NotAllowedException, TableMalformedException, DatabaseConnectionException, QueryMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, ContainerNotFoundException {

        /* test */
        generic_import(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, TABLE_4_ID, TABLE_4, USER_3_USERNAME,
                DATABASE_2_DATA_STEWARD_WRITE_ALL_ACCESS, USER_3_PRINCIPAL);
    }

    @Test
    public void insert_privateAnonymous_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_insert(CONTAINER_2_ID, DATABASE_2_ID, TABLE_4_ID, DATABASE_2, TABLE_4, USER_1_USERNAME,
                    null, TABLE_4_CSV_DTO, null);
        });
    }

    @Test
    public void insert_privateResearcherRead_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_insert(CONTAINER_2_ID, DATABASE_2_ID, TABLE_4_ID, DATABASE_2, TABLE_4, USER_1_USERNAME,
                    DATABASE_2_RESEARCHER_READ_ACCESS, TABLE_4_CSV_DTO, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void insert_privateResearcherWriteOwn_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_insert(CONTAINER_2_ID, DATABASE_2_ID, TABLE_4_ID, DATABASE_2, TABLE_4, USER_1_USERNAME,
                    DATABASE_2_RESEARCHER_WRITE_OWN_ACCESS, TABLE_1_CSV_DTO, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void insert_privateResearcher_WriteAll_succeeds() throws UserNotFoundException, TableNotFoundException, NotAllowedException,
            TableMalformedException, DatabaseConnectionException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException {

        /* test */
        generic_insert(CONTAINER_2_ID, DATABASE_2_ID, TABLE_4_ID, DATABASE_2, TABLE_4, USER_1_USERNAME,
                DATABASE_2_RESEARCHER_WRITE_ALL_ACCESS, TABLE_1_CSV_DTO, USER_1_PRINCIPAL);
    }

    @Test
    public void insert_privateDeveloperRead_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_insert(CONTAINER_2_ID, DATABASE_2_ID, TABLE_4_ID, DATABASE_2, TABLE_4, USER_2_USERNAME,
                    DATABASE_2_DEVELOPER_READ_ACCESS, TABLE_4_CSV_DTO, USER_2_PRINCIPAL);
        });
    }

    @Test
    public void insert_privateDeveloperWriteOwn_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_insert(CONTAINER_2_ID, DATABASE_2_ID, TABLE_4_ID, DATABASE_2, TABLE_4, USER_2_USERNAME,
                    DATABASE_2_DEVELOPER_WRITE_OWN_ACCESS, TABLE_4_CSV_DTO, USER_2_PRINCIPAL);
        });
    }

    @Test
    public void insert_privateDeveloper_WriteAll_succeeds() throws UserNotFoundException, TableNotFoundException, NotAllowedException,
            TableMalformedException, DatabaseConnectionException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException {

        /* test */
        generic_insert(CONTAINER_2_ID, DATABASE_2_ID, TABLE_4_ID, DATABASE_2, TABLE_4, USER_2_USERNAME,
                DATABASE_2_DEVELOPER_WRITE_ALL_ACCESS, TABLE_4_CSV_DTO, USER_2_PRINCIPAL);
    }

    @Test
    public void insert_privateDataStewardRead_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_insert(CONTAINER_2_ID, DATABASE_2_ID, TABLE_4_ID, DATABASE_2, TABLE_4, USER_3_USERNAME,
                    DATABASE_2_DATA_STEWARD_READ_ACCESS, TABLE_4_CSV_DTO, USER_3_PRINCIPAL);
        });
    }

    @Test
    public void insert_privateDataStewardWriteOwn_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_insert(CONTAINER_2_ID, DATABASE_2_ID, TABLE_4_ID, DATABASE_2, TABLE_4, USER_3_USERNAME,
                    DATABASE_2_DATA_STEWARD_WRITE_OWN_ACCESS, TABLE_4_CSV_DTO, USER_3_PRINCIPAL);
        });
    }

    @Test
    public void insert_privateDataSteward_WriteAll_succeeds() throws UserNotFoundException, TableNotFoundException, NotAllowedException,
            TableMalformedException, DatabaseConnectionException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException {

        /* test */
        generic_insert(CONTAINER_2_ID, DATABASE_2_ID, TABLE_4_ID, DATABASE_2, TABLE_4, USER_3_USERNAME,
                DATABASE_2_DATA_STEWARD_WRITE_ALL_ACCESS, TABLE_4_CSV_DTO, USER_3_PRINCIPAL);
    }

    @Test
    public void getAll_privateAnonymous_succeeds() throws TableNotFoundException, DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, PaginationException, ContainerNotFoundException,
            QueryStoreException, NotAllowedException, QueryMalformedException, SortException, UserNotFoundException {

        /* test */
        generic_getAll(CONTAINER_2_ID, DATABASE_2_ID, TABLE_4_ID, DATABASE_2, TABLE_4, null,
                null, null, null, null, null, null, null);
    }

    @Test
    public void getAll_privateResearcherRead_succeeds() throws TableNotFoundException, DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, PaginationException, ContainerNotFoundException,
            QueryStoreException, NotAllowedException, QueryMalformedException, SortException, UserNotFoundException {

        /* test */
        generic_getAll(CONTAINER_2_ID, DATABASE_2_ID, TABLE_4_ID, DATABASE_2, TABLE_4, USER_1_USERNAME,
                DATABASE_2_RESEARCHER_READ_ACCESS, USER_1_PRINCIPAL, null, null, null, null, null);
    }

    @Test
    public void getAll_privateResearcherWriteOwn_succeeds() throws TableNotFoundException, DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, PaginationException, ContainerNotFoundException,
            QueryStoreException, NotAllowedException, QueryMalformedException, SortException, UserNotFoundException {

        /* test */
        generic_getAll(CONTAINER_2_ID, DATABASE_2_ID, TABLE_4_ID, DATABASE_2, TABLE_4, USER_1_USERNAME,
                DATABASE_2_RESEARCHER_WRITE_OWN_ACCESS, USER_1_PRINCIPAL, null, null, null, null, null);
    }

    @Test
    public void getAll_privateResearcherWriteAll_succeeds() throws TableNotFoundException, DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, PaginationException, ContainerNotFoundException,
            QueryStoreException, NotAllowedException, QueryMalformedException, SortException, UserNotFoundException {

        /* test */
        generic_getAll(CONTAINER_2_ID, DATABASE_2_ID, TABLE_4_ID, DATABASE_2, TABLE_4, USER_1_USERNAME,
                DATABASE_2_RESEARCHER_WRITE_ALL_ACCESS, USER_1_PRINCIPAL, null, null, null, null, null);
    }

    @Test
    public void getAll_privateDeveloperRead_succeeds() throws TableNotFoundException, DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, PaginationException, ContainerNotFoundException,
            QueryStoreException, NotAllowedException, QueryMalformedException, SortException, UserNotFoundException {

        /* test */
        generic_getAll(CONTAINER_2_ID, DATABASE_2_ID, TABLE_4_ID, DATABASE_2, TABLE_4, USER_2_USERNAME,
                DATABASE_2_DEVELOPER_READ_ACCESS, USER_2_PRINCIPAL, null, null, null, null, null);
    }

    @Test
    public void getAll_privateDeveloperWriteOwn_succeeds() throws TableNotFoundException, DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, PaginationException, ContainerNotFoundException,
            QueryStoreException, NotAllowedException, QueryMalformedException, SortException, UserNotFoundException {

        /* test */
        generic_getAll(CONTAINER_2_ID, DATABASE_2_ID, TABLE_4_ID, DATABASE_2, TABLE_4, USER_2_USERNAME,
                DATABASE_2_DEVELOPER_WRITE_OWN_ACCESS, USER_2_PRINCIPAL, null, null, null, null, null);
    }

    @Test
    public void getAll_privateDeveloperWriteAll_succeeds() throws TableNotFoundException, DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, PaginationException, ContainerNotFoundException,
            QueryStoreException, NotAllowedException, QueryMalformedException, SortException, UserNotFoundException {

        /* test */
        generic_getAll(CONTAINER_2_ID, DATABASE_2_ID, TABLE_4_ID, DATABASE_2, TABLE_4, USER_2_USERNAME,
                DATABASE_2_DEVELOPER_WRITE_ALL_ACCESS, USER_2_PRINCIPAL, null, null, null, null, null);
    }

    @Test
    public void getAll_privateDataStewardRead_succeeds() throws TableNotFoundException, DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, PaginationException, ContainerNotFoundException,
            QueryStoreException, NotAllowedException, QueryMalformedException, SortException, UserNotFoundException {

        /* test */
        generic_getAll(CONTAINER_2_ID, DATABASE_2_ID, TABLE_4_ID, DATABASE_2, TABLE_4, USER_3_USERNAME,
                DATABASE_2_DATA_STEWARD_READ_ACCESS, USER_3_PRINCIPAL, null, null, null, null, null);
    }

    @Test
    public void getAll_privateDataStewardWriteOwn_succeeds() throws TableNotFoundException, DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, PaginationException, ContainerNotFoundException,
            QueryStoreException, NotAllowedException, QueryMalformedException, SortException, UserNotFoundException {

        /* test */
        generic_getAll(CONTAINER_2_ID, DATABASE_2_ID, TABLE_4_ID, DATABASE_2, TABLE_4, USER_3_USERNAME,
                DATABASE_2_DATA_STEWARD_WRITE_OWN_ACCESS, USER_3_PRINCIPAL, null, null, null, null, null);
    }

    @Test
    public void getAll_privateDataStewardWriteAll_succeeds() throws TableNotFoundException, DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, PaginationException, ContainerNotFoundException,
            QueryStoreException, NotAllowedException, QueryMalformedException, SortException, UserNotFoundException {

        /* test */
        generic_getAll(CONTAINER_2_ID, DATABASE_2_ID, TABLE_4_ID, DATABASE_2, TABLE_4, USER_3_USERNAME,
                DATABASE_2_DATA_STEWARD_WRITE_ALL_ACCESS, USER_3_PRINCIPAL, null, null, null, null, null);
    }

    @Test
    public void getAll_privateAnonymousPageNull_fails() {
        final Long page = null;
        final Long size = 1L;

        /* test */
        assertThrows(PaginationException.class, () -> {
            generic_getAll(CONTAINER_2_ID, DATABASE_2_ID, TABLE_4_ID, DATABASE_2, TABLE_4, null,
                    null, null, null, page, size, null, null);
        });
    }

    @Test
    public void getAll_privateAnonymousSizeNull_fails() {
        final Long page = 1L;
        final Long size = null;

        /* test */
        assertThrows(PaginationException.class, () -> {
            generic_getAll(CONTAINER_2_ID, DATABASE_2_ID, TABLE_4_ID, DATABASE_2, TABLE_4, null,
                    null, null, null, page, size, null, null);
        });
    }

    @Test
    public void getAll_privateAnonymousPageNegative_fails() {
        final Long page = -1L;
        final Long size = 1L;

        /* test */
        assertThrows(PaginationException.class, () -> {
            generic_getAll(CONTAINER_2_ID, DATABASE_2_ID, TABLE_4_ID, DATABASE_2, TABLE_4, null,
                    null, null, null, page, size, null, null);
        });
    }

    @Test
    public void getAll_privateAnonymousSizeZero_fails() {
        final Long page = 0L;
        final Long size = 0L;

        /* test */
        assertThrows(PaginationException.class, () -> {
            generic_getAll(CONTAINER_2_ID, DATABASE_2_ID, TABLE_4_ID, DATABASE_2, TABLE_4, null,
                    null, null, null, page, size, null, null);
        });
    }

    @Test
    public void getAll_privateAnonymousSizeNegative_fails() {
        final Long page = 0L;
        final Long size = -1L;

        /* test */
        assertThrows(PaginationException.class, () -> {
            generic_getAll(CONTAINER_2_ID, DATABASE_2_ID, TABLE_4_ID, DATABASE_2, TABLE_4, null,
                    null, null, null, page, size, null, null);
        });
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    public void generic_import(Long containerId, Long databaseId, Database database, Long tableId, Table table,
                               String username, DatabaseAccess access, Principal principal)
            throws DatabaseNotFoundException, TableNotFoundException, NotAllowedException, UserNotFoundException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, ImageNotSupportedException,
            ContainerNotFoundException {
        final ImportDto request = ImportDto.builder()
                .location("test:csv/csv_01.csv")
                .build();

        /* mock */
        when(databaseService.find(containerId, databaseId))
                .thenReturn(database);
        when(tableService.find(containerId, databaseId, tableId))
                .thenReturn(table);
        when(accessService.find(databaseId, username))
                .thenReturn(access);

        /* test */
        final ResponseEntity<?> response = dataEndpoint.importCsv(containerId, databaseId, tableId, request,
                principal);
        assertNotNull(response);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    public void generic_insert(Long containerId, Long databaseId, Long tableId, Database database, Table table,
                               String username, DatabaseAccess access, TableCsvDto data, Principal principal)
            throws DatabaseNotFoundException, TableNotFoundException, NotAllowedException, UserNotFoundException,
            TableMalformedException, DatabaseConnectionException, ImageNotSupportedException,
            ContainerNotFoundException {

        /* mock */
        when(databaseService.find(containerId, databaseId))
                .thenReturn(database);
        when(tableService.find(containerId, databaseId, tableId))
                .thenReturn(table);
        when(accessService.find(databaseId, username))
                .thenReturn(access);

        /* test */
        final ResponseEntity<?> response = dataEndpoint.insert(containerId, databaseId, tableId, data, principal);
        assertNotNull(response);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    public void generic_getAll(Long containerId, Long databaseId, Long tableId, Database database, Table table,
                               String username, DatabaseAccess access, Principal principal, Instant timestamp,
                               Long page, Long size, SortType sortDirection, String sortColumn)
            throws UserNotFoundException, TableMalformedException, NotAllowedException, PaginationException,
            TableNotFoundException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException, QueryStoreException, SortException {

        /* mock */
        when(databaseService.find(containerId, databaseId))
                .thenReturn(database);
        when(tableService.find(containerId, databaseId, tableId))
                .thenReturn(table);
        when(accessService.find(databaseId, username))
                .thenReturn(access);
        when(queryService.findAll(containerId, databaseId, tableId, timestamp, page, size, principal))
                .thenReturn(QUERY_1_RESULT_DTO);

        /* test */
        final ResponseEntity<QueryResultDto> response = dataEndpoint.data(containerId, databaseId, tableId, principal, timestamp, page, size, sortDirection, sortColumn);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(QUERY_1_RESULT_ID, response.getBody().getId());
        assertEquals(QUERY_1_RESULT_NUMBER, response.getBody().getResultNumber());
        assertEquals(QUERY_1_RESULT_RESULT, response.getBody().getResult());
    }

}
