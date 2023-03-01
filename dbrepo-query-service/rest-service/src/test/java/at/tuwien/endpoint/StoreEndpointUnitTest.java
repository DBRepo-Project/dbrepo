package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.query.QueryBriefDto;
import at.tuwien.api.database.query.QueryDto;
import at.tuwien.config.IndexConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.listener.impl.RabbitMqListenerImpl;
import at.tuwien.querystore.Query;
import at.tuwien.repository.jpa.IdentifierRepository;
import at.tuwien.service.AccessService;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.QueryService;
import at.tuwien.service.UserService;
import at.tuwien.service.impl.StoreServiceImpl;
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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class StoreEndpointUnitTest extends BaseUnitTest {

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
    private StoreServiceImpl storeService;

    @MockBean
    private UserService userService;

    @MockBean
    private IdentifierRepository identifierRepository;

    @MockBean
    private DatabaseService databaseService;

    @MockBean
    private AccessService accessService;

    @Autowired
    private StoreEndpoint storeEndpoint;

    @Test
    public void findAll_publicAnonymous_succeeds() throws QueryStoreException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, NotAllowedException, DatabaseConnectionException, TableMalformedException, UserNotFoundException {

        /* test */
        findAll_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, null, null, null, List.of(QUERY_4));
    }

    @Test
    @WithAnonymousUser
    public void findAll_publicAnonymous2_succeeds() throws QueryStoreException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, NotAllowedException, DatabaseConnectionException, TableMalformedException, UserNotFoundException {

        /* test */
        findAll_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, null, null, null, List.of(QUERY_4));
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void findAll_publicResearcherNoAccess_succeeds() throws QueryStoreException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, NotAllowedException, DatabaseConnectionException, TableMalformedException, UserNotFoundException {

        /* test */
        findAll_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, USER_1, USER_1_PRINCIPAL, null, List.of(QUERY_4));
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void findAll_publicResearcherRead_succeeds() throws QueryStoreException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, NotAllowedException, DatabaseConnectionException, TableMalformedException, UserNotFoundException {

        /* test */
        findAll_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, USER_1, USER_1_PRINCIPAL, DATABASE_3_RESEARCHER_READ_ACCESS, List.of(QUERY_4));
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void findAll_publicResearcherWriteOwn_succeeds() throws QueryStoreException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, NotAllowedException, DatabaseConnectionException, TableMalformedException, UserNotFoundException {

        /* test */
        findAll_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, USER_1, USER_1_PRINCIPAL, DATABASE_3_RESEARCHER_WRITE_OWN_ACCESS, List.of(QUERY_4));
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void findAll_publicResearcherWriteAll_succeeds() throws QueryStoreException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, NotAllowedException, DatabaseConnectionException, TableMalformedException, UserNotFoundException {

        /* test */
        findAll_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, USER_1, USER_1_PRINCIPAL, DATABASE_3_RESEARCHER_WRITE_ALL_ACCESS, List.of(QUERY_4));
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME)
    public void findAll_publicDeveloperNoAccess_succeeds() throws QueryStoreException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, NotAllowedException, DatabaseConnectionException, TableMalformedException, UserNotFoundException {

        /* test */
        findAll_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, USER_2, USER_2_PRINCIPAL, null, List.of(QUERY_4));
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME)
    public void findAll_publicDeveloperRead_succeeds() throws QueryStoreException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, NotAllowedException, DatabaseConnectionException, TableMalformedException, UserNotFoundException {

        /* test */
        findAll_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, USER_2, USER_2_PRINCIPAL, DATABASE_3_DEVELOPER_READ_ACCESS, List.of(QUERY_4));
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME)
    public void findAll_publicDeveloperWriteOwn_succeeds() throws QueryStoreException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, NotAllowedException, DatabaseConnectionException, TableMalformedException, UserNotFoundException {

        /* test */
        findAll_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, USER_2, USER_2_PRINCIPAL, DATABASE_3_DEVELOPER_WRITE_OWN_ACCESS, List.of(QUERY_4));
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME)
    public void findAll_publicDeveloperWriteAll_succeeds() throws QueryStoreException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, NotAllowedException, DatabaseConnectionException, TableMalformedException, UserNotFoundException {

        /* test */
        findAll_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, USER_2, USER_2_PRINCIPAL, DATABASE_3_DEVELOPER_WRITE_ALL_ACCESS, List.of(QUERY_4));
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME)
    public void findAll_publicDataStewardNoAccess_succeeds() throws QueryStoreException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, NotAllowedException, DatabaseConnectionException, TableMalformedException, UserNotFoundException {

        /* test */
        findAll_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, USER_3, USER_3_PRINCIPAL, null, List.of(QUERY_4));
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME)
    public void findAll_publicDataStewardRead_succeeds() throws QueryStoreException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, NotAllowedException, DatabaseConnectionException, TableMalformedException, UserNotFoundException {

        /* test */
        findAll_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, USER_3, USER_3_PRINCIPAL, DATABASE_3_DATA_STEWARD_READ_ACCESS, List.of(QUERY_4));
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME)
    public void findAll_publicDataStewardWriteOwn_succeeds() throws QueryStoreException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, NotAllowedException, DatabaseConnectionException, TableMalformedException, UserNotFoundException {

        /* test */
        findAll_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, USER_3, USER_3_PRINCIPAL, DATABASE_3_DATA_STEWARD_WRITE_OWN_ACCESS, List.of(QUERY_4));
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME)
    public void findAll_publicDataStewardWriteAll_succeeds() throws QueryStoreException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, NotAllowedException, DatabaseConnectionException, TableMalformedException, UserNotFoundException {

        /* test */
        findAll_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, USER_3, USER_3_PRINCIPAL, DATABASE_3_DATA_STEWARD_WRITE_ALL_ACCESS, List.of(QUERY_4));
    }

    @Test
    public void find_publicAnonymous_succeeds() throws QueryStoreException, QueryNotFoundException, DatabaseNotFoundException,
            ImageNotSupportedException, UserNotFoundException, NotAllowedException, DatabaseConnectionException {

        /* test */
        final QueryDto response = find_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, QUERY_4_ID, QUERY_4, USER_1_ID, null, null);
        assertEquals(QUERY_4_ID, response.getId());
        assertEquals(QUERY_4_STATEMENT, response.getQuery());
    }

    @Test
    @WithAnonymousUser
    public void find_publicAnonymous2_succeeds() throws QueryStoreException, QueryNotFoundException, DatabaseNotFoundException,
            ImageNotSupportedException, UserNotFoundException, NotAllowedException, DatabaseConnectionException {

        /* test */
        final QueryDto response = find_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, QUERY_4_ID, QUERY_4, USER_1_ID, null, null);
        assertEquals(QUERY_4_ID, response.getId());
        assertEquals(QUERY_4_STATEMENT, response.getQuery());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = "RESEARCHER")
    public void find_publicResearcher_succeeds() throws QueryStoreException, QueryNotFoundException, DatabaseNotFoundException,
            ImageNotSupportedException, UserNotFoundException, NotAllowedException, DatabaseConnectionException {

        /* test */
        final QueryDto response = find_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, QUERY_4_ID, QUERY_4, USER_1_ID, USER_1, USER_1_PRINCIPAL);
        assertEquals(QUERY_4_ID, response.getId());
        assertEquals(QUERY_4_STATEMENT, response.getQuery());
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = "DATA_STEWARD")
    public void find_publicDataSteward_succeeds() throws QueryStoreException, QueryNotFoundException, DatabaseNotFoundException,
            ImageNotSupportedException, UserNotFoundException, NotAllowedException, DatabaseConnectionException {

        /* test */
        final QueryDto response = find_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, QUERY_4_ID, QUERY_4, USER_2_ID, USER_2, USER_2_PRINCIPAL);
        assertEquals(QUERY_4_ID, response.getId());
        assertEquals(QUERY_4_STATEMENT, response.getQuery());
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = "DEVELOPER")
    public void find_publicDeveloper_succeeds() throws QueryStoreException, QueryNotFoundException, DatabaseNotFoundException,
            ImageNotSupportedException, UserNotFoundException, NotAllowedException, DatabaseConnectionException {

        /* test */
        final QueryDto response = find_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, QUERY_4_ID, QUERY_4, USER_3_ID, USER_3, USER_3_PRINCIPAL);
        assertEquals(QUERY_4_ID, response.getId());
        assertEquals(QUERY_4_STATEMENT, response.getQuery());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = "RESEARCHER")
    public void find_publicResearcherQueryNotFound_fails() {

        /* test */
        assertThrows(QueryNotFoundException.class, () -> {
            find_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, QUERY_4_ID, null, USER_1_ID, USER_1, USER_1_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = "RESEARCHER")
    public void find_publicResearcherDatabaseNotFound_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            find_generic(CONTAINER_3_ID, DATABASE_3_ID, null, QUERY_4_ID, QUERY_4, USER_1_ID, USER_1, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void persist_publicAnonymous_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            persist_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, QUERY_4_ID, QUERY_4, null, null, null);
        });
    }

    @Test
    public void persist_publicResearcherRead_succeeds() throws UserNotFoundException, QueryStoreException,
            NotAllowedException, DatabaseConnectionException, QueryAlreadyPersistedException, QueryNotFoundException, DatabaseNotFoundException, ImageNotSupportedException {

        /* test */
        persist_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, QUERY_4_ID, QUERY_4, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_3_RESEARCHER_READ_ACCESS);
    }

    @Test
    public void persist_publicResearcherWriteOwn_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            persist_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, QUERY_4_ID, QUERY_4, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_3_RESEARCHER_WRITE_OWN_ACCESS);
        });
    }

    @Test
    public void persist_publicResearcherWriteAll_succeeds() throws UserNotFoundException, QueryStoreException,
            NotAllowedException, DatabaseConnectionException, QueryAlreadyPersistedException, QueryNotFoundException,
            DatabaseNotFoundException, ImageNotSupportedException {

        /* test */
        final QueryDto response = persist_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, QUERY_4_ID, QUERY_4, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_3_RESEARCHER_WRITE_ALL_ACCESS);
        assertEquals(QUERY_4_ID, response.getId());
        assertEquals(QUERY_4_STATEMENT, response.getQuery());
    }

    @Test
    public void persist_publicDeveloperRead_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            persist_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, QUERY_4_ID, QUERY_4, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_3_DEVELOPER_READ_ACCESS);
        });
    }

    @Test
    public void persist_publicDeveloperWriteOwn_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            persist_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, QUERY_4_ID, QUERY_4, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_3_DEVELOPER_WRITE_OWN_ACCESS);
        });
    }

    @Test
    public void persist_publicDeveloperWriteAll_succeeds() throws UserNotFoundException, QueryStoreException,
            NotAllowedException, DatabaseConnectionException, QueryAlreadyPersistedException, QueryNotFoundException,
            DatabaseNotFoundException, ImageNotSupportedException {

        /* test */
        final QueryDto response = persist_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, QUERY_4_ID, QUERY_4, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_3_DEVELOPER_WRITE_ALL_ACCESS);
        assertEquals(QUERY_4_ID, response.getId());
        assertEquals(QUERY_4_STATEMENT, response.getQuery());
    }

    @Test
    public void persist_publicDataStewardRead_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            persist_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, QUERY_4_ID, QUERY_4, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_3_DATA_STEWARD_READ_ACCESS);
        });
    }

    @Test
    public void persist_publicDataStewardWriteOwn_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            persist_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, QUERY_4_ID, QUERY_4, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_3_DATA_STEWARD_WRITE_OWN_ACCESS);
        });
    }

    @Test
    public void persist_publicDataStewardWriteAll_succeeds() throws UserNotFoundException, QueryStoreException,
            NotAllowedException, DatabaseConnectionException, QueryAlreadyPersistedException, QueryNotFoundException,
            DatabaseNotFoundException, ImageNotSupportedException {

        /* test */
        final QueryDto response = persist_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, QUERY_4_ID, QUERY_4, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_3_DATA_STEWARD_WRITE_ALL_ACCESS);
        assertEquals(QUERY_4_ID, response.getId());
        assertEquals(QUERY_4_STATEMENT, response.getQuery());
    }

    @Test
    public void persist_publicResearcherWriteAllAlreadyPersisted_succeeds() {

        /* test */
        assertThrows(QueryAlreadyPersistedException.class, () -> {
            persist_generic(CONTAINER_3_ID, DATABASE_3_ID, DATABASE_3, QUERY_4_ID, QUERY_4, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_3_RESEARCHER_WRITE_ALL_ACCESS);
        });
    }

    /* ################################################################################################### */
    /* ## PRIVATE DATABASES                                                                             ## */
    /* ################################################################################################### */

    @Test
    public void findAll_privateAnonymous_succeeds() throws QueryStoreException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, NotAllowedException, DatabaseConnectionException, TableMalformedException, UserNotFoundException {

        /* test */
        findAll_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, null, null, null, List.of(QUERY_2));
    }

    @Test
    @WithAnonymousUser
    public void findAll_privateAnonymous2_succeeds() throws QueryStoreException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, NotAllowedException, DatabaseConnectionException, TableMalformedException, UserNotFoundException {

        /* test */
        findAll_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, null, null, null, List.of(QUERY_2));
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void findAll_privateResearcherNoAccess_succeeds() throws QueryStoreException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, NotAllowedException, DatabaseConnectionException, TableMalformedException, UserNotFoundException {

        /* test */
        findAll_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, USER_1, USER_1_PRINCIPAL, null, List.of(QUERY_2));
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void findAll_privateResearcherRead_succeeds() throws QueryStoreException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, NotAllowedException, DatabaseConnectionException, TableMalformedException, UserNotFoundException {

        /* test */
        findAll_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, USER_1, USER_1_PRINCIPAL, DATABASE_2_RESEARCHER_READ_ACCESS, List.of(QUERY_2));
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void findAll_privateResearcherWriteOwn_succeeds() throws QueryStoreException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, NotAllowedException, DatabaseConnectionException, TableMalformedException, UserNotFoundException {

        /* test */
        findAll_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, USER_1, USER_1_PRINCIPAL, DATABASE_2_RESEARCHER_WRITE_OWN_ACCESS, List.of(QUERY_2));
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void findAll_privateResearcherWriteAll_succeeds() throws QueryStoreException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, NotAllowedException, DatabaseConnectionException, TableMalformedException, UserNotFoundException {

        /* test */
        findAll_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, USER_1, USER_1_PRINCIPAL, DATABASE_2_RESEARCHER_WRITE_ALL_ACCESS, List.of(QUERY_2));
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME)
    public void findAll_privateDeveloperNoAccess_succeeds() throws QueryStoreException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, NotAllowedException, DatabaseConnectionException, TableMalformedException, UserNotFoundException {

        /* test */
        findAll_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, USER_2, USER_2_PRINCIPAL, null, List.of(QUERY_2));
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME)
    public void findAll_privateDeveloperRead_succeeds() throws QueryStoreException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, NotAllowedException, DatabaseConnectionException, TableMalformedException, UserNotFoundException {

        /* test */
        findAll_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, USER_2, USER_2_PRINCIPAL, DATABASE_2_DEVELOPER_READ_ACCESS, List.of(QUERY_2));
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME)
    public void findAll_privateDeveloperWriteOwn_succeeds() throws QueryStoreException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, NotAllowedException, DatabaseConnectionException, TableMalformedException, UserNotFoundException {

        /* test */
        findAll_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, USER_2, USER_2_PRINCIPAL, DATABASE_2_DEVELOPER_WRITE_OWN_ACCESS, List.of(QUERY_2));
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME)
    public void findAll_privateDeveloperWriteAll_succeeds() throws QueryStoreException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, NotAllowedException, DatabaseConnectionException, TableMalformedException, UserNotFoundException {

        /* test */
        findAll_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, USER_2, USER_2_PRINCIPAL, DATABASE_2_DEVELOPER_WRITE_ALL_ACCESS, List.of(QUERY_2));
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME)
    public void findAll_privateDataStewardNoAccess_succeeds() throws QueryStoreException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, NotAllowedException, DatabaseConnectionException, TableMalformedException, UserNotFoundException {

        /* test */
        findAll_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, USER_3, USER_3_PRINCIPAL, null, List.of(QUERY_2));
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME)
    public void findAll_privateDataStewardRead_succeeds() throws QueryStoreException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, NotAllowedException, DatabaseConnectionException, TableMalformedException, UserNotFoundException {

        /* test */
        findAll_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, USER_3, USER_3_PRINCIPAL, DATABASE_2_DATA_STEWARD_READ_ACCESS, List.of(QUERY_2));
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME)
    public void findAll_privateDataStewardWriteOwn_succeeds() throws QueryStoreException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, NotAllowedException, DatabaseConnectionException, TableMalformedException, UserNotFoundException {

        /* test */
        findAll_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, USER_3, USER_3_PRINCIPAL, DATABASE_2_DATA_STEWARD_WRITE_OWN_ACCESS, List.of(QUERY_2));
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME)
    public void findAll_privateDataStewardWriteAll_succeeds() throws QueryStoreException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, NotAllowedException, DatabaseConnectionException, TableMalformedException, UserNotFoundException {

        /* test */
        findAll_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, USER_3, USER_3_PRINCIPAL, DATABASE_2_DATA_STEWARD_WRITE_ALL_ACCESS, List.of(QUERY_2));
    }

    @Test
    public void find_privateAnonymous_succeeds() throws QueryStoreException, QueryNotFoundException, DatabaseNotFoundException,
            ImageNotSupportedException, UserNotFoundException, NotAllowedException, DatabaseConnectionException {

        /* test */
        final QueryDto response = find_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, QUERY_2_ID, QUERY_2, USER_1_ID, null, null);
        assertEquals(QUERY_2_ID, response.getId());
        assertEquals(QUERY_2_STATEMENT, response.getQuery());
    }

    @Test
    @WithAnonymousUser
    public void find_privateAnonymous2_succeeds() throws QueryStoreException, QueryNotFoundException, DatabaseNotFoundException,
            ImageNotSupportedException, UserNotFoundException, NotAllowedException, DatabaseConnectionException {

        /* test */
        final QueryDto response = find_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, QUERY_2_ID, QUERY_2, USER_1_ID, null, null);
        assertEquals(QUERY_2_ID, response.getId());
        assertEquals(QUERY_2_STATEMENT, response.getQuery());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = "RESEARCHER")
    public void find_privateResearcher_succeeds() throws QueryStoreException, QueryNotFoundException, DatabaseNotFoundException,
            ImageNotSupportedException, UserNotFoundException, NotAllowedException, DatabaseConnectionException {

        /* test */
        final QueryDto response = find_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, QUERY_2_ID, QUERY_2, USER_1_ID, USER_1, USER_1_PRINCIPAL);
        assertEquals(QUERY_2_ID, response.getId());
        assertEquals(QUERY_2_STATEMENT, response.getQuery());
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, roles = "DATA_STEWARD")
    public void find_privateDataSteward_succeeds() throws QueryStoreException, QueryNotFoundException, DatabaseNotFoundException,
            ImageNotSupportedException, UserNotFoundException, NotAllowedException, DatabaseConnectionException {

        /* test */
        final QueryDto response = find_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, QUERY_2_ID, QUERY_2, USER_2_ID, USER_2, USER_2_PRINCIPAL);
        assertEquals(QUERY_2_ID, response.getId());
        assertEquals(QUERY_2_STATEMENT, response.getQuery());
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, roles = "DEVELOPER")
    public void find_privateDeveloper_succeeds() throws QueryStoreException, QueryNotFoundException, DatabaseNotFoundException,
            ImageNotSupportedException, UserNotFoundException, NotAllowedException, DatabaseConnectionException {

        /* test */
        final QueryDto response = find_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, QUERY_2_ID, QUERY_2, USER_3_ID, USER_3, USER_3_PRINCIPAL);
        assertEquals(QUERY_2_ID, response.getId());
        assertEquals(QUERY_2_STATEMENT, response.getQuery());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = "RESEARCHER")
    public void find_privateResearcherQueryNotFound_fails() {

        /* test */
        assertThrows(QueryNotFoundException.class, () -> {
            find_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, QUERY_2_ID, null, USER_1_ID, USER_1, USER_1_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = "RESEARCHER")
    public void find_privateResearcherDatabaseNotFound_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            find_generic(CONTAINER_2_ID, DATABASE_2_ID, null, QUERY_2_ID, QUERY_2, USER_1_ID, USER_1, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void persist_privateAnonymous_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            persist_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, QUERY_2_ID, QUERY_2, null, null, null);
        });
    }

    @Test
    public void persist_privateResearcherRead_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            persist_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, QUERY_2_ID, QUERY_2, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_2_RESEARCHER_READ_ACCESS);
        });
    }

    @Test
    public void persist_privateResearcherWriteOwn_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            persist_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, QUERY_2_ID, QUERY_2, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_2_RESEARCHER_WRITE_OWN_ACCESS);
        });
    }

    @Test
    public void persist_privateResearcherWriteAll_succeeds() throws UserNotFoundException, QueryStoreException,
            NotAllowedException, DatabaseConnectionException, QueryAlreadyPersistedException, QueryNotFoundException,
            DatabaseNotFoundException, ImageNotSupportedException {

        /* test */
        final QueryDto response = persist_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, QUERY_2_ID, QUERY_2, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_2_RESEARCHER_WRITE_ALL_ACCESS);
        assertEquals(QUERY_2_ID, response.getId());
        assertEquals(QUERY_2_STATEMENT, response.getQuery());
    }

    @Test
    public void persist_privateDeveloperRead_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            persist_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, QUERY_2_ID, QUERY_2, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2_DEVELOPER_READ_ACCESS);
        });
    }

    @Test
    public void persist_privateDeveloperWriteOwn_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            persist_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, QUERY_2_ID, QUERY_2, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2_DEVELOPER_WRITE_OWN_ACCESS);
        });
    }

    @Test
    public void persist_privateDeveloperWriteAll_succeeds() throws UserNotFoundException, QueryStoreException,
            NotAllowedException, DatabaseConnectionException, QueryAlreadyPersistedException, QueryNotFoundException,
            DatabaseNotFoundException, ImageNotSupportedException {

        /* test */
        final QueryDto response = persist_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, QUERY_2_ID, QUERY_2, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2_DEVELOPER_WRITE_ALL_ACCESS);
        assertEquals(QUERY_2_ID, response.getId());
        assertEquals(QUERY_2_STATEMENT, response.getQuery());
    }

    @Test
    public void persist_privateDataStewardRead_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            persist_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, QUERY_2_ID, QUERY_2, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_2_DATA_STEWARD_READ_ACCESS);
        });
    }

    @Test
    public void persist_privateDataStewardWriteOwn_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            persist_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, QUERY_2_ID, QUERY_2, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_2_DATA_STEWARD_WRITE_OWN_ACCESS);
        });
    }

    @Test
    public void persist_privateDataStewardWriteAll_succeeds() throws UserNotFoundException, QueryStoreException,
            NotAllowedException, DatabaseConnectionException, QueryAlreadyPersistedException, QueryNotFoundException,
            DatabaseNotFoundException, ImageNotSupportedException {

        /* test */
        final QueryDto response = persist_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, QUERY_2_ID, QUERY_2, USER_3_USERNAME, USER_3_PRINCIPAL, DATABASE_2_DATA_STEWARD_WRITE_ALL_ACCESS);
        assertEquals(QUERY_2_ID, response.getId());
        assertEquals(QUERY_2_STATEMENT, response.getQuery());
    }

    @Test
    public void persist_privateResearcherWriteAllAlreadyPersisted_succeeds() {

        /* test */
        assertThrows(QueryAlreadyPersistedException.class, () -> {
            persist_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, QUERY_2_ID, QUERY_2, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_2_RESEARCHER_WRITE_ALL_ACCESS);
        });
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected QueryDto persist_generic(Long containerId, Long databaseId, Database database, Long queryId, Query query,
                                       String username, Principal principal, DatabaseAccess access)
            throws DatabaseNotFoundException, UserNotFoundException, QueryStoreException, QueryNotFoundException,
            ImageNotSupportedException, NotAllowedException, DatabaseConnectionException,
            QueryAlreadyPersistedException {

        /* mock */
        when(databaseService.find(containerId, databaseId))
                .thenReturn(database);
        when(storeService.findOne(containerId, databaseId, queryId, principal))
                .thenReturn(query);
        when(storeService.persist(containerId, databaseId, queryId, principal))
                .thenReturn(query);
        if (access != null) {
            log.trace("mock access for database with id {} and username {}", databaseId, username);
            when(accessService.find(databaseId, username))
                    .thenReturn(access);
        } else {
            log.trace("mock no access for database with id {} and username {}", databaseId, username);
            when(accessService.find(databaseId, username))
                    .thenThrow(NotAllowedException.class);
        }

        /* test */
        final ResponseEntity<QueryDto> response = storeEndpoint.persist(containerId, databaseId, queryId, principal);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        return response.getBody();
    }

    protected void findAll_generic(Long containerId, Long databaseId, Database database, User user, Principal principal,
                                   DatabaseAccess access, List<Query> queries) throws UserNotFoundException, QueryStoreException,
            DatabaseConnectionException, TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, NotAllowedException {

        /* mock */
        doReturn(queries)
                .when(storeService)
                .findAll(containerId, databaseId, true, principal);
        if (user != null) {
            when(userService.findAll())
                    .thenReturn(List.of(user));
            if (access != null) {
                when(accessService.find(databaseId, user.getUsername()))
                        .thenReturn(access);
            }
        } else {
            when(userService.findAll())
                    .thenReturn(List.of());
        }
        when(databaseService.find(containerId, databaseId))
                .thenReturn(database);

        /* test */
        final ResponseEntity<List<QueryBriefDto>> response = storeEndpoint.findAll(containerId, databaseId, true, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        final QueryBriefDto query = response.getBody().get(0);
        assertEquals(QUERY_4_ID, query.getId());
        assertEquals(QUERY_4_STATEMENT, query.getQuery());
    }

    protected QueryDto find_generic(Long containerId, Long databaseId, Database database, Long queryId, Query query,
                                    Long userId, User user, Principal principal) throws QueryStoreException,
            QueryNotFoundException, DatabaseNotFoundException, ImageNotSupportedException, UserNotFoundException,
            NotAllowedException, DatabaseConnectionException {

        /* mock */
        if (query != null) {
            when(storeService.findOne(containerId, databaseId, queryId, principal))
                    .thenReturn(query);
        } else {
            when(storeService.findOne(containerId, databaseId, queryId, principal))
                    .thenThrow(QueryNotFoundException.class);
        }
        if (user != null) {
            when(userService.find(userId))
                    .thenReturn(user);
        } else {
            when(userService.find(userId))
                    .thenThrow(UserNotFoundException.class);
        }
        if (database != null) {
            when(databaseService.find(containerId, databaseId))
                    .thenReturn(database);
        } else {
            when(databaseService.find(containerId, databaseId))
                    .thenThrow(DatabaseNotFoundException.class);
        }

        /* test */
        final ResponseEntity<QueryDto> response = storeEndpoint.find(CONTAINER_3_ID, DATABASE_3_ID, QUERY_4_ID, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final QueryDto body = response.getBody();
        assertNotNull(body);
        return body;
    }

}
