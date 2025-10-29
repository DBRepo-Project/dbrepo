package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.View;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.gateway.DataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.gateway.SearchServiceGateway;
import at.ac.tuwien.ifs.dbrepo.repository.ContainerRepository;
import at.ac.tuwien.ifs.dbrepo.repository.DatabaseRepository;
import at.ac.tuwien.ifs.dbrepo.repository.LicenseRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest
@Disabled("CI/CD")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
public class ViewServicePersistenceTest extends BaseTest {

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private ViewService viewService;

    @MockitoBean
    private DataServiceGateway dataServiceGateway;

    @MockitoBean
    private SearchServiceGateway searchServiceGateway;

    @BeforeEach
    public void beforeEach() {
        /* metadata database */
        licenseRepository.save(LICENSE_1);
        containerRepository.save(CONTAINER_1);
        databaseRepository.saveAll(List.of(DATABASE_1, DATABASE_2, DATABASE_3));
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
                .thenReturn(DATABASE_1_BRIEF_DTO);

        /* test */
        viewService.delete(VIEW_1);
        assertThrows(ViewNotFoundException.class, () -> {
            viewService.findById(DATABASE_1, VIEW_1_ID);
        });
    }

}
