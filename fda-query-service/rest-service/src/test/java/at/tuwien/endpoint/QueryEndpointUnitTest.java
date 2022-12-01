package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.SortType;
import at.tuwien.api.database.query.ExecuteStatementDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.database.query.QueryTypeDto;
import at.tuwien.config.ReadyConfig;
import at.tuwien.exception.*;
import at.tuwien.listener.impl.RabbitMqListenerImpl;
import at.tuwien.repository.jpa.ContainerRepository;
import at.tuwien.repository.jpa.DatabaseAccessRepository;
import at.tuwien.repository.jpa.DatabaseRepository;
import at.tuwien.repository.jpa.ImageRepository;
import at.tuwien.service.QueryService;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.apache.http.auth.BasicUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

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

    @Autowired
    private QueryEndpoint queryEndpoint;

    @Test
    public void execute_anonymized_fails() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException, SortException, NotAllowedException,
            PaginationException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_1_STATEMENT)
                .build();
        final Principal principal = new BasicUserPrincipal(USER_2_USERNAME);
        final Long page = 0L;
        final Long size = 2L;
        final SortType sortDirection = SortType.ASC;
        final String sortColumn = "location";

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(databaseAccessRepository.findByDatabaseIdAndUsername(DATABASE_1_ID, USER_2_USERNAME))
                .thenReturn(Optional.empty());
        when(queryService.execute(CONTAINER_1_ID, DATABASE_1_ID, request, QueryTypeDto.QUERY,
                principal, page, size, sortDirection, sortColumn))
                .thenReturn(QUERY_1_RESULT_DTO);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            queryEndpoint.execute(CONTAINER_1_ID, DATABASE_1_ID, request, page, size, principal, sortDirection,
                    sortColumn);
        });
    }

    @Test
    public void execute_read_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException, SortException, NotAllowedException,
            PaginationException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_1_STATEMENT)
                .build();
        final Principal principal = new BasicUserPrincipal(USER_2_USERNAME);
        final Long page = 0L;
        final Long size = 2L;
        final SortType sortDirection = SortType.ASC;
        final String sortColumn = "location";

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(databaseAccessRepository.findByDatabaseIdAndUsername(DATABASE_1_ID, USER_2_USERNAME))
                .thenReturn(Optional.of(DATABASE_1_ACCESS));
        when(queryService.execute(CONTAINER_1_ID, DATABASE_1_ID, request, QueryTypeDto.QUERY,
                principal, page, size, sortDirection, sortColumn))
                .thenReturn(QUERY_1_RESULT_DTO);

        /* test */
        final ResponseEntity<QueryResultDto> response = queryEndpoint.execute(CONTAINER_1_ID, DATABASE_1_ID, request,
                page, size, principal, sortDirection, sortColumn);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(QUERY_1_RESULT_ID, response.getBody().getId());
        assertEquals(QUERY_1_RESULT_NUMBER, response.getBody().getResultNumber());
        assertEquals(QUERY_1_RESULT_NUMBER, response.getBody().getResult().size());
        assertEquals(QUERY_1_RESULT_RESULT, response.getBody().getResult());
    }

    @Test
    public void execute_writeOwn_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException, SortException, NotAllowedException,
            PaginationException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_1_STATEMENT)
                .build();
        final Principal principal = new BasicUserPrincipal(USER_2_USERNAME);
        final Long page = 0L;
        final Long size = 2L;
        final SortType sortDirection = SortType.ASC;
        final String sortColumn = "location";

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(databaseAccessRepository.findByDatabaseIdAndUsername(DATABASE_1_ID, USER_2_USERNAME))
                .thenReturn(Optional.of(DATABASE_2_ACCESS));
        when(queryService.execute(CONTAINER_1_ID, DATABASE_1_ID, request, QueryTypeDto.QUERY,
                principal, page, size, sortDirection, sortColumn))
                .thenReturn(QUERY_1_RESULT_DTO);

        /* test */
        final ResponseEntity<QueryResultDto> response = queryEndpoint.execute(CONTAINER_1_ID, DATABASE_1_ID, request,
                page, size, principal, sortDirection, sortColumn);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(QUERY_1_RESULT_ID, response.getBody().getId());
        assertEquals(QUERY_1_RESULT_NUMBER, response.getBody().getResultNumber());
        assertEquals(QUERY_1_RESULT_NUMBER, response.getBody().getResult().size());
        assertEquals(QUERY_1_RESULT_RESULT, response.getBody().getResult());
    }

    @Test
    public void execute_writeAll_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException, SortException, NotAllowedException,
            PaginationException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_1_STATEMENT)
                .build();
        final Principal principal = new BasicUserPrincipal(USER_2_USERNAME);
        final Long page = 0L;
        final Long size = 2L;
        final SortType sortDirection = SortType.ASC;
        final String sortColumn = "location";

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(databaseAccessRepository.findByDatabaseIdAndUsername(DATABASE_1_ID, USER_2_USERNAME))
                .thenReturn(Optional.of(DATABASE_3_ACCESS));
        when(queryService.execute(CONTAINER_1_ID, DATABASE_1_ID, request, QueryTypeDto.QUERY,
                principal, page, size, sortDirection, sortColumn))
                .thenReturn(QUERY_1_RESULT_DTO);

        /* test */
        final ResponseEntity<QueryResultDto> response = queryEndpoint.execute(CONTAINER_1_ID, DATABASE_1_ID, request,
                page, size, principal, sortDirection, sortColumn);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(QUERY_1_RESULT_ID, response.getBody().getId());
        assertEquals(QUERY_1_RESULT_NUMBER, response.getBody().getResultNumber());
        assertEquals(QUERY_1_RESULT_NUMBER, response.getBody().getResult().size());
        assertEquals(QUERY_1_RESULT_RESULT, response.getBody().getResult());
    }

    @Test
    public void execute_owner_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException, SortException, NotAllowedException,
            PaginationException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_1_STATEMENT)
                .build();
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);
        final Long page = 0L;
        final Long size = 2L;
        final SortType sortDirection = SortType.ASC;
        final String sortColumn = "location";

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(databaseAccessRepository.findByDatabaseIdAndUsername(DATABASE_1_ID, USER_1_USERNAME))
                .thenReturn(Optional.of(DATABASE_3_ACCESS));
        when(queryService.execute(CONTAINER_1_ID, DATABASE_1_ID, request, QueryTypeDto.QUERY,
                principal, page, size, sortDirection, sortColumn))
                .thenReturn(QUERY_1_RESULT_DTO);

        /* test */
        final ResponseEntity<QueryResultDto> response = queryEndpoint.execute(CONTAINER_1_ID, DATABASE_1_ID, request,
                page, size, principal, sortDirection, sortColumn);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(QUERY_1_RESULT_ID, response.getBody().getId());
        assertEquals(QUERY_1_RESULT_NUMBER, response.getBody().getResultNumber());
        assertEquals(QUERY_1_RESULT_NUMBER, response.getBody().getResult().size());
        assertEquals(QUERY_1_RESULT_RESULT, response.getBody().getResult());
    }

}
