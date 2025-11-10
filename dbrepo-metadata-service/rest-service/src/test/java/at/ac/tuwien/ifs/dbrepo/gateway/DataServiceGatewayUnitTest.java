package at.ac.tuwien.ifs.dbrepo.gateway;

import at.ac.tuwien.ifs.dbrepo.core.api.database.AccessTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.ViewDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.QueryDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableStatisticDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class DataServiceGatewayUnitTest extends BaseTest {

    @MockitoBean
    @Qualifier("dataServiceRestTemplate")
    private RestTemplate dataServiceRestTemplate;

    @Autowired
    private DataServiceGateway dataServiceGateway;

    @Test
    public void createAccess_succeeds() throws DataServiceException, DataServiceConnectionException, DatabaseNotFoundException {

        /* mock */
        when(dataServiceRestTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED)
                        .build());

        /* test */
        dataServiceGateway.createAccess(DATABASE_1_ID, USER_1_USERNAME, AccessTypeDto.READ);
    }

    @Test
    public void createAccess_connection_fails() {

        /* mock */
        doThrow(HttpServerErrorException.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(DataServiceConnectionException.class, () -> {
            dataServiceGateway.createAccess(DATABASE_1_ID, USER_1_USERNAME, AccessTypeDto.READ);
        });
    }

    @Test
    public void createAccess_notFound_fails() {

        /* mock */
        doThrow(HttpClientErrorException.NotFound.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            dataServiceGateway.createAccess(DATABASE_1_ID, USER_1_USERNAME, AccessTypeDto.READ);
        });
    }

    @Test
    public void createAccess_unexpected_fails() {

        /* mock */
        doThrow(HttpClientErrorException.BadRequest.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(DataServiceException.class, () -> {
            dataServiceGateway.createAccess(DATABASE_1_ID, USER_1_USERNAME, AccessTypeDto.READ);
        });
    }

    @Test
    public void createAccess_responseCode_fails() {

        /* mock */
        when(dataServiceRestTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .build());

        /* test */
        assertThrows(DataServiceException.class, () -> {
            dataServiceGateway.createAccess(DATABASE_1_ID, USER_1_USERNAME, AccessTypeDto.READ);
        });
    }

    @Test
    public void updateAccess_succeeds() throws DataServiceException, DataServiceConnectionException, AccessNotFoundException {

        /* mock */
        when(dataServiceRestTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.accepted()
                        .build());

        /* test */
        dataServiceGateway.updateAccess(DATABASE_1_ID, USER_1_USERNAME, AccessTypeDto.READ);
    }

    @Test
    public void updateAccess_connection_fails() {

        /* mock */
        doThrow(HttpServerErrorException.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(DataServiceConnectionException.class, () -> {
            dataServiceGateway.updateAccess(DATABASE_1_ID, USER_1_USERNAME, AccessTypeDto.READ);
        });
    }

    @Test
    public void updateAccess_notFound_fails() {

        /* mock */
        doThrow(HttpClientErrorException.NotFound.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(AccessNotFoundException.class, () -> {
            dataServiceGateway.updateAccess(DATABASE_1_ID, USER_1_USERNAME, AccessTypeDto.READ);
        });
    }

    @Test
    public void updateAccess_unexpected_fails() {

        /* mock */
        doThrow(HttpClientErrorException.BadRequest.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(DataServiceException.class, () -> {
            dataServiceGateway.updateAccess(DATABASE_1_ID, USER_1_USERNAME, AccessTypeDto.READ);
        });
    }

    @Test
    public void updateAccess_responseCode_fails() {

        /* mock */
        when(dataServiceRestTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .build());

        /* test */
        assertThrows(DataServiceException.class, () -> {
            dataServiceGateway.updateAccess(DATABASE_1_ID, USER_1_USERNAME, AccessTypeDto.READ);
        });
    }

    @Test
    public void deleteAccess_succeeds() throws DataServiceException, DataServiceConnectionException, AccessNotFoundException {

        /* mock */
        when(dataServiceRestTemplate.exchange(anyString(), eq(HttpMethod.DELETE), eq(HttpEntity.EMPTY), eq(Void.class)))
                .thenReturn(ResponseEntity.accepted()
                        .build());

        /* test */
        dataServiceGateway.deleteAccess(DATABASE_1_ID, USER_1_USERNAME);
    }

    @Test
    public void deleteAccess_connection_fails() {

        /* mock */
        doThrow(HttpServerErrorException.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.DELETE), eq(HttpEntity.EMPTY), eq(Void.class));

        /* test */
        assertThrows(DataServiceConnectionException.class, () -> {
            dataServiceGateway.deleteAccess(DATABASE_1_ID, USER_1_USERNAME);
        });
    }

    @Test
    public void deleteAccess_notFound_fails() {

        /* mock */
        doThrow(HttpClientErrorException.NotFound.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.DELETE), eq(HttpEntity.EMPTY), eq(Void.class));

        /* test */
        assertThrows(AccessNotFoundException.class, () -> {
            dataServiceGateway.deleteAccess(DATABASE_1_ID, USER_1_USERNAME);
        });
    }

    @Test
    public void deleteAccess_unauthorized_fails() {

        /* mock */
        doThrow(HttpClientErrorException.Unauthorized.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.DELETE), eq(HttpEntity.EMPTY), eq(Void.class));

        /* test */
        assertThrows(DataServiceException.class, () -> {
            dataServiceGateway.deleteAccess(DATABASE_1_ID, USER_1_USERNAME);
        });
    }

    @Test
    public void deleteAccess_responseCode_fails() {

        /* mock */
        when(dataServiceRestTemplate.exchange(anyString(), eq(HttpMethod.DELETE), eq(HttpEntity.EMPTY), eq(Void.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .build());

        /* test */
        assertThrows(DataServiceException.class, () -> {
            dataServiceGateway.deleteAccess(DATABASE_1_ID, USER_1_USERNAME);
        });
    }

    @Test
    public void createDatabase_succeeds() throws DataServiceException, DataServiceConnectionException,
            DatabaseNotFoundException {

        /* mock */
        when(dataServiceRestTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(DatabaseDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED)
                        .body(DATABASE_1_DTO));

        /* test */
        dataServiceGateway.createDatabase(DATABASE_1_CREATE_INTERNAL);
    }

    @Test
    public void createDatabase_connection_fails() {

        /* mock */
        doThrow(HttpServerErrorException.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(DatabaseDto.class));

        /* test */
        assertThrows(DataServiceConnectionException.class, () -> {
            dataServiceGateway.createDatabase(DATABASE_1_CREATE_INTERNAL);
        });
    }

    @Test
    public void createDatabase_unauthorized_fails() {

        /* mock */
        doThrow(HttpClientErrorException.Unauthorized.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(DatabaseDto.class));

        /* test */
        assertThrows(DataServiceException.class, () -> {
            dataServiceGateway.createDatabase(DATABASE_1_CREATE_INTERNAL);
        });
    }

    @Test
    public void createDatabase_unexpected_fails() {

        /* mock */
        doThrow(HttpClientErrorException.BadRequest.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(DatabaseDto.class));

        /* test */
        assertThrows(DataServiceException.class, () -> {
            dataServiceGateway.createDatabase(DATABASE_1_CREATE_INTERNAL);
        });
    }

    @Test
    public void createDatabase_responseCode_fails() {

        /* mock */
        when(dataServiceRestTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(DatabaseDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .build());

        /* test */
        assertThrows(DataServiceException.class, () -> {
            dataServiceGateway.createDatabase(DATABASE_1_CREATE_INTERNAL);
        });
    }

    @Test
    public void createDatabase_notFound_fails() {

        /* mock */
        doThrow(HttpClientErrorException.NotFound.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(DatabaseDto.class));

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            dataServiceGateway.createDatabase(DATABASE_1_CREATE_INTERNAL);
        });
    }

    @Test
    public void updateDatabase_succeeds() throws DataServiceException, DataServiceConnectionException,
            DatabaseNotFoundException {

        /* mock */
        when(dataServiceRestTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.accepted()
                        .build());

        /* test */
        dataServiceGateway.updateDatabase(DATABASE_1_ID, USER_1_UPDATE_PASSWORD_DTO);
    }

    @Test
    public void updateDatabase_connection_fails() {

        /* mock */
        doThrow(HttpServerErrorException.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(DataServiceConnectionException.class, () -> {
            dataServiceGateway.updateDatabase(DATABASE_1_ID, USER_1_UPDATE_PASSWORD_DTO);
        });
    }

    @Test
    public void updateDatabase_unauthorized_fails() {

        /* mock */
        doThrow(HttpClientErrorException.Unauthorized.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(DataServiceException.class, () -> {
            dataServiceGateway.updateDatabase(DATABASE_1_ID, USER_1_UPDATE_PASSWORD_DTO);
        });
    }

    @Test
    public void updateDatabase_notFound_fails() {

        /* mock */
        doThrow(HttpClientErrorException.NotFound.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            dataServiceGateway.updateDatabase(DATABASE_1_ID, USER_1_UPDATE_PASSWORD_DTO);
        });
    }

    @Test
    public void updateDatabase_unexpected_fails() {

        /* mock */
        doThrow(HttpClientErrorException.BadRequest.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(DataServiceException.class, () -> {
            dataServiceGateway.updateDatabase(DATABASE_1_ID, USER_1_UPDATE_PASSWORD_DTO);
        });
    }

    @Test
    public void updateDatabase_responseCode_fails() {

        /* mock */
        when(dataServiceRestTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .build());

        /* test */
        assertThrows(DataServiceException.class, () -> {
            dataServiceGateway.updateDatabase(DATABASE_1_ID, USER_1_UPDATE_PASSWORD_DTO);
        });
    }

    @Test
    public void createTable_succeeds() throws DataServiceException, DataServiceConnectionException,
            DatabaseNotFoundException, TableExistsException {

        /* mock */
        when(dataServiceRestTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED)
                        .build());

        /* test */
        dataServiceGateway.createTable(DATABASE_1_ID, TABLE_1_CREATE_DTO);
    }

    @Test
    public void createTable_connection_fails() {

        /* mock */
        doThrow(HttpServerErrorException.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(DataServiceConnectionException.class, () -> {
            dataServiceGateway.createTable(DATABASE_1_ID, TABLE_1_CREATE_DTO);
        });
    }

    @Test
    public void createTable_unauthorized_fails() {

        /* mock */
        doThrow(HttpClientErrorException.Unauthorized.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(DataServiceException.class, () -> {
            dataServiceGateway.createTable(DATABASE_1_ID, TABLE_1_CREATE_DTO);
        });
    }

    @Test
    public void createTable_notFound_fails() {

        /* mock */
        doThrow(HttpClientErrorException.NotFound.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            dataServiceGateway.createTable(DATABASE_1_ID, TABLE_1_CREATE_DTO);
        });
    }

    @Test
    public void createTable_exists_fails() {

        /* mock */
        doThrow(HttpClientErrorException.Conflict.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(TableExistsException.class, () -> {
            dataServiceGateway.createTable(DATABASE_1_ID, TABLE_1_CREATE_DTO);
        });
    }

    @Test
    public void createTable_unexpected_fails() {

        /* mock */
        doThrow(HttpClientErrorException.BadRequest.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(DataServiceException.class, () -> {
            dataServiceGateway.createTable(DATABASE_1_ID, TABLE_1_CREATE_DTO);
        });
    }

    @Test
    public void createTable_responseCode_fails() {

        /* mock */
        when(dataServiceRestTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .build());

        /* test */
        assertThrows(DataServiceException.class, () -> {
            dataServiceGateway.createTable(DATABASE_1_ID, TABLE_1_CREATE_DTO);
        });
    }

    @Test
    public void deleteTable_succeeds() throws DataServiceException, DataServiceConnectionException,
            TableNotFoundException {

        /* mock */
        when(dataServiceRestTemplate.exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.accepted()
                        .build());

        /* test */
        dataServiceGateway.deleteTable(DATABASE_1_ID, TABLE_1_ID);
    }

    @Test
    public void deleteTable_connection_fails() {

        /* mock */
        doThrow(HttpServerErrorException.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(DataServiceConnectionException.class, () -> {
            dataServiceGateway.deleteTable(DATABASE_1_ID, TABLE_1_ID);
        });
    }

    @Test
    public void deleteTable_unauthorized_fails() {

        /* mock */
        doThrow(HttpClientErrorException.Unauthorized.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(DataServiceException.class, () -> {
            dataServiceGateway.deleteTable(DATABASE_1_ID, TABLE_1_ID);
        });
    }

    @Test
    public void deleteTable_unexpected_fails() {

        /* mock */
        doThrow(HttpClientErrorException.NotFound.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            dataServiceGateway.deleteTable(DATABASE_1_ID, TABLE_1_ID);
        });
    }

    @Test
    public void deleteTable_responseCode_fails() {

        /* mock */
        when(dataServiceRestTemplate.exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .build());

        /* test */
        assertThrows(DataServiceException.class, () -> {
            dataServiceGateway.deleteTable(DATABASE_1_ID, TABLE_1_ID);
        });
    }

    @Test
    public void createView_succeeds() throws DataServiceException, DataServiceConnectionException,
            ColumnNotFoundException {

        /* mock */
        when(dataServiceRestTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(ViewDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED)
                        .body(VIEW_1_DTO));

        /* test */
        dataServiceGateway.createView(DATABASE_1_ID, VIEW_1_CREATE_DTO);
    }

    @Test
    public void createView_connection_fails() {

        /* mock */
        doThrow(HttpServerErrorException.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(ViewDto.class));

        /* test */
        assertThrows(DataServiceConnectionException.class, () -> {
            dataServiceGateway.createView(DATABASE_1_ID, VIEW_1_CREATE_DTO);
        });
    }

    @Test
    public void createView_unauthorized_fails() {

        /* mock */
        doThrow(HttpClientErrorException.Unauthorized.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(ViewDto.class));

        /* test */
        assertThrows(DataServiceException.class, () -> {
            dataServiceGateway.createView(DATABASE_1_ID, VIEW_1_CREATE_DTO);
        });
    }

    @Test
    public void createView_unexpected_fails() {

        /* mock */
        doThrow(HttpClientErrorException.BadRequest.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(ViewDto.class));

        /* test */
        assertThrows(DataServiceException.class, () -> {
            dataServiceGateway.createView(DATABASE_1_ID, VIEW_1_CREATE_DTO);
        });
    }

    @Test
    public void createView_responseCode_fails() {

        /* mock */
        when(dataServiceRestTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(ViewDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .build());

        /* test */
        assertThrows(DataServiceException.class, () -> {
            dataServiceGateway.createView(DATABASE_1_ID, VIEW_1_CREATE_DTO);
        });
    }

    @Test
    public void createView_emptyBody_fails() {

        /* mock */
        when(dataServiceRestTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(ViewDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED)
                        .build());

        /* test */
        assertThrows(DataServiceException.class, () -> {
            dataServiceGateway.createView(DATABASE_1_ID, VIEW_1_CREATE_DTO);
        });
    }

    @Test
    public void deleteView_succeeds() throws DataServiceException, DataServiceConnectionException,
            ViewNotFoundException {

        /* mock */
        when(dataServiceRestTemplate.exchange(anyString(), eq(HttpMethod.DELETE), eq(HttpEntity.EMPTY), eq(Void.class)))
                .thenReturn(ResponseEntity.accepted()
                        .build());

        /* test */
        dataServiceGateway.deleteView(DATABASE_1_ID, VIEW_1_ID);
    }

    @Test
    public void deleteView_connection_fails() {

        /* mock */
        doThrow(HttpServerErrorException.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.DELETE), eq(HttpEntity.EMPTY), eq(Void.class));

        /* test */
        assertThrows(DataServiceConnectionException.class, () -> {
            dataServiceGateway.deleteView(DATABASE_1_ID, VIEW_1_ID);
        });
    }

    @Test
    public void deleteView_unauthorized_fails() {

        /* mock */
        doThrow(HttpClientErrorException.Unauthorized.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.DELETE), eq(HttpEntity.EMPTY), eq(Void.class));

        /* test */
        assertThrows(DataServiceException.class, () -> {
            dataServiceGateway.deleteView(DATABASE_1_ID, VIEW_1_ID);
        });
    }

    @Test
    public void deleteView_notFound_fails() {

        /* mock */
        doThrow(HttpClientErrorException.NotFound.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.DELETE), eq(HttpEntity.EMPTY), eq(Void.class));

        /* test */
        assertThrows(ViewNotFoundException.class, () -> {
            dataServiceGateway.deleteView(DATABASE_1_ID, VIEW_1_ID);
        });
    }

    @Test
    public void deleteView_responseCode_fails() {

        /* mock */
        when(dataServiceRestTemplate.exchange(anyString(), eq(HttpMethod.DELETE), eq(HttpEntity.EMPTY), eq(Void.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .build());

        /* test */
        assertThrows(DataServiceException.class, () -> {
            dataServiceGateway.deleteView(DATABASE_1_ID, VIEW_1_ID);
        });
    }

    @Test
    public void findQuery_succeeds() throws DataServiceException, DataServiceConnectionException,
            QueryNotFoundException {

        /* mock */
        when(dataServiceRestTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(QueryDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .body(QUERY_1_DTO));

        /* test */
        dataServiceGateway.findQuery(DATABASE_1_ID, QUERY_1_ID);
    }

    @Test
    public void findQuery_connection_fails() {

        /* mock */
        doThrow(HttpServerErrorException.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(QueryDto.class));

        /* test */
        assertThrows(DataServiceConnectionException.class, () -> {
            dataServiceGateway.findQuery(DATABASE_1_ID, QUERY_1_ID);
        });
    }

    @Test
    public void findQuery_unauthorized_fails() {

        /* mock */
        doThrow(HttpClientErrorException.Unauthorized.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(QueryDto.class));

        /* test */
        assertThrows(DataServiceException.class, () -> {
            dataServiceGateway.findQuery(DATABASE_1_ID, QUERY_1_ID);
        });
    }

    @Test
    public void findQuery_notFound_fails() {

        /* mock */
        doThrow(HttpClientErrorException.NotFound.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(QueryDto.class));

        /* test */
        assertThrows(QueryNotFoundException.class, () -> {
            dataServiceGateway.findQuery(DATABASE_1_ID, QUERY_1_ID);
        });
    }

    @Test
    public void findQuery_notAcceptable_fails() {

        /* mock */
        doThrow(HttpClientErrorException.NotAcceptable.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(QueryDto.class));

        /* test */
        assertThrows(DataServiceException.class, () -> {
            dataServiceGateway.findQuery(DATABASE_1_ID, QUERY_1_ID);
        });
    }

    @Test
    public void findQuery_responseCode_fails() {

        /* mock */
        when(dataServiceRestTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(QueryDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .build());

        /* test */
        assertThrows(DataServiceException.class, () -> {
            dataServiceGateway.findQuery(DATABASE_1_ID, QUERY_1_ID);
        });
    }

    @Test
    public void getTableSchemas_succeeds() throws DataServiceException, DataServiceConnectionException, TableNotFoundException {

        /* mock */
        when(dataServiceRestTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(TableDto[].class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .body(new TableDto[]{}));

        /* test */
        dataServiceGateway.getTableSchemas(DATABASE_1_ID);
    }

    @Test
    public void getTableSchemas_connection_fails() {

        /* mock */
        doThrow(HttpServerErrorException.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(TableDto[].class));

        /* test */
        assertThrows(DataServiceConnectionException.class, () -> {
            dataServiceGateway.getTableSchemas(DATABASE_1_ID);
        });
    }

    @Test
    public void getTableSchemas_unauthorized_fails() {

        /* mock */
        doThrow(HttpClientErrorException.Unauthorized.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(TableDto[].class));

        /* test */
        assertThrows(DataServiceException.class, () -> {
            dataServiceGateway.getTableSchemas(DATABASE_1_ID);
        });
    }

    @Test
    public void getTableSchemas_notFound_fails() {

        /* mock */
        doThrow(HttpClientErrorException.NotFound.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(TableDto[].class));

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            dataServiceGateway.getTableSchemas(DATABASE_1_ID);
        });
    }

    @Test
    public void getTableSchemas_responseCode_fails() {

        /* mock */
        when(dataServiceRestTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(TableDto[].class)))
                .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .build());

        /* test */
        assertThrows(DataServiceException.class, () -> {
            dataServiceGateway.getTableSchemas(DATABASE_1_ID);
        });
    }

    @Test
    public void getTableSchemas_emptyBody_fails() {

        /* mock */
        when(dataServiceRestTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(TableDto[].class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .build());

        /* test */
        assertThrows(DataServiceException.class, () -> {
            dataServiceGateway.getTableSchemas(DATABASE_1_ID);
        });
    }

    @Test
    public void getViewSchemas_succeeds() throws DataServiceException, DataServiceConnectionException,
            ViewNotFoundException {

        /* mock */
        when(dataServiceRestTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(ViewDto[].class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .body(new ViewDto[]{}));

        /* test */
        dataServiceGateway.getViewSchemas(DATABASE_1_ID);
    }

    @Test
    public void getViewSchemas_connection_fails() {

        /* mock */
        doThrow(HttpServerErrorException.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(ViewDto[].class));

        /* test */
        assertThrows(DataServiceConnectionException.class, () -> {
            dataServiceGateway.getViewSchemas(DATABASE_1_ID);
        });
    }

    @Test
    public void getViewSchemas_unauthorized_fails() {

        /* mock */
        doThrow(HttpClientErrorException.Unauthorized.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(ViewDto[].class));

        /* test */
        assertThrows(DataServiceException.class, () -> {
            dataServiceGateway.getViewSchemas(DATABASE_1_ID);
        });
    }

    @Test
    public void getViewSchemas_notFound_fails() {

        /* mock */
        doThrow(HttpClientErrorException.NotFound.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(ViewDto[].class));

        /* test */
        assertThrows(ViewNotFoundException.class, () -> {
            dataServiceGateway.getViewSchemas(DATABASE_1_ID);
        });
    }

    @Test
    public void getViewSchemas_responseCode_fails() {

        /* mock */
        when(dataServiceRestTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(ViewDto[].class)))
                .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .build());

        /* test */
        assertThrows(DataServiceException.class, () -> {
            dataServiceGateway.getViewSchemas(DATABASE_1_ID);
        });
    }

    @Test
    public void getViewSchemas_emptyBody_fails() {

        /* mock */
        when(dataServiceRestTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(ViewDto[].class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .build());

        /* test */
        assertThrows(DataServiceException.class, () -> {
            dataServiceGateway.getViewSchemas(DATABASE_1_ID);
        });
    }

    @Test
    public void getTableStatistics_succeeds() throws DataServiceException, DataServiceConnectionException,
            TableNotFoundException {

        /* mock */
        when(dataServiceRestTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(TableStatisticDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .body(TABLE_8_STATISTIC_DTO));

        /* test */
        dataServiceGateway.getTableStatistics(DATABASE_3_ID, TABLE_8_ID);
    }

    @Test
    public void getTableStatistics_connection_fails() {

        /* mock */
        doThrow(HttpServerErrorException.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(TableStatisticDto.class));

        /* test */
        assertThrows(DataServiceConnectionException.class, () -> {
            dataServiceGateway.getTableStatistics(DATABASE_3_ID, TABLE_8_ID);
        });
    }

    @Test
    public void getTableStatistics_unauthorized_fails() {

        /* mock */
        doThrow(HttpClientErrorException.Unauthorized.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(TableStatisticDto.class));

        /* test */
        assertThrows(DataServiceException.class, () -> {
            dataServiceGateway.getTableStatistics(DATABASE_3_ID, TABLE_8_ID);
        });
    }

    @Test
    public void getTableStatistics_notFound_fails() {

        /* mock */
        doThrow(HttpClientErrorException.NotFound.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(TableStatisticDto.class));

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            dataServiceGateway.getTableStatistics(DATABASE_3_ID, TABLE_8_ID);
        });
    }

    @Test
    public void getTableStatistics_responseCode_fails() {

        /* mock */
        when(dataServiceRestTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(TableStatisticDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .build());

        /* test */
        assertThrows(DataServiceException.class, () -> {
            dataServiceGateway.getTableStatistics(DATABASE_3_ID, TABLE_8_ID);
        });
    }

    @Test
    public void updateTable_succeeds() throws DataServiceException, DataServiceConnectionException,
            DatabaseNotFoundException {

        /* mock */
        when(dataServiceRestTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.ACCEPTED)
                        .build());

        /* test */
        dataServiceGateway.updateTable(DATABASE_3_ID, TABLE_8_ID, TABLE_8_UPDATE_DTO);
    }

    @Test
    public void updateTable_connection_fails() {

        /* mock */
        doThrow(HttpServerErrorException.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(DataServiceConnectionException.class, () -> {
            dataServiceGateway.updateTable(DATABASE_3_ID, TABLE_8_ID, TABLE_8_UPDATE_DTO);
        });
    }

    @Test
    public void updateTable_notFound_fails() {

        /* mock */
        doThrow(HttpClientErrorException.NotFound.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            dataServiceGateway.updateTable(DATABASE_3_ID, TABLE_8_ID, TABLE_8_UPDATE_DTO);
        });
    }

    @Test
    public void updateTable_malformed_fails() {

        /* mock */
        doThrow(HttpClientErrorException.BadRequest.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(DataServiceException.class, () -> {
            dataServiceGateway.updateTable(DATABASE_3_ID, TABLE_8_ID, TABLE_8_UPDATE_DTO);
        });
    }

    @Test
    public void updateTable_unauthorized_fails() {

        /* mock */
        doThrow(HttpClientErrorException.Unauthorized.class)
                .when(dataServiceRestTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(DataServiceException.class, () -> {
            dataServiceGateway.updateTable(DATABASE_3_ID, TABLE_8_ID, TABLE_8_UPDATE_DTO);
        });
    }

    @Test
    public void updateTable_responseCode_fails() {

        /* mock */
        when(dataServiceRestTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .build());

        /* test */
        assertThrows(DataServiceException.class, () -> {
            dataServiceGateway.updateTable(DATABASE_3_ID, TABLE_8_ID, TABLE_8_UPDATE_DTO);
        });
    }

}
