package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.ExportResource;
import at.tuwien.SortType;
import at.tuwien.api.database.query.ExecuteStatementDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.config.IndexConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.exception.*;
import at.tuwien.listener.impl.RabbitMqListenerImpl;
import at.tuwien.repository.jpa.ContainerRepository;
import at.tuwien.repository.jpa.DatabaseAccessRepository;
import at.tuwien.repository.jpa.DatabaseRepository;
import at.tuwien.repository.jpa.ImageRepository;
import at.tuwien.service.QueryService;
import at.tuwien.service.StoreService;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.management.Query;
import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class QueryEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private Channel channel;

    @MockBean
    private IndexConfig indexInitializer;

    @MockBean
    private RabbitMqListenerImpl rabbitMqListener;

    @MockBean
    private DatabaseAccessRepository databaseAccessRepository;

    @MockBean
    private ImageRepository imageRepository;

    @MockBean
    private ContainerRepository containerRepository;

    @MockBean
    private DatabaseRepository databaseRepository;

    @MockBean
    private QueryService queryService;

    @MockBean
    private StoreService storeService;

    @Autowired
    private QueryEndpoint queryEndpoint;

    @Test
    public void execute_publicResearcherWriteAllForbiddenKeyword_fails() {
        final String statement = "SELECT m.* FROM `mfcc` m";

        /* test */
        assertThrows(QueryMalformedException.class, () -> {
            generic_execute(CONTAINER_3_ID, DATABASE_3_ID, statement, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_3, DATABASE_3_RESEARCHER_READ_ACCESS);
        });
    }

    @Test
    public void execute_publicResearcherReadEmptyStatement_fails() {
        final String statement = null;

        /* test */
        assertThrows(QueryMalformedException.class, () -> {
            generic_execute(CONTAINER_3_ID, DATABASE_3_ID, statement, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_3, DATABASE_3_RESEARCHER_READ_ACCESS);
        });
    }

    @Test
    public void execute_publicResearcherReadBlankStatement_fails() {
        final String statement = "";

        /* test */
        assertThrows(QueryMalformedException.class, () -> {
            generic_execute(CONTAINER_3_ID, DATABASE_3_ID, statement, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_3, DATABASE_3_RESEARCHER_READ_ACCESS);
        });
    }

    @Test
    public void execute_publicResearcherReadForbiddenKeyword2_fails() {
        final String statement = "SELECT * FROM `mfcc` m";

        /* test */
        assertThrows(QueryMalformedException.class, () -> {
            generic_execute(CONTAINER_3_ID, DATABASE_3_ID, statement, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_3, DATABASE_3_RESEARCHER_READ_ACCESS);
        });
    }

    @Test
    public void execute_publicAnonymized_fails() {
        final Principal principal = null;

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_execute(CONTAINER_3_ID, DATABASE_3_ID, QUERY_4_STATEMENT, null, principal, DATABASE_3, null);
        });
    }

    @Test
    public void execute_publicResearcherRead_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException, SortException, NotAllowedException,
            PaginationException {

        /* test */
        generic_execute(CONTAINER_3_ID, DATABASE_3_ID, QUERY_4_STATEMENT, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_3, DATABASE_3_RESEARCHER_READ_ACCESS);
    }

    @Test
    public void execute_publicResearcherWriteOwn_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException, SortException, NotAllowedException,
            PaginationException {

        /* test */
        generic_execute(CONTAINER_3_ID, DATABASE_3_ID, QUERY_4_STATEMENT, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_3, DATABASE_3_RESEARCHER_WRITE_OWN_ACCESS);
    }

    @Test
    public void execute_publicResearcherWriteAll_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException, SortException, NotAllowedException,
            PaginationException {

        /* test */
        generic_execute(CONTAINER_3_ID, DATABASE_3_ID, QUERY_4_STATEMENT, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_3, DATABASE_3_RESEARCHER_WRITE_ALL_ACCESS);
    }

    @Test
    public void execute_publicDeveloperRead_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException, SortException, NotAllowedException,
            PaginationException {

        /* test */
        generic_execute(CONTAINER_3_ID, DATABASE_3_ID, QUERY_4_STATEMENT, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_3, DATABASE_3_DEVELOPER_READ_ACCESS);
    }

    @Test
    public void execute_publicDeveloperWriteOwn_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException, SortException, NotAllowedException,
            PaginationException {

        /* test */
        generic_execute(CONTAINER_3_ID, DATABASE_3_ID, QUERY_4_STATEMENT, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_3, DATABASE_3_DEVELOPER_WRITE_OWN_ACCESS);
    }

    @Test
    public void execute_publicDeveloperWriteAll_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException, SortException, NotAllowedException,
            PaginationException {

        /* test */
        generic_execute(CONTAINER_3_ID, DATABASE_3_ID, QUERY_4_STATEMENT, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_3, DATABASE_3_DEVELOPER_WRITE_ALL_ACCESS);
    }

    @Test
    public void execute_publicDataStewardRead_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException, SortException, NotAllowedException,
            PaginationException {

        /* test */
        generic_execute(CONTAINER_3_ID, DATABASE_3_ID, QUERY_4_STATEMENT, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_3, DATABASE_3_DATA_STEWARD_READ_ACCESS);
    }

    @Test
    public void execute_publicDataStewardWriteOwn_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException, SortException, NotAllowedException,
            PaginationException {

        /* test */
        generic_execute(CONTAINER_3_ID, DATABASE_3_ID, QUERY_4_STATEMENT, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_3, DATABASE_3_DATA_STEWARD_WRITE_OWN_ACCESS);
    }

    @Test
    public void execute_publicDataStewardWriteAll_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException, SortException, NotAllowedException,
            PaginationException {

        /* test */
        generic_execute(CONTAINER_3_ID, DATABASE_3_ID, QUERY_4_STATEMENT, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_3, DATABASE_3_DATA_STEWARD_WRITE_ALL_ACCESS);
    }

    @Test
    public void reExecute_publicAnonymized_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, SortException, NotAllowedException,
            PaginationException, QueryNotFoundException {

        /* test */
        generic_reExecute(CONTAINER_3_ID, DATABASE_3_ID, null, null, DATABASE_3, null);
    }

    @Test
    public void reExecute_publicResearcherRead_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, SortException, NotAllowedException,
            PaginationException, QueryNotFoundException {

        /* test */
        generic_reExecute(CONTAINER_3_ID, DATABASE_3_ID, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_3, DATABASE_3_RESEARCHER_READ_ACCESS);
    }

    @Test
    public void reExecute_publicResearcherWriteOwn_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, SortException, NotAllowedException,
            PaginationException, QueryNotFoundException {

        /* test */
        generic_reExecute(CONTAINER_3_ID, DATABASE_3_ID, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_3, DATABASE_3_RESEARCHER_WRITE_OWN_ACCESS);
    }

    @Test
    public void reExecute_publicResearcherWriteAll_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, SortException, NotAllowedException,
            PaginationException, QueryNotFoundException {

        /* test */
        generic_reExecute(CONTAINER_3_ID, DATABASE_3_ID, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_3, DATABASE_3_RESEARCHER_WRITE_ALL_ACCESS);
    }

    @Test
    public void reExecute_publicDeveloperRead_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, SortException, NotAllowedException,
            PaginationException, QueryNotFoundException {

        /* test */
        generic_reExecute(CONTAINER_3_ID, DATABASE_3_ID, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_3, DATABASE_3_DEVELOPER_READ_ACCESS);
    }

    @Test
    public void reExecute_publicDeveloperWriteOwn_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, SortException, NotAllowedException,
            PaginationException, QueryNotFoundException {

        /* test */
        generic_reExecute(CONTAINER_3_ID, DATABASE_3_ID, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_3, DATABASE_3_DEVELOPER_WRITE_OWN_ACCESS);
    }

    @Test
    public void reExecute_publicDeveloperWriteAll_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, SortException, NotAllowedException,
            PaginationException, QueryNotFoundException {

        /* test */
        generic_reExecute(CONTAINER_3_ID, DATABASE_3_ID, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_3, DATABASE_3_DEVELOPER_WRITE_ALL_ACCESS);
    }

    @Test
    public void reExecute_publicDataStewardRead_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, SortException, NotAllowedException,
            PaginationException, QueryNotFoundException {

        /* test */
        generic_reExecute(CONTAINER_3_ID, DATABASE_3_ID, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_3, DATABASE_3_DATA_STEWARD_READ_ACCESS);
    }

    @Test
    public void reExecute_publicDataStewardWriteOwn_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, SortException, NotAllowedException,
            PaginationException, QueryNotFoundException {

        /* test */
        generic_reExecute(CONTAINER_3_ID, DATABASE_3_ID, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_3, DATABASE_3_DATA_STEWARD_WRITE_OWN_ACCESS);
    }

    @Test
    public void reExecute_publicDataStewardWriteAll_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, SortException, NotAllowedException,
            PaginationException, QueryNotFoundException {

        /* test */
        generic_reExecute(CONTAINER_3_ID, DATABASE_3_ID, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_3, DATABASE_3_DATA_STEWARD_WRITE_ALL_ACCESS);
    }

    @Test
    public void export_publicAnonymized_succeeds() throws UserNotFoundException, QueryStoreException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, NotAllowedException, QueryNotFoundException, FileStorageException,
            ContainerNotFoundException, IOException {

        /* test */
        export_generic(CONTAINER_3_ID, DATABASE_3_ID, QUERY_4_ID, null, null, DATABASE_3, null, null, HttpStatus.OK);
    }

    @Test
    public void export_publicAnonymized_fails() throws UserNotFoundException, QueryStoreException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, NotAllowedException, QueryNotFoundException, FileStorageException,
            ContainerNotFoundException, IOException {

        /* test */
        export_generic(CONTAINER_3_ID, DATABASE_3_ID, QUERY_4_ID, null, null, DATABASE_3, null, "application/json", HttpStatus.NOT_IMPLEMENTED);
    }

    @Test
    public void export_publicResearcherRead_succeeds() throws UserNotFoundException, QueryStoreException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, NotAllowedException, QueryNotFoundException, FileStorageException,
            ContainerNotFoundException, IOException {

        /* test */
        export_generic(CONTAINER_3_ID, DATABASE_3_ID, QUERY_4_ID, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_3, DATABASE_3_RESEARCHER_READ_ACCESS, null, HttpStatus.OK);
    }

    @Test
    public void export_publicResearcherWriteOwn_succeeds() throws UserNotFoundException, QueryStoreException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, NotAllowedException, QueryNotFoundException, FileStorageException,
            ContainerNotFoundException, IOException {

        /* test */
        export_generic(CONTAINER_3_ID, DATABASE_3_ID, QUERY_4_ID, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_3, DATABASE_3_RESEARCHER_WRITE_OWN_ACCESS, null, HttpStatus.OK);
    }

    @Test
    public void export_publicResearcherWriteAll_succeeds() throws UserNotFoundException, QueryStoreException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, NotAllowedException, QueryNotFoundException, FileStorageException,
            ContainerNotFoundException, IOException {

        /* test */
        export_generic(CONTAINER_3_ID, DATABASE_3_ID, QUERY_4_ID, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_3, DATABASE_3_RESEARCHER_WRITE_ALL_ACCESS, null, HttpStatus.OK);
    }

    @Test
    public void export_publicDeveloperRead_succeeds() throws UserNotFoundException, QueryStoreException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, NotAllowedException, QueryNotFoundException, FileStorageException,
            ContainerNotFoundException, IOException {

        /* test */
        export_generic(CONTAINER_3_ID, DATABASE_3_ID, QUERY_4_ID, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_3, DATABASE_3_DEVELOPER_READ_ACCESS, null, HttpStatus.OK);
    }

    @Test
    public void export_publicDeveloperWriteOwn_succeeds() throws UserNotFoundException, QueryStoreException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, NotAllowedException, QueryNotFoundException, FileStorageException,
            ContainerNotFoundException, IOException {

        /* test */
        export_generic(CONTAINER_3_ID, DATABASE_3_ID, QUERY_4_ID, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_3, DATABASE_3_DEVELOPER_WRITE_OWN_ACCESS, null, HttpStatus.OK);
    }

    @Test
    public void export_publicDeveloperWriteAll_succeeds() throws UserNotFoundException, QueryStoreException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, NotAllowedException, QueryNotFoundException, FileStorageException,
            ContainerNotFoundException, IOException {

        /* test */
        export_generic(CONTAINER_3_ID, DATABASE_3_ID, QUERY_4_ID, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_3, DATABASE_3_DEVELOPER_WRITE_ALL_ACCESS, null, HttpStatus.OK);
    }

    @Test
    public void export_publicDataStewardRead_succeeds() throws UserNotFoundException, QueryStoreException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, NotAllowedException, QueryNotFoundException, FileStorageException,
            ContainerNotFoundException, IOException {

        /* test */
        export_generic(CONTAINER_3_ID, DATABASE_3_ID, QUERY_4_ID, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_3, DATABASE_3_DATA_STEWARD_READ_ACCESS, null, HttpStatus.OK);
    }

    @Test
    public void export_publicDataStewardWriteOwn_succeeds() throws UserNotFoundException, QueryStoreException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, NotAllowedException, QueryNotFoundException, FileStorageException,
            ContainerNotFoundException, IOException {

        /* test */
        export_generic(CONTAINER_3_ID, DATABASE_3_ID, QUERY_4_ID, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_3, DATABASE_3_DATA_STEWARD_WRITE_OWN_ACCESS, null, HttpStatus.OK);
    }

    @Test
    public void export_publicDataStewardWriteAll_succeeds() throws UserNotFoundException, QueryStoreException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, NotAllowedException, QueryNotFoundException, FileStorageException,
            ContainerNotFoundException, IOException {

        /* test */
        export_generic(CONTAINER_3_ID, DATABASE_3_ID, QUERY_4_ID, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_3, DATABASE_3_DATA_STEWARD_WRITE_ALL_ACCESS, null, HttpStatus.OK);
    }

    /* ################################################################################################### */
    /* ## PRIVATE DATABASES                                                                             ## */
    /* ################################################################################################### */

    @Test
    public void execute_privateResearcherWriteAllForbiddenKeyword_fails() {
        final String statement = "SELECT m.* FROM `mfcc` m";

        /* test */
        assertThrows(QueryMalformedException.class, () -> {
            generic_execute(CONTAINER_2_ID, DATABASE_2_ID, statement, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_2, DATABASE_2_RESEARCHER_READ_ACCESS);
        });
    }

    @Test
    public void execute_privateResearcherReadEmptyStatement_fails() {
        final String statement = null;

        /* test */
        assertThrows(QueryMalformedException.class, () -> {
            generic_execute(CONTAINER_2_ID, DATABASE_2_ID, statement, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_2, DATABASE_2_RESEARCHER_READ_ACCESS);
        });
    }

    @Test
    public void execute_privateResearcherReadBlankStatement_fails() {
        final String statement = "";

        /* test */
        assertThrows(QueryMalformedException.class, () -> {
            generic_execute(CONTAINER_2_ID, DATABASE_2_ID, statement, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_2, DATABASE_2_RESEARCHER_READ_ACCESS);
        });
    }

    @Test
    public void execute_privateResearcherReadForbiddenKeyword2_fails() {
        final String statement = "SELECT * FROM `mfcc` m";

        /* test */
        assertThrows(QueryMalformedException.class, () -> {
            generic_execute(CONTAINER_2_ID, DATABASE_2_ID, statement, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_2, DATABASE_2_RESEARCHER_READ_ACCESS);
        });
    }

    @Test
    public void execute_privateAnonymized_fails() {
        final Principal principal = null;

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_execute(CONTAINER_2_ID, DATABASE_2_ID, QUERY_3_STATEMENT, null, principal, DATABASE_2, null);
        });
    }

    @Test
    public void execute_privateResearcherRead_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException, SortException, NotAllowedException,
            PaginationException {

        /* test */
        generic_execute(CONTAINER_2_ID, DATABASE_2_ID, QUERY_3_STATEMENT, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_2, DATABASE_2_RESEARCHER_READ_ACCESS);
    }

    @Test
    public void execute_privateResearcherWriteOwn_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException, SortException, NotAllowedException,
            PaginationException {

        /* test */
        generic_execute(CONTAINER_2_ID, DATABASE_2_ID, QUERY_3_STATEMENT, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_2, DATABASE_2_RESEARCHER_WRITE_OWN_ACCESS);
    }

    @Test
    public void execute_privateResearcherWriteAll_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException, SortException, NotAllowedException,
            PaginationException {

        /* test */
        generic_execute(CONTAINER_2_ID, DATABASE_2_ID, QUERY_3_STATEMENT, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_2, DATABASE_2_RESEARCHER_WRITE_ALL_ACCESS);
    }

    @Test
    public void execute_privateDeveloperRead_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException, SortException, NotAllowedException,
            PaginationException {

        /* test */
        generic_execute(CONTAINER_2_ID, DATABASE_2_ID, QUERY_3_STATEMENT, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2, DATABASE_2_DEVELOPER_READ_ACCESS);
    }

    @Test
    public void execute_privateDeveloperWriteOwn_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException, SortException, NotAllowedException,
            PaginationException {

        /* test */
        generic_execute(CONTAINER_2_ID, DATABASE_2_ID, QUERY_3_STATEMENT, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2, DATABASE_2_DEVELOPER_WRITE_OWN_ACCESS);
    }

    @Test
    public void execute_privateDeveloperWriteAll_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException, SortException, NotAllowedException,
            PaginationException {

        /* test */
        generic_execute(CONTAINER_2_ID, DATABASE_2_ID, QUERY_3_STATEMENT, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2, DATABASE_2_DEVELOPER_WRITE_ALL_ACCESS);
    }

    @Test
    public void execute_privateDataStewardRead_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException, SortException, NotAllowedException,
            PaginationException {

        /* test */
        generic_execute(CONTAINER_2_ID, DATABASE_2_ID, QUERY_3_STATEMENT, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_2, DATABASE_2_DATA_STEWARD_READ_ACCESS);
    }

    @Test
    public void execute_privateDataStewardWriteOwn_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException, SortException, NotAllowedException,
            PaginationException {

        /* test */
        generic_execute(CONTAINER_2_ID, DATABASE_2_ID, QUERY_3_STATEMENT, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_2, DATABASE_2_DATA_STEWARD_WRITE_OWN_ACCESS);
    }

    @Test
    public void execute_privateDataStewardWriteAll_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException, SortException, NotAllowedException,
            PaginationException {

        /* test */
        generic_execute(CONTAINER_2_ID, DATABASE_2_ID, QUERY_3_STATEMENT, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_2, DATABASE_2_DATA_STEWARD_WRITE_ALL_ACCESS);
    }

    @Test
    public void reExecute_privateAnonymized_succeeds() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            generic_reExecute(CONTAINER_2_ID, DATABASE_2_ID, null, null, DATABASE_2, null);
        });
    }

    @Test
    public void reExecute_privateResearcherRead_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, SortException, NotAllowedException,
            PaginationException, QueryNotFoundException {

        /* test */
        generic_reExecute(CONTAINER_2_ID, DATABASE_2_ID, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_2, DATABASE_2_RESEARCHER_READ_ACCESS);
    }

    @Test
    public void reExecute_privateResearcherWriteOwn_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, SortException, NotAllowedException,
            PaginationException, QueryNotFoundException {

        /* test */
        generic_reExecute(CONTAINER_2_ID, DATABASE_2_ID, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_2, DATABASE_2_RESEARCHER_WRITE_OWN_ACCESS);
    }

    @Test
    public void reExecute_privateResearcherWriteAll_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, SortException, NotAllowedException,
            PaginationException, QueryNotFoundException {

        /* test */
        generic_reExecute(CONTAINER_2_ID, DATABASE_2_ID, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_2, DATABASE_2_RESEARCHER_WRITE_ALL_ACCESS);
    }

    @Test
    public void reExecute_privateDeveloperRead_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, SortException, NotAllowedException,
            PaginationException, QueryNotFoundException {

        /* test */
        generic_reExecute(CONTAINER_2_ID, DATABASE_2_ID, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2, DATABASE_2_DEVELOPER_READ_ACCESS);
    }

    @Test
    public void reExecute_privateDeveloperWriteOwn_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, SortException, NotAllowedException,
            PaginationException, QueryNotFoundException {

        /* test */
        generic_reExecute(CONTAINER_2_ID, DATABASE_2_ID, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2, DATABASE_2_DEVELOPER_WRITE_OWN_ACCESS);
    }

    @Test
    public void reExecute_privateDeveloperWriteAll_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, SortException, NotAllowedException,
            PaginationException, QueryNotFoundException {

        /* test */
        generic_reExecute(CONTAINER_2_ID, DATABASE_2_ID, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2, DATABASE_2_DEVELOPER_WRITE_ALL_ACCESS);
    }

    @Test
    public void reExecute_privateDataStewardRead_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, SortException, NotAllowedException,
            PaginationException, QueryNotFoundException {

        /* test */
        generic_reExecute(CONTAINER_2_ID, DATABASE_2_ID, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_2, DATABASE_2_DATA_STEWARD_READ_ACCESS);
    }

    @Test
    public void reExecute_privateDataStewardWriteOwn_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, SortException, NotAllowedException,
            PaginationException, QueryNotFoundException {

        /* test */
        generic_reExecute(CONTAINER_2_ID, DATABASE_2_ID, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_2, DATABASE_2_DATA_STEWARD_WRITE_OWN_ACCESS);
    }

    @Test
    public void reExecute_privateDataStewardWriteAll_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, SortException, NotAllowedException,
            PaginationException, QueryNotFoundException {

        /* test */
        generic_reExecute(CONTAINER_2_ID, DATABASE_2_ID, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_2, DATABASE_2_DATA_STEWARD_WRITE_ALL_ACCESS);
    }

    @Test
    public void export_privateAnonymized_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            export_generic(CONTAINER_2_ID, DATABASE_2_ID, QUERY_3_ID, null, null, DATABASE_2, null, null, HttpStatus.OK);
        });
    }

    @Test
    public void export_privateAnonymized2_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            export_generic(CONTAINER_2_ID, DATABASE_2_ID, QUERY_3_ID, null, null, DATABASE_2, null, "application/json", HttpStatus.NOT_IMPLEMENTED);
        });
    }

    @Test
    public void export_privateResearcherRead_succeeds() throws UserNotFoundException, QueryStoreException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, NotAllowedException, QueryNotFoundException, FileStorageException,
            ContainerNotFoundException, IOException {

        /* test */
        export_generic(CONTAINER_2_ID, DATABASE_2_ID, QUERY_3_ID, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_2, DATABASE_2_RESEARCHER_READ_ACCESS, null, HttpStatus.OK);
    }

    @Test
    public void export_privateResearcherWriteOwn_succeeds() throws UserNotFoundException, QueryStoreException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, NotAllowedException, QueryNotFoundException, FileStorageException,
            ContainerNotFoundException, IOException {

        /* test */
        export_generic(CONTAINER_2_ID, DATABASE_2_ID, QUERY_3_ID, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_2, DATABASE_2_RESEARCHER_WRITE_OWN_ACCESS, null, HttpStatus.OK);
    }

    @Test
    public void export_privateResearcherWriteAll_succeeds() throws UserNotFoundException, QueryStoreException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, NotAllowedException, QueryNotFoundException, FileStorageException,
            ContainerNotFoundException, IOException {

        /* test */
        export_generic(CONTAINER_2_ID, DATABASE_2_ID, QUERY_3_ID, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_2, DATABASE_2_RESEARCHER_WRITE_ALL_ACCESS, null, HttpStatus.OK);
    }

    @Test
    public void export_privateDeveloperRead_succeeds() throws UserNotFoundException, QueryStoreException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, NotAllowedException, QueryNotFoundException, FileStorageException,
            ContainerNotFoundException, IOException {

        /* test */
        export_generic(CONTAINER_2_ID, DATABASE_2_ID, QUERY_3_ID, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2, DATABASE_2_DEVELOPER_READ_ACCESS, null, HttpStatus.OK);
    }

    @Test
    public void export_privateDeveloperWriteOwn_succeeds() throws UserNotFoundException, QueryStoreException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, NotAllowedException, QueryNotFoundException, FileStorageException,
            ContainerNotFoundException, IOException {

        /* test */
        export_generic(CONTAINER_2_ID, DATABASE_2_ID, QUERY_3_ID, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2, DATABASE_2_DEVELOPER_WRITE_OWN_ACCESS, null, HttpStatus.OK);
    }

    @Test
    public void export_privateDeveloperWriteAll_succeeds() throws UserNotFoundException, QueryStoreException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, NotAllowedException, QueryNotFoundException, FileStorageException,
            ContainerNotFoundException, IOException {

        /* test */
        export_generic(CONTAINER_2_ID, DATABASE_2_ID, QUERY_3_ID, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2, DATABASE_2_DEVELOPER_WRITE_ALL_ACCESS, null, HttpStatus.OK);
    }

    @Test
    public void export_privateDataStewardRead_succeeds() throws UserNotFoundException, QueryStoreException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, NotAllowedException, QueryNotFoundException, FileStorageException,
            ContainerNotFoundException, IOException {

        /* test */
        export_generic(CONTAINER_2_ID, DATABASE_2_ID, QUERY_3_ID, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_2, DATABASE_2_DATA_STEWARD_READ_ACCESS, null, HttpStatus.OK);
    }

    @Test
    public void export_privateDataStewardWriteOwn_succeeds() throws UserNotFoundException, QueryStoreException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, NotAllowedException, QueryNotFoundException, FileStorageException,
            ContainerNotFoundException, IOException {

        /* test */
        export_generic(CONTAINER_2_ID, DATABASE_2_ID, QUERY_3_ID, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_2, DATABASE_2_DATA_STEWARD_WRITE_OWN_ACCESS, null, HttpStatus.OK);
    }

    @Test
    public void export_privateDataStewardWriteAll_succeeds() throws UserNotFoundException, QueryStoreException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, NotAllowedException, QueryNotFoundException, FileStorageException,
            ContainerNotFoundException, IOException {

        /* test */
        export_generic(CONTAINER_2_ID, DATABASE_2_ID, QUERY_3_ID, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_2, DATABASE_2_DATA_STEWARD_WRITE_ALL_ACCESS, null, HttpStatus.OK);
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected void generic_execute(Long containerId, Long databaseId, String statement, String username,
                                   Principal principal, Database database, DatabaseAccess access)
            throws UserNotFoundException, QueryStoreException, TableMalformedException, DatabaseConnectionException,
            QueryMalformedException, ColumnParseException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, SortException, NotAllowedException, PaginationException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(statement)
                .build();
        final Long page = 0L;
        final Long size = 2L;
        final SortType sortDirection = SortType.ASC;
        final String sortColumn = "location";

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(containerId, databaseId))
                .thenReturn(Optional.of(database));
        log.trace("mock database for container with id {} and database id {}", containerId, databaseId);
        if (access == null) {
            when(databaseAccessRepository.findByDatabaseIdAndUsername(databaseId, username))
                    .thenReturn(Optional.empty());
            log.trace("mock no access for database with id {} and username {}", databaseId, username);
        } else {
            when(databaseAccessRepository.findByDatabaseIdAndUsername(databaseId, username))
                    .thenReturn(Optional.of(access));
            log.trace("mock access {} for database with id {} and username {}", access.getType(), databaseId, username);
        }
        when(queryService.execute(containerId, databaseId, request, principal, page, size, sortDirection, sortColumn))
                .thenReturn(QUERY_1_RESULT_DTO);
        log.trace("mock query service for container with id {} and database with id {}", containerId, databaseId);

        /* test */
        final ResponseEntity<QueryResultDto> response = queryEndpoint.execute(containerId, databaseId, request,
                page, size, principal, sortDirection, sortColumn);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(QUERY_1_RESULT_ID, response.getBody().getId());
        assertEquals(QUERY_1_RESULT_NUMBER, response.getBody().getResultNumber());
        assertEquals(QUERY_1_RESULT_NUMBER, response.getBody().getResult().size());
        assertEquals(QUERY_1_RESULT_RESULT, response.getBody().getResult());
    }

    protected void generic_reExecute(Long containerId, Long databaseId, String username, Principal principal, Database database, DatabaseAccess access)
            throws UserNotFoundException, QueryStoreException, DatabaseConnectionException, QueryNotFoundException,
            DatabaseNotFoundException, ImageNotSupportedException, TableMalformedException, QueryMalformedException,
            ColumnParseException, SortException, NotAllowedException, PaginationException {
        final Long page = 0L;
        final Long size = 2L;
        final SortType sortDirection = SortType.ASC;
        final String sortColumn = "location";

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(containerId, databaseId))
                .thenReturn(Optional.of(database));
        when(storeService.findOne(containerId, databaseId, QUERY_4_ID, principal))
                .thenReturn(QUERY_4);
        if (access == null) {
            when(databaseAccessRepository.findByDatabaseIdAndUsername(databaseId, username))
                    .thenReturn(Optional.empty());
        } else {
            when(databaseAccessRepository.findByDatabaseIdAndUsername(databaseId, username))
                    .thenReturn(Optional.of(access));
        }
        when(queryService.reExecute(containerId, databaseId, QUERY_4, page, size, sortDirection, sortColumn, principal))
                .thenReturn(QUERY_4_RESULT_DTO);

        /* test */
        final ResponseEntity<QueryResultDto> response = queryEndpoint.reExecute(containerId, databaseId, QUERY_4_ID,
                principal, page, size, sortDirection, sortColumn);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(QUERY_4_RESULT_ID, response.getBody().getId());
        assertEquals(QUERY_4_RESULT_NUMBER, response.getBody().getResultNumber());
        assertEquals(QUERY_4_RESULT_RESULT, response.getBody().getResult());
    }

    protected void export_generic(Long containerId, Long databaseId, Long queryId, String username, Principal principal,
                                  Database database, DatabaseAccess access, String accept, HttpStatus status) throws IOException,
            UserNotFoundException, QueryStoreException, DatabaseConnectionException, QueryNotFoundException,
            DatabaseNotFoundException, ImageNotSupportedException, TableMalformedException, QueryMalformedException,
            FileStorageException, ContainerNotFoundException, NotAllowedException {
        final ExportResource resource = ExportResource.builder()
                .filename("location.csv")
                .resource(new InputStreamResource(FileUtils.openInputStream(new File("../../dbrepo-metadata-db/test/src/test/resources/weather/location.csv"))))
                .build();

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(containerId, databaseId))
                .thenReturn(Optional.of(database));
        if (access == null) {
            when(databaseAccessRepository.findByDatabaseIdAndUsername(databaseId, username))
                    .thenReturn(Optional.empty());
        } else {
            when(databaseAccessRepository.findByDatabaseIdAndUsername(databaseId, username))
                    .thenReturn(Optional.of(access));
        }
        when(storeService.findOne(containerId, databaseId, QUERY_4_ID, principal))
                .thenReturn(QUERY_4);
        when(storeService.findOne(containerId, databaseId, QUERY_3_ID, principal))
                .thenReturn(QUERY_3);
        when(queryService.findOne(containerId, databaseId, queryId, principal))
                .thenReturn(resource);

        /* test */
        final ResponseEntity<?> response = queryEndpoint.export(containerId, databaseId, queryId, accept, principal);
        assertEquals(status, response.getStatusCode());
        if (status.equals(HttpStatus.OK)) {
            assertNotNull(response.getBody());
        }
    }

}