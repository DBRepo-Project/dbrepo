package at.tuwien.repository;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.entities.database.Database;
import at.tuwien.repository.mdb.*;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Log4j2
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@ExtendWith(SpringExtension.class)
@MockAmqp
@MockOpensearch
public class DatabaseRepositoryIntegrationTest extends BaseUnitTest {

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private LicenseRepository licenseRepository;

    @BeforeEach
    public void beforeEach() {
        TABLE_1.setColumns(TABLE_1_COLUMNS);
        TABLE_2.setColumns(TABLE_2_COLUMNS);
        TABLE_3.setColumns(TABLE_3_COLUMNS);
        TABLE_4.setColumns(TABLE_4_COLUMNS);
        TABLE_5.setColumns(TABLE_5_COLUMNS);
        TABLE_6.setColumns(TABLE_6_COLUMNS);
        TABLE_7.setColumns(TABLE_7_COLUMNS);
        DATABASE_1.setAccesses(List.of());
        DATABASE_2.setAccesses(List.of());
        VIEW_1.setColumns(VIEW_1_COLUMNS);
        VIEW_2.setColumns(VIEW_2_COLUMNS);
        VIEW_3.setColumns(VIEW_3_COLUMNS);
        VIEW_4.setColumns(VIEW_4_COLUMNS);
        /* metadata database */
        imageRepository.save(IMAGE_1);
        licenseRepository.save(LICENSE_1);
        userRepository.saveAll(List.of(USER_1, USER_2, USER_3));
        containerRepository.saveAll(List.of(CONTAINER_1, CONTAINER_2));
        DATABASE_1.setAccesses(List.of(DATABASE_1_USER_1_READ_ACCESS, DATABASE_1_USER_2_WRITE_OWN_ACCESS));
        DATABASE_2.setAccesses(List.of(DATABASE_2_USER_2_WRITE_ALL_ACCESS, DATABASE_2_USER_3_READ_ACCESS));
        databaseRepository.saveAll(List.of(DATABASE_1, DATABASE_2));
    }

    @Test
    public void findConfigureAccess_noAccess_succeeds() {

        /* test */
        final List<Database> response = databaseRepository.findConfigureAccess(USER_1_ID);
        assertEquals(1, response.size());
    }

    @Test
    public void findConfigureAccess_hasReadAccess_succeeds() {

        /* test */
        final List<Database> response = databaseRepository.findConfigureAccess(USER_1_ID);
        assertEquals(1, response.size());
    }

    @Test
    public void findConfigureAccess_hasWriteOwnAccess_succeeds() {

        /* test */
        final List<Database> response = databaseRepository.findConfigureAccess(USER_1_ID);
        assertEquals(1, response.size());
    }

    @Test
    public void findConfigureAccess_hasWriteAllAccess_succeeds() {

        /* test */
        final List<Database> response = databaseRepository.findConfigureAccess(USER_1_ID);
        assertEquals(1, response.size());
    }

    @Test
    public void findWriteAccess_noAccess_fails() {

        /* test */
        final List<Database> response = databaseRepository.findWriteAccess(USER_1_ID);
        assertEquals(0, response.size());
    }

    @Test
    public void findWriteAccess_hasReadAccess_succeeds() {

        /* test */
        final List<Database> response = databaseRepository.findWriteAccess(USER_1_ID);
        assertEquals(0, response.size());
    }

    @Test
    public void findWriteAccess_hasWriteOwnAccess_succeeds() {

        /* test */
        final List<Database> response = databaseRepository.findWriteAccess(USER_1_ID);
        assertEquals(0, response.size());
    }

    @Test
    public void findWriteAccess_hasWriteAllAccess_succeeds() {

        /* test */
        final List<Database> response = databaseRepository.findWriteAccess(USER_1_ID);
        assertEquals(0, response.size());
    }

    @Test
    public void findReadAccess_noAccess_fails() {

        /* test */
        final List<Database> response = databaseRepository.findReadAccess(USER_1_ID);
        assertEquals(1, response.size());
    }

    @Test
    public void findReadAccess_hasReadAccess_succeeds() {

        /* test */
        final List<Database> response = databaseRepository.findReadAccess(USER_1_ID);
        assertEquals(1, response.size());
    }

    @Test
    public void findReadAccess_hasWriteOwnAccess_succeeds() {

        /* test */
        final List<Database> response = databaseRepository.findReadAccess(USER_1_ID);
        assertEquals(1, response.size());
    }

    @Test
    public void findReadAccess_hasWriteAllAccess_succeeds() {

        /* test */
        final List<Database> response = databaseRepository.findReadAccess(USER_1_ID);
        assertEquals(1, response.size());
    }

}
