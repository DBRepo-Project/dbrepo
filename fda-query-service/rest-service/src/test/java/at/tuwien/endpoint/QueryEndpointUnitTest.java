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
import at.tuwien.repository.jpa.DatabaseRepository;
import at.tuwien.repository.jpa.ImageRepository;
import at.tuwien.service.QueryService;
import at.tuwien.service.StoreService;
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
    public void execute_forbiddenKeyword_fails() throws UserNotFoundException, QueryStoreException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, ColumnParseException,
            DatabaseNotFoundException, ImageNotSupportedException, ContainerNotFoundException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement("SELECT w.* FROM `weather_aus` w")
                .build();
        final Long page = 0L;
        final Long size = 2L;
        final SortType sortDirection = SortType.ASC;
        final String sortColumn = "location";

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(queryService.execute(CONTAINER_1_ID, DATABASE_1_ID, request, QueryTypeDto.QUERY,
                USER_1_PRINCIPAL, page, size, sortDirection, sortColumn))
                .thenReturn(QUERY_1_RESULT_DTO);

        /* test */
        assertThrows(QueryMalformedException.class, () -> {
            queryEndpoint.execute(CONTAINER_1_ID, DATABASE_1_ID, request, page, size, USER_1_PRINCIPAL, sortDirection,
                    sortColumn);
        });
    }

    @Test
    public void execute_forbiddenKeyword2_fails() throws UserNotFoundException, QueryStoreException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, ColumnParseException,
            DatabaseNotFoundException, ImageNotSupportedException, ContainerNotFoundException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement("SELECT * FROM `weather_aus` w")
                .build();
        final Long page = 0L;
        final Long size = 2L;
        final SortType sortDirection = SortType.ASC;
        final String sortColumn = "location";

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(queryService.execute(CONTAINER_1_ID, DATABASE_1_ID, request, QueryTypeDto.QUERY,
                USER_1_PRINCIPAL, page, size, sortDirection, sortColumn))
                .thenReturn(QUERY_1_RESULT_DTO);

        /* test */
        assertThrows(QueryMalformedException.class, () -> {
            queryEndpoint.execute(CONTAINER_1_ID, DATABASE_1_ID, request, page, size, USER_1_PRINCIPAL, sortDirection,
                    sortColumn);
        });
    }


}
