package at.tuwien.service;

import at.tuwien.entities.container.Container;
import at.tuwien.entities.database.Database;
import at.tuwien.exception.DatabaseNotFoundException;
import at.tuwien.repository.ContainerRepository;
import at.tuwien.repository.DatabaseRepository;
import at.tuwien.repository.LicenseRepository;
import at.tuwien.repository.UserRepository;
import at.tuwien.test.AbstractUnitTest;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Log4j2
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
public class DatabaseServicePersistenceTest extends AbstractUnitTest {

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private AccessService accessService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @BeforeEach
    public void beforeEach() {
        genesis();
        /* metadata database */
        licenseRepository.save(LICENSE_1);
        userRepository.saveAll(List.of(USER_1, USER_2, USER_3, USER_4, USER_5));
        containerRepository.saveAll(List.of(CONTAINER_1, CONTAINER_2, CONTAINER_3, CONTAINER_4));
        databaseRepository.saveAll(List.of(DATABASE_1, DATABASE_2, DATABASE_3, DATABASE_4));
    }

    @Test
    @Transactional(readOnly = true)
    public void findById_succeeds() throws DatabaseNotFoundException {

        /* test */
        final Database response = databaseService.findById(DATABASE_1_ID);
        assertEquals(DATABASE_1, response);
    }

    @Test
    @Transactional(readOnly = true)
    public void findAllPublicByInternalName_succeeds() {

        /* test */
        final List<Database> response = databaseService.findAllPublicByInternalName(DATABASE_3_INTERNALNAME);
        assertEquals(1, response.size());
        assertEquals(DATABASE_3, response.get(0));
    }

    @Test
    @Transactional(readOnly = true)
    public void findAllPublicByInternalName_privateEmpty_succeeds() {

        /* test */
        final List<Database> response = databaseService.findAllPublicByInternalName(DATABASE_1_INTERNALNAME);
        assertEquals(0, response.size());
    }

    @Test
    @Transactional(readOnly = true)
    public void findAllPublicOrReadAccess_privateNoAccessEmpty_succeeds() {

        /* test */
        final List<Database> response = databaseService.findAllPublicOrReadAccess(USER_4_ID);
        assertEquals(3, response.size());
        assertEquals(DATABASE_4, response.get(0));
        assertEquals(DATABASE_3, response.get(1));
        assertEquals(DATABASE_2, response.get(2));
    }

    @Test
    @Transactional(readOnly = true)
    public void findAllPublicOrReadAccess_privateAccess_succeeds() {

        /* test */
        final List<Database> response = databaseService.findAllPublicOrReadAccess(USER_2_ID);
        assertEquals(4, response.size());
        assertEquals(DATABASE_4, response.get(0));
        assertEquals(DATABASE_3, response.get(1));
        assertEquals(DATABASE_2, response.get(2));
        assertEquals(DATABASE_1, response.get(3));
    }

}
