package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.entities.database.Database;
import at.tuwien.exception.DatabaseNotFoundException;
import at.tuwien.repository.mdb.*;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
@Testcontainers
@MockAmqp
@MockOpensearch
public class DatabaseServiceIntegrationTest extends BaseUnitTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private DatabaseService databaseService;

    @BeforeEach
    public void beforeEach() {
        TABLE_1.setColumns(TABLE_1_COLUMNS);
        TABLE_2.setColumns(TABLE_2_COLUMNS);
        TABLE_3.setColumns(TABLE_3_COLUMNS);
        TABLE_4.setColumns(TABLE_4_COLUMNS);
        /* metadata database */
        userRepository.save(USER_1);
        imageRepository.save(IMAGE_1);
        licenseRepository.save(LICENSE_1);
        containerRepository.save(CONTAINER_1);
        databaseRepository.save(DATABASE_1);
    }

    @Test
    public void find_succeeds() throws DatabaseNotFoundException {

        /* test */
        final Database response = databaseService.find(DATABASE_1_ID);
        assertEquals(DATABASE_1_ID, response.getId());
    }

    @Test
    public void find_fails() {

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            databaseService.find(DATABASE_2_ID);
        });
    }

    @Test
    public void findByInternalName_succeeds() throws DatabaseNotFoundException {

        /* test */
        final Database response = databaseService.findByInternalName(DATABASE_1_INTERNALNAME);
        assertEquals(DATABASE_1_ID, response.getId());
    }

    @Test
    public void findByInternalName_fails() {

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            databaseService.findByInternalName(DATABASE_2_INTERNALNAME);
        });
    }

    @Test
    public void findAll_succeeds() {

        /* test */
        final List<Database> response = databaseService.findAll();
        assertEquals(1, response.size());
        final Database database0 = response.get(0);
        assertEquals(DATABASE_1_ID, database0.getId());
    }

}
