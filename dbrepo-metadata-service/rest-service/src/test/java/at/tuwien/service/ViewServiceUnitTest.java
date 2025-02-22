package at.tuwien.service;

import at.tuwien.api.database.CreateViewDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.View;
import at.tuwien.exception.*;
import at.tuwien.gateway.DataServiceGateway;
import at.tuwien.gateway.SearchServiceGateway;
import at.tuwien.repository.DatabaseRepository;
import at.tuwien.test.AbstractUnitTest;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Log4j2
@Testcontainers
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class ViewServiceUnitTest extends AbstractUnitTest {

    @MockBean
    private DataServiceGateway dataServiceGateway;

    @MockBean
    private SearchServiceGateway searchServiceGateway;

    @MockBean
    private DatabaseRepository databaseRepository;

    @Autowired
    private ViewService viewService;

    @BeforeEach
    public void beforeEach() {
        genesis();
    }

    @Test
    public void create_succeeds() throws MalformedException, DataServiceException, DataServiceConnectionException,
            DatabaseNotFoundException, SearchServiceException, SearchServiceConnectionException {
        final CreateViewDto request = CreateViewDto.builder()
                .name(VIEW_1_NAME)
                .query(VIEW_1_QUERY)
                .isPublic(VIEW_1_PUBLIC)
                .build();

        /* mock */
        when(dataServiceGateway.createView(DATABASE_1_ID, request))
                .thenReturn(VIEW_1_DTO);
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_1_BRIEF_DTO);

        /* test */
        final View response = viewService.create(DATABASE_1, USER_1, request);
        assertEquals(VIEW_1_NAME, response.getName());
        assertEquals(VIEW_1_INTERNAL_NAME, response.getInternalName());
        assertEquals(VIEW_1_QUERY, response.getQuery());
    }

    @Test
    public void findById_succeeds() throws ViewNotFoundException {

        /* test */
        final View response = viewService.findById(DATABASE_1, VIEW_1_ID);
        assertEquals(VIEW_1_ID, response.getId());
        assertEquals(VIEW_1_NAME, response.getName());
        assertEquals(VIEW_1_INTERNAL_NAME, response.getInternalName());
        assertEquals(VIEW_1_QUERY, response.getQuery());

    }

    @Test
    public void findById_notFound_fails() {

        /* test */
        assertThrows(ViewNotFoundException.class, () -> {
            viewService.findById(DATABASE_1, UUID.randomUUID());
        });
    }

    @Test
    public void findAll_public_succeeds() {

        /* test */
        viewService.findAll(DATABASE_1, null);
    }

    @Test
    public void findAll_publicAndPrivate_succeeds() {

        /* test */
        viewService.findAll(DATABASE_1, USER_1);
    }

    @Test
    public void delete_succeeds() throws DataServiceException, DataServiceConnectionException,
            DatabaseNotFoundException, ViewNotFoundException, SearchServiceException, SearchServiceConnectionException {

        /* mock */
        doNothing()
                .when(dataServiceGateway)
                .deleteView(DATABASE_1_ID, VIEW_1_ID);
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_1_BRIEF_DTO);

        /* test */
        viewService.delete(VIEW_1);
    }

    @Test
    public void delete_dataServiceException_fails() throws DataServiceException, DataServiceConnectionException,
            ViewNotFoundException {

        /* mock */
        doThrow(DataServiceException.class)
                .when(dataServiceGateway)
                .deleteView(DATABASE_1_ID, VIEW_1_ID);

        /* test */
        assertThrows(DataServiceException.class, () -> {
            viewService.delete(VIEW_1);
        });
    }

    @Test
    public void delete_dataServiceConnection_fails() throws DataServiceException, DataServiceConnectionException,
            ViewNotFoundException {

        /* mock */
        doThrow(DataServiceConnectionException.class)
                .when(dataServiceGateway)
                .deleteView(DATABASE_1_ID, VIEW_1_ID);

        /* test */
        assertThrows(DataServiceConnectionException.class, () -> {
            viewService.delete(VIEW_1);
        });
    }

    @Test
    public void delete_searchServiceError_fails() throws DataServiceException, DataServiceConnectionException,
            ViewNotFoundException, DatabaseNotFoundException, SearchServiceException, SearchServiceConnectionException {

        /* mock */
        doNothing()
                .when(dataServiceGateway)
                .deleteView(DATABASE_1_ID, VIEW_1_ID);
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        doThrow(SearchServiceException.class)
                .when(searchServiceGateway)
                .update(any(Database.class));

        /* test */
        assertThrows(SearchServiceException.class, () -> {
            viewService.delete(VIEW_1);
        });
    }

    @Test
    public void delete_searchServiceConnection_fails() throws DataServiceException, DataServiceConnectionException,
            DatabaseNotFoundException, ViewNotFoundException, SearchServiceException, SearchServiceConnectionException {

        /* mock */
        doNothing()
                .when(dataServiceGateway)
                .deleteView(DATABASE_1_ID, VIEW_1_ID);
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        doThrow(SearchServiceConnectionException.class)
                .when(searchServiceGateway)
                .update(any(Database.class));

        /* test */
        assertThrows(SearchServiceConnectionException.class, () -> {
            viewService.delete(VIEW_1);
        });
    }

    @Test
    public void delete_searchServiceNotFound_fails() throws DataServiceException, DataServiceConnectionException,
            ViewNotFoundException, DatabaseNotFoundException, SearchServiceException, SearchServiceConnectionException {

        /* mock */
        doNothing()
                .when(dataServiceGateway)
                .deleteView(DATABASE_1_ID, VIEW_1_ID);
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        doThrow(DatabaseNotFoundException.class)
                .when(searchServiceGateway)
                .update(any(Database.class));

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            viewService.delete(VIEW_1);
        });
    }

}
