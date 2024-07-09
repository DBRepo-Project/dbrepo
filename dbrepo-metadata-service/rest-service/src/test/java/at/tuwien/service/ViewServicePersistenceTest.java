package at.tuwien.service;

import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.View;
import at.tuwien.exception.*;
import at.tuwien.gateway.DataServiceGateway;
import at.tuwien.gateway.SearchServiceGateway;
import at.tuwien.repository.ContainerRepository;
import at.tuwien.repository.DatabaseRepository;
import at.tuwien.repository.LicenseRepository;
import at.tuwien.repository.UserRepository;
import at.tuwien.test.AbstractUnitTest;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@Disabled("CI/CD")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
public class ViewServicePersistenceTest extends AbstractUnitTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private ViewService viewService;

    @MockBean
    private DataServiceGateway dataServiceGateway;

    @MockBean
    private SearchServiceGateway searchServiceGateway;

    @BeforeEach
    public void beforeEach() {
        genesis();
        /* metadata database */
        licenseRepository.save(LICENSE_1);
        containerRepository.save(CONTAINER_1);
        userRepository.saveAll(List.of(USER_1, USER_2, USER_3));
        databaseRepository.save(DATABASE_1);
    }

    @Test
    public void findById_succeeds() throws ViewNotFoundException {

        /* test */
        final View response = viewService.findById(DATABASE_1, VIEW_1_ID);
        assertEquals(VIEW_1_ID, response.getId());
        assertEquals(VIEW_1_NAME, response.getName());
        assertEquals(VIEW_1_INTERNAL_NAME, response.getInternalName());
        assertEquals(VIEW_1_QUERY, response.getQuery());
        assertEquals(VIEW_1_COLUMNS.size(), response.getColumns().size());
    }

    @Test
    public void delete_succeeds() throws SearchServiceException, DataServiceException, DataServiceConnectionException,
            DatabaseNotFoundException, SearchServiceConnectionException, ViewNotFoundException {

        /* mock */
        doNothing()
                .when(dataServiceGateway)
                .deleteView(DATABASE_1_ID, VIEW_1_ID);
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_1_DTO);

        /* test */
        viewService.delete(VIEW_1);
    }

}
