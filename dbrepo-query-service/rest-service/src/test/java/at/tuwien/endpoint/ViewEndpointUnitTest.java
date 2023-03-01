package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.ViewBriefDto;
import at.tuwien.api.database.ViewCreateDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.api.database.query.ExecuteStatementDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.config.IndexConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.exception.*;
import at.tuwien.listener.impl.RabbitMqListenerImpl;
import at.tuwien.service.AccessService;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.QueryService;
import at.tuwien.service.ViewService;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.Principal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class ViewEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private Channel channel;

    @MockBean
    private IndexConfig indexInitializer;

    @MockBean
    private RabbitMqListenerImpl rabbitMqListener;

    @MockBean
    private QueryService queryService;

    @MockBean
    private DatabaseService databaseService;

    @MockBean
    private AccessService accessService;

    @MockBean
    private ViewService viewService;

    @Autowired
    private ViewEndpoint viewEndpoint;

    @Test
    public void findAll_publicAnonymous_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException {

        /* test */
        findAll_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, null, null, null);
    }

    @Test
    @WithAnonymousUser
    public void findAll_publicAnonymous2_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException {

        /* test */
        findAll_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, null, null, null);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void findAll_publicResearcherRead_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException {

        /* test */
        findAll_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_3_RESEARCHER_READ_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void findAll_publicResearcherWriteOwn_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException {

        /* test */
        findAll_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_3_RESEARCHER_WRITE_OWN_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void findAll_publicResearcherWriteAll_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException {

        /* test */
        findAll_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_3_RESEARCHER_WRITE_ALL_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void findAll_publicDeveloperRead_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException {

        /* test */
        findAll_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_3_DEVELOPER_READ_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void findAll_publicDeveloperWriteOwn_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException {

        /* test */
        findAll_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_3_DEVELOPER_WRITE_OWN_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void findAll_publicDeveloperWriteAll_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException {

        /* test */
        findAll_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_3_DEVELOPER_WRITE_ALL_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void findAll_publicDataStewardRead_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException {

        /* test */
        findAll_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_3_DATA_STEWARD_READ_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void findAll_publicDataStewardWriteOwn_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException {

        /* test */
        findAll_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_3_DATA_STEWARD_WRITE_OWN_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void findAll_publicDataStewardWriteAll_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException {

        /* test */
        findAll_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_3_DATA_STEWARD_WRITE_ALL_ACCESS);
    }
    
    @Test
    public void create_publicAnonymous_succeeds() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            create_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, null, null, null);
        });
    }

    @Test
    @WithAnonymousUser
    public void create_publicAnonymous2_succeeds() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            create_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void create_publicResearcherRead_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            create_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_3_RESEARCHER_READ_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void create_publicResearcherWriteOwn_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            create_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_3_RESEARCHER_WRITE_OWN_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void create_publicResearcherWriteAll_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            create_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_3_RESEARCHER_WRITE_ALL_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void create_publicDeveloperRead_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            create_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_3_DEVELOPER_READ_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void create_publicDeveloperWriteOwn_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            create_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_3_DEVELOPER_WRITE_OWN_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void create_publicDeveloperWriteAll_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            create_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_3_DEVELOPER_WRITE_ALL_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void create_publicDataStewardRead_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            create_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_3_DATA_STEWARD_READ_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void create_publicDataStewardWriteOwn_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            create_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_3_DATA_STEWARD_WRITE_OWN_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void create_publicDataStewardWriteAll_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            create_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_3_DATA_STEWARD_WRITE_ALL_ACCESS);
        });
    }

    @Test
    public void find_publicAnonymous_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException {

        /* test */
        find_generic(CONTAINER_3_ID, DATABASE_3_ID, VIEW_5_ID, DATABASE_1, null, null, null);
    }

    @Test
    @WithAnonymousUser
    public void find_publicAnonymous2_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException {

        /* test */
        find_generic(CONTAINER_3_ID, DATABASE_3_ID, VIEW_5_ID, DATABASE_3, null, null, null);
    }
    
    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void find_publicResearcherRead_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException {

        /* test */
        find_generic(CONTAINER_3_ID, DATABASE_3_ID, VIEW_5_ID, DATABASE_3, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_3_RESEARCHER_READ_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void find_publicResearcherWriteOwn_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException {

        /* test */
        find_generic(CONTAINER_3_ID, DATABASE_3_ID, VIEW_5_ID, DATABASE_3, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_3_RESEARCHER_WRITE_OWN_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void find_publicResearcherWriteAll_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException {

        /* test */
        find_generic(CONTAINER_3_ID, DATABASE_3_ID, VIEW_5_ID, DATABASE_3, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_3_RESEARCHER_WRITE_ALL_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void find_publicDeveloperRead_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException {

        /* test */
        find_generic(CONTAINER_3_ID, DATABASE_3_ID, VIEW_5_ID, DATABASE_3, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_3_DEVELOPER_READ_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void find_publicDeveloperWriteOwn_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException {

        /* test */
        find_generic(CONTAINER_3_ID, DATABASE_3_ID, VIEW_5_ID, DATABASE_3, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_3_DEVELOPER_WRITE_OWN_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void find_publicDeveloperWriteAll_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException {

        /* test */
        find_generic(CONTAINER_3_ID, DATABASE_3_ID, VIEW_5_ID, DATABASE_3, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_3_DEVELOPER_WRITE_ALL_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void find_publicDataStewardRead_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException {

        /* test */
        find_generic(CONTAINER_3_ID, DATABASE_3_ID, VIEW_5_ID, DATABASE_3, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_3_DATA_STEWARD_READ_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void find_publicDataStewardWriteOwn_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException {

        /* test */
        find_generic(CONTAINER_3_ID, DATABASE_3_ID, VIEW_5_ID, DATABASE_3, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_3_DATA_STEWARD_WRITE_OWN_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void find_publicDataStewardWriteAll_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException {

        /* test */
        find_generic(CONTAINER_3_ID, DATABASE_3_ID, VIEW_5_ID, DATABASE_3, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_3_DATA_STEWARD_WRITE_ALL_ACCESS);
    }
    
    @Test
    public void delete_publicAnonymous_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            delete_generic(CONTAINER_3_ID, DATABASE_3_ID, VIEW_5_ID, DATABASE_3, null, null, null);
        });
    }

    @Test
    @WithAnonymousUser
    public void delete_publicAnonymous2_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            delete_generic(CONTAINER_3_ID, DATABASE_3_ID, VIEW_5_ID, DATABASE_3, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void delete_publicResearcherRead_succeeds() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            delete_generic(CONTAINER_3_ID, DATABASE_3_ID, VIEW_5_ID, DATABASE_3, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_3_RESEARCHER_READ_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void delete_publicResearcherWriteOwn_succeeds() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            delete_generic(CONTAINER_3_ID, DATABASE_3_ID, VIEW_5_ID, DATABASE_3, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_3_RESEARCHER_WRITE_OWN_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void delete_publicResearcherWriteAll_succeeds() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            delete_generic(CONTAINER_3_ID, DATABASE_3_ID, VIEW_5_ID, DATABASE_3, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_3_RESEARCHER_WRITE_ALL_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void delete_publicDeveloperRead_succeeds() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            delete_generic(CONTAINER_3_ID, DATABASE_3_ID, VIEW_5_ID, DATABASE_3, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_3_DEVELOPER_READ_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void delete_publicDeveloperWriteOwn_succeeds() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            delete_generic(CONTAINER_3_ID, DATABASE_3_ID, VIEW_5_ID, DATABASE_3, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_3_DEVELOPER_WRITE_OWN_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void delete_publicDeveloperWriteAll_succeeds() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            delete_generic(CONTAINER_3_ID, DATABASE_3_ID, VIEW_5_ID, DATABASE_3, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_3_DEVELOPER_WRITE_ALL_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void delete_publicDataStewardRead_succeeds() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            delete_generic(CONTAINER_3_ID, DATABASE_3_ID, VIEW_5_ID, DATABASE_3, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_3_DATA_STEWARD_READ_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void delete_publicDataStewardWriteOwn_succeeds() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            delete_generic(CONTAINER_3_ID, DATABASE_3_ID, VIEW_5_ID, DATABASE_3, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_3_DATA_STEWARD_WRITE_OWN_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void delete_publicDataStewardWriteAll_succeeds() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            delete_generic(CONTAINER_3_ID, DATABASE_3_ID, VIEW_5_ID, DATABASE_3, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_3_DATA_STEWARD_WRITE_ALL_ACCESS);
        });
    }

    @Test
    public void data_publicAnonymous_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, DatabaseConnectionException, QueryMalformedException,
            QueryStoreException, TableMalformedException, ColumnParseException, ImageNotSupportedException,
            ContainerNotFoundException, PaginationException {

        /* test */
        data_generic(CONTAINER_3_ID, DATABASE_3_ID, VIEW_5_ID, DATABASE_3, null, null, null);
    }

    @Test
    @WithAnonymousUser
    public void data_publicAnonymous2_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, DatabaseConnectionException, QueryMalformedException,
            QueryStoreException, TableMalformedException, ColumnParseException, ImageNotSupportedException,
            ContainerNotFoundException, PaginationException {

        /* test */
        data_generic(CONTAINER_3_ID, DATABASE_3_ID, VIEW_5_ID, DATABASE_3, null, null, null);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void data_publicResearcherRead_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, DatabaseConnectionException, QueryMalformedException,
            QueryStoreException, TableMalformedException, ColumnParseException, ImageNotSupportedException,
            ContainerNotFoundException, PaginationException {

        /* test */
        data_generic(CONTAINER_3_ID, DATABASE_3_ID, VIEW_5_ID, DATABASE_3, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_3_RESEARCHER_READ_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void data_publicResearcherWriteOwn_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, DatabaseConnectionException, QueryMalformedException,
            QueryStoreException, TableMalformedException, ColumnParseException, ImageNotSupportedException,
            ContainerNotFoundException, PaginationException {

        /* test */
        data_generic(CONTAINER_3_ID, DATABASE_3_ID, VIEW_5_ID, DATABASE_3, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_3_RESEARCHER_WRITE_OWN_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void data_publicResearcherWriteAll_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, DatabaseConnectionException, QueryMalformedException,
            QueryStoreException, TableMalformedException, ColumnParseException, ImageNotSupportedException,
            ContainerNotFoundException, PaginationException {

        /* test */
        data_generic(CONTAINER_3_ID, DATABASE_3_ID, VIEW_5_ID, DATABASE_3, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_3_RESEARCHER_WRITE_ALL_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void data_publicDeveloperRead_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, DatabaseConnectionException, QueryMalformedException,
            QueryStoreException, TableMalformedException, ColumnParseException, ImageNotSupportedException,
            ContainerNotFoundException, PaginationException {

        /* test */
        data_generic(CONTAINER_3_ID, DATABASE_3_ID, VIEW_5_ID, DATABASE_3, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_3_DEVELOPER_READ_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void data_publicDeveloperWriteOwn_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, DatabaseConnectionException, QueryMalformedException,
            QueryStoreException, TableMalformedException, ColumnParseException, ImageNotSupportedException,
            ContainerNotFoundException, PaginationException {

        /* test */
        data_generic(CONTAINER_3_ID, DATABASE_3_ID, VIEW_5_ID, DATABASE_3, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_3_DEVELOPER_WRITE_OWN_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void data_publicDeveloperWriteAll_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, DatabaseConnectionException, QueryMalformedException,
            QueryStoreException, TableMalformedException, ColumnParseException, ImageNotSupportedException,
            ContainerNotFoundException, PaginationException {

        /* test */
        data_generic(CONTAINER_3_ID, DATABASE_3_ID, VIEW_5_ID, DATABASE_3, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_3_DEVELOPER_WRITE_ALL_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void data_publicDataStewardRead_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, DatabaseConnectionException, QueryMalformedException,
            QueryStoreException, TableMalformedException, ColumnParseException, ImageNotSupportedException,
            ContainerNotFoundException, PaginationException {

        /* test */
        data_generic(CONTAINER_3_ID, DATABASE_3_ID, VIEW_5_ID, DATABASE_3, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_3_DATA_STEWARD_READ_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void data_publicDataStewardWriteOwn_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, DatabaseConnectionException, QueryMalformedException,
            QueryStoreException, TableMalformedException, ColumnParseException, ImageNotSupportedException,
            ContainerNotFoundException, PaginationException {

        /* test */
        data_generic(CONTAINER_3_ID, DATABASE_3_ID, VIEW_5_ID, DATABASE_3, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_3_DATA_STEWARD_WRITE_OWN_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void data_publicDataStewardWriteAll_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, DatabaseConnectionException, QueryMalformedException,
            QueryStoreException, TableMalformedException, ColumnParseException, ImageNotSupportedException,
            ContainerNotFoundException, PaginationException {

        /* test */
        data_generic(CONTAINER_3_ID, DATABASE_3_ID, VIEW_5_ID, DATABASE_3, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_3_DATA_STEWARD_WRITE_ALL_ACCESS);
    }

    /* ################################################################################################### */
    /* ## PRIVATE DATABASES                                                                             ## */
    /* ################################################################################################### */

    @Test
    public void findAll_privateAnonymous_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException {

        /* test */
        findAll_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, null, null, null);
    }

    @Test
    @WithAnonymousUser
    public void findAll_privateAnonymous2_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException {

        /* test */
        findAll_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, null, null, null);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void findAll_privateResearcherRead_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException {

        /* test */
        findAll_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_2_RESEARCHER_READ_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void findAll_privateResearcherWriteOwn_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException {

        /* test */
        findAll_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_2_RESEARCHER_WRITE_OWN_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void findAll_privateResearcherWriteAll_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException {

        /* test */
        findAll_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_2_RESEARCHER_WRITE_ALL_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void findAll_privateDeveloperRead_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException {

        /* test */
        findAll_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2_DEVELOPER_READ_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void findAll_privateDeveloperWriteOwn_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException {

        /* test */
        findAll_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2_DEVELOPER_WRITE_OWN_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void findAll_privateDeveloperWriteAll_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException {

        /* test */
        findAll_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2_DEVELOPER_WRITE_ALL_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void findAll_privateDataStewardRead_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException {

        /* test */
        findAll_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_2_DATA_STEWARD_READ_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void findAll_privateDataStewardWriteOwn_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException {

        /* test */
        findAll_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_2_DATA_STEWARD_WRITE_OWN_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void findAll_privateDataStewardWriteAll_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException {

        /* test */
        findAll_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_2_DATA_STEWARD_WRITE_ALL_ACCESS);
    }

    @Test
    public void create_privateAnonymous_succeeds() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            create_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, null, null, null);
        });
    }

    @Test
    @WithAnonymousUser
    public void create_privateAnonymous2_succeeds() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            create_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void create_privateResearcherRead_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            create_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_2_RESEARCHER_READ_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void create_privateResearcherWriteOwn_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            create_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_2_RESEARCHER_WRITE_OWN_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void create_privateResearcherWriteAll_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            create_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_2_RESEARCHER_WRITE_ALL_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void create_privateDeveloperRead_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            create_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2_DEVELOPER_READ_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void create_privateDeveloperWriteOwn_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            create_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2_DEVELOPER_WRITE_OWN_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void create_privateDeveloperWriteAll_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            create_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2_DEVELOPER_WRITE_ALL_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void create_privateDataStewardRead_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            create_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_2_DATA_STEWARD_READ_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void create_privateDataStewardWriteOwn_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            create_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_2_DATA_STEWARD_WRITE_OWN_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void create_privateDataStewardWriteAll_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            create_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_2_DATA_STEWARD_WRITE_ALL_ACCESS);
        });
    }

    @Test
    public void find_privateAnonymous_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException {

        /* test */
        find_generic(CONTAINER_2_ID, DATABASE_2_ID, VIEW_5_ID, DATABASE_1, null, null, null);
    }

    @Test
    @WithAnonymousUser
    public void find_privateAnonymous2_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException {

        /* test */
        find_generic(CONTAINER_2_ID, DATABASE_2_ID, VIEW_5_ID, DATABASE_2, null, null, null);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void find_privateResearcherRead_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException {

        /* test */
        find_generic(CONTAINER_2_ID, DATABASE_2_ID, VIEW_5_ID, DATABASE_2, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_2_RESEARCHER_READ_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void find_privateResearcherWriteOwn_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException {

        /* test */
        find_generic(CONTAINER_2_ID, DATABASE_2_ID, VIEW_5_ID, DATABASE_2, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_2_RESEARCHER_WRITE_OWN_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void find_privateResearcherWriteAll_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException {

        /* test */
        find_generic(CONTAINER_2_ID, DATABASE_2_ID, VIEW_5_ID, DATABASE_2, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_2_RESEARCHER_WRITE_ALL_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void find_privateDeveloperRead_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException {

        /* test */
        find_generic(CONTAINER_2_ID, DATABASE_2_ID, VIEW_5_ID, DATABASE_2, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2_DEVELOPER_READ_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void find_privateDeveloperWriteOwn_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException {

        /* test */
        find_generic(CONTAINER_2_ID, DATABASE_2_ID, VIEW_5_ID, DATABASE_2, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2_DEVELOPER_WRITE_OWN_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void find_privateDeveloperWriteAll_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException {

        /* test */
        find_generic(CONTAINER_2_ID, DATABASE_2_ID, VIEW_5_ID, DATABASE_2, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2_DEVELOPER_WRITE_ALL_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void find_privateDataStewardRead_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException {

        /* test */
        find_generic(CONTAINER_2_ID, DATABASE_2_ID, VIEW_5_ID, DATABASE_2, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_2_DATA_STEWARD_READ_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void find_privateDataStewardWriteOwn_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException {

        /* test */
        find_generic(CONTAINER_2_ID, DATABASE_2_ID, VIEW_5_ID, DATABASE_2, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_2_DATA_STEWARD_WRITE_OWN_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void find_privateDataStewardWriteAll_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException {

        /* test */
        find_generic(CONTAINER_2_ID, DATABASE_2_ID, VIEW_5_ID, DATABASE_2, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_2_DATA_STEWARD_WRITE_ALL_ACCESS);
    }

    @Test
    public void delete_privateAnonymous_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            delete_generic(CONTAINER_2_ID, DATABASE_2_ID, VIEW_5_ID, DATABASE_2, null, null, null);
        });
    }

    @Test
    @WithAnonymousUser
    public void delete_privateAnonymous2_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            delete_generic(CONTAINER_2_ID, DATABASE_2_ID, VIEW_5_ID, DATABASE_2, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void delete_privateResearcherRead_succeeds() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            delete_generic(CONTAINER_2_ID, DATABASE_2_ID, VIEW_5_ID, DATABASE_2, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_2_RESEARCHER_READ_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void delete_privateResearcherWriteOwn_succeeds() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            delete_generic(CONTAINER_2_ID, DATABASE_2_ID, VIEW_5_ID, DATABASE_2, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_2_RESEARCHER_WRITE_OWN_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void delete_privateResearcherWriteAll_succeeds() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            delete_generic(CONTAINER_2_ID, DATABASE_2_ID, VIEW_5_ID, DATABASE_2, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_2_RESEARCHER_WRITE_ALL_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void delete_privateDeveloperRead_succeeds() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            delete_generic(CONTAINER_2_ID, DATABASE_2_ID, VIEW_5_ID, DATABASE_2, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2_DEVELOPER_READ_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void delete_privateDeveloperWriteOwn_succeeds() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            delete_generic(CONTAINER_2_ID, DATABASE_2_ID, VIEW_5_ID, DATABASE_2, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2_DEVELOPER_WRITE_OWN_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void delete_privateDeveloperWriteAll_succeeds() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            delete_generic(CONTAINER_2_ID, DATABASE_2_ID, VIEW_5_ID, DATABASE_2, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2_DEVELOPER_WRITE_ALL_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void delete_privateDataStewardRead_succeeds() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            delete_generic(CONTAINER_2_ID, DATABASE_2_ID, VIEW_5_ID, DATABASE_2, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_2_DATA_STEWARD_READ_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void delete_privateDataStewardWriteOwn_succeeds() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            delete_generic(CONTAINER_2_ID, DATABASE_2_ID, VIEW_5_ID, DATABASE_2, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_2_DATA_STEWARD_WRITE_OWN_ACCESS);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void delete_privateDataStewardWriteAll_succeeds() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            delete_generic(CONTAINER_2_ID, DATABASE_2_ID, VIEW_5_ID, DATABASE_2, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_2_DATA_STEWARD_WRITE_ALL_ACCESS);
        });
    }

    @Test
    public void data_privateAnonymous_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, DatabaseConnectionException, QueryMalformedException,
            QueryStoreException, TableMalformedException, ColumnParseException, ImageNotSupportedException,
            ContainerNotFoundException, PaginationException {

        /* test */
        data_generic(CONTAINER_2_ID, DATABASE_2_ID, VIEW_5_ID, DATABASE_2, null, null, null);
    }

    @Test
    @WithAnonymousUser
    public void data_privateAnonymous2_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, DatabaseConnectionException, QueryMalformedException,
            QueryStoreException, TableMalformedException, ColumnParseException, ImageNotSupportedException,
            ContainerNotFoundException, PaginationException {

        /* test */
        data_generic(CONTAINER_2_ID, DATABASE_2_ID, VIEW_5_ID, DATABASE_2, null, null, null);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void data_privateResearcherRead_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, DatabaseConnectionException, QueryMalformedException,
            QueryStoreException, TableMalformedException, ColumnParseException, ImageNotSupportedException,
            ContainerNotFoundException, PaginationException {

        /* test */
        data_generic(CONTAINER_2_ID, DATABASE_2_ID, VIEW_5_ID, DATABASE_2, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_2_RESEARCHER_READ_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void data_privateResearcherWriteOwn_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, DatabaseConnectionException, QueryMalformedException,
            QueryStoreException, TableMalformedException, ColumnParseException, ImageNotSupportedException,
            ContainerNotFoundException, PaginationException {

        /* test */
        data_generic(CONTAINER_2_ID, DATABASE_2_ID, VIEW_5_ID, DATABASE_2, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_2_RESEARCHER_WRITE_OWN_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = {"RESEARCHER"})
    public void data_privateResearcherWriteAll_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, DatabaseConnectionException, QueryMalformedException,
            QueryStoreException, TableMalformedException, ColumnParseException, ImageNotSupportedException,
            ContainerNotFoundException, PaginationException {

        /* test */
        data_generic(CONTAINER_2_ID, DATABASE_2_ID, VIEW_5_ID, DATABASE_2, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_2_RESEARCHER_WRITE_ALL_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void data_privateDeveloperRead_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, DatabaseConnectionException, QueryMalformedException,
            QueryStoreException, TableMalformedException, ColumnParseException, ImageNotSupportedException,
            ContainerNotFoundException, PaginationException {

        /* test */
        data_generic(CONTAINER_2_ID, DATABASE_2_ID, VIEW_5_ID, DATABASE_2, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2_DEVELOPER_READ_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void data_privateDeveloperWriteOwn_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, DatabaseConnectionException, QueryMalformedException,
            QueryStoreException, TableMalformedException, ColumnParseException, ImageNotSupportedException,
            ContainerNotFoundException, PaginationException {

        /* test */
        data_generic(CONTAINER_2_ID, DATABASE_2_ID, VIEW_5_ID, DATABASE_2, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2_DEVELOPER_WRITE_OWN_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = {"DEVELOPER"})
    public void data_privateDeveloperWriteAll_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, DatabaseConnectionException, QueryMalformedException,
            QueryStoreException, TableMalformedException, ColumnParseException, ImageNotSupportedException,
            ContainerNotFoundException, PaginationException {

        /* test */
        data_generic(CONTAINER_2_ID, DATABASE_2_ID, VIEW_5_ID, DATABASE_2, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2_DEVELOPER_WRITE_ALL_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void data_privateDataStewardRead_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, DatabaseConnectionException, QueryMalformedException,
            QueryStoreException, TableMalformedException, ColumnParseException, ImageNotSupportedException,
            ContainerNotFoundException, PaginationException {

        /* test */
        data_generic(CONTAINER_2_ID, DATABASE_2_ID, VIEW_5_ID, DATABASE_2, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_2_DATA_STEWARD_READ_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void data_privateDataStewardWriteOwn_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, DatabaseConnectionException, QueryMalformedException,
            QueryStoreException, TableMalformedException, ColumnParseException, ImageNotSupportedException,
            ContainerNotFoundException, PaginationException {

        /* test */
        data_generic(CONTAINER_2_ID, DATABASE_2_ID, VIEW_5_ID, DATABASE_2, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_2_DATA_STEWARD_WRITE_OWN_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = {"DATA_STEWARD"})
    public void data_privateDataStewardWriteAll_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, DatabaseConnectionException, QueryMalformedException,
            QueryStoreException, TableMalformedException, ColumnParseException, ImageNotSupportedException,
            ContainerNotFoundException, PaginationException {

        /* test */
        data_generic(CONTAINER_2_ID, DATABASE_2_ID, VIEW_5_ID, DATABASE_2, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_2_DATA_STEWARD_WRITE_ALL_ACCESS);
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected void findAll_generic(Long containerId, Long databaseId, Database database, String username,
                                   Principal principal, DatabaseAccess access) throws UserNotFoundException,
            NotAllowedException, DatabaseNotFoundException {

        /* mock */
        when(databaseService.find(containerId, databaseId))
                .thenReturn(database);
        if (access != null) {
            log.trace("mock access of database with id {} and username {}", databaseId, username);
            when(accessService.find(databaseId, username))
                    .thenReturn(access);
            when(viewService.findAll(databaseId, principal))
                    .thenReturn(List.of(VIEW_1, VIEW_2));
        } else {
            log.trace("mock no access of database with id {} and username {}", databaseId, username);
            when(accessService.find(databaseId, username))
                    .thenThrow(NotAllowedException.class);
            when(viewService.findAll(databaseId, principal))
                    .thenReturn(List.of(VIEW_1));
        }

        /* test */
        final ResponseEntity<List<ViewBriefDto>> response = viewEndpoint.findAll(containerId, databaseId, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        if (access == null) {
            assertEquals(1, response.getBody().size());
        } else {
            assertEquals(2, response.getBody().size());
        }
    }

    protected void create_generic(Long containerId, Long databaseId, Database database, String username,
                                  Principal principal, DatabaseAccess access) throws DatabaseNotFoundException,
            UserNotFoundException, DatabaseConnectionException, ViewMalformedException, QueryMalformedException,
            NotAllowedException {
        final ViewCreateDto request = ViewCreateDto.builder()
                .name(VIEW_1_NAME)
                .query(VIEW_1_QUERY)
                .isPublic(VIEW_1_PUBLIC)
                .build();

        /* mock */
        when(databaseService.find(containerId, databaseId))
                .thenReturn(database);
        if (access != null) {
            log.trace("mock access of database with id {} and username {}", databaseId, username);
            when(accessService.find(databaseId, username))
                    .thenReturn(access);
        } else {
            log.trace("mock no access of database with id {} and username {}", databaseId, username);
            when(accessService.find(databaseId, username))
                    .thenThrow(NotAllowedException.class);
        }
        when(viewService.create(containerId, databaseId, request, principal))
                .thenReturn(VIEW_1);

        /* test */
        final ResponseEntity<ViewBriefDto> response = viewEndpoint.create(containerId, databaseId, request, principal);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(VIEW_1_ID, response.getBody().getId());
        assertEquals(VIEW_1_NAME, response.getBody().getName());
    }

    protected void find_generic(Long containerId, Long databaseId, Long viewId, Database database, String username,
                                Principal principal, DatabaseAccess access) throws DatabaseNotFoundException,
            UserNotFoundException, NotAllowedException, ViewNotFoundException {

        /* mock */
        when(databaseService.find(containerId, databaseId))
                .thenReturn(database);
        if (access != null) {
            log.trace("mock access of database with id {} and username {}", databaseId, username);
            when(accessService.find(databaseId, username))
                    .thenReturn(access);
        } else {
            log.trace("mock no access of database with id {} and username {}", databaseId, username);
            when(accessService.find(databaseId, username))
                    .thenThrow(NotAllowedException.class);
        }
        when(viewService.findById(databaseId, viewId, principal))
                .thenReturn(VIEW_1);

        /* test */
        final ResponseEntity<ViewDto> response = viewEndpoint.find(containerId, databaseId, viewId, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(VIEW_1_ID, response.getBody().getId());
        assertEquals(VIEW_1_NAME, response.getBody().getName());
    }

    protected void delete_generic(Long containerId, Long databaseId, Long viewId, Database database, String username,
                                  Principal principal, DatabaseAccess access) throws DatabaseNotFoundException,
            UserNotFoundException, NotAllowedException, ViewNotFoundException, DatabaseConnectionException,
            ViewMalformedException, QueryMalformedException {

        /* mock */
        when(databaseService.find(containerId, databaseId))
                .thenReturn(database);
        if (access != null) {
            log.trace("mock access of database with id {} and username {}", databaseId, username);
            when(accessService.find(databaseId, username))
                    .thenReturn(access);
        } else {
            log.trace("mock no access of database with id {} and username {}", databaseId, username);
            when(accessService.find(databaseId, username))
                    .thenThrow(NotAllowedException.class);
        }
        doNothing()
                .when(viewService)
                .delete(containerId, databaseId, viewId, principal);

        /* test */
        final ResponseEntity<?> response = viewEndpoint.delete(containerId, databaseId, viewId, principal);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    protected void data_generic(Long containerId, Long databaseId, Long viewId, Database database, String username,
                                Principal principal, DatabaseAccess access) throws DatabaseNotFoundException,
            UserNotFoundException, NotAllowedException, ViewNotFoundException, DatabaseConnectionException,
            QueryMalformedException, QueryStoreException, TableMalformedException, ColumnParseException,
            ImageNotSupportedException, ContainerNotFoundException, PaginationException {
        final ExecuteStatementDto statement = ExecuteStatementDto.builder()
                .statement(VIEW_1_QUERY)
                .build();
        final Long page = 0L;
        final Long size = 2L;

        /* mock */
        when(databaseService.find(containerId, databaseId))
                .thenReturn(database);
        if (access != null) {
            log.trace("mock access of database with id {} and username {}", databaseId, username);
            when(accessService.find(databaseId, username))
                    .thenReturn(access);
        } else {
            log.trace("mock no access of database with id {} and username {}", databaseId, username);
            when(accessService.find(databaseId, username))
                    .thenThrow(NotAllowedException.class);
        }
        when(viewService.findById(databaseId, viewId, principal))
                .thenReturn(VIEW_1);
        when(queryService.execute(containerId, databaseId, statement, principal, page, size, null, null))
                .thenReturn(QUERY_1_RESULT_DTO);

        /* test */
        final ResponseEntity<QueryResultDto> response = viewEndpoint.data(containerId, databaseId, viewId, principal, page, size);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(QUERY_1_RESULT_ID, response.getBody().getId());
        assertEquals(QUERY_1_RESULT_NUMBER, response.getBody().getResultNumber());
        assertEquals(QUERY_1_RESULT_DTO, response.getBody());
    }

}
