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
public class DatabaseRepositoryUnitTest extends BaseUnitTest {

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private DatabaseAccessRepository databaseAccessRepository;

    @BeforeEach
    public void beforeEach() {
        imageRepository.save(IMAGE_1_SIMPLE);
        containerRepository.save(CONTAINER_1_SIMPLE);
        containerRepository.save(CONTAINER_2_SIMPLE);
        databaseRepository.save(DATABASE_1_SIMPLE);
        databaseRepository.save(DATABASE_2_SIMPLE);
    }

    @Test
    public void findConfigureAccess_noAccess_succeeds() {

        /* mock */
        databaseAccessRepository.deleteAll();

        /* test */
        final List<Database> response = databaseRepository.findConfigureAccess(USER_1_ID);
        assertEquals(1, response.size());
        assertEquals(DATABASE_1_ID, response.get(0).getId());
    }

    @Test
    public void findConfigureAccess_hasReadAccess_succeeds() {

        /* mock */
        databaseAccessRepository.deleteAll();
        databaseAccessRepository.save(DATABASE_1_USER_1_READ_ACCESS);

        /* test */
        final List<Database> response = databaseRepository.findConfigureAccess(USER_1_ID);
        assertEquals(1, response.size());
        assertEquals(DATABASE_1_ID, response.get(0).getId());
    }

    @Test
    public void findConfigureAccess_hasWriteOwnAccess_succeeds() {

        /* mock */
        databaseAccessRepository.deleteAll();
        databaseAccessRepository.save(DATABASE_1_USER_1_WRITE_OWN_ACCESS);

        /* test */
        final List<Database> response = databaseRepository.findConfigureAccess(USER_1_ID);
        assertEquals(1, response.size());
        assertEquals(DATABASE_1_ID, response.get(0).getId());
    }

    @Test
    public void findConfigureAccess_hasWriteAllAccess_succeeds() {

        /* mock */
        databaseAccessRepository.deleteAll();
        databaseAccessRepository.save(DATABASE_1_USER_1_WRITE_ALL_ACCESS);

        /* test */
        final List<Database> response = databaseRepository.findConfigureAccess(USER_1_ID);
        assertEquals(1, response.size());
        assertEquals(DATABASE_1_ID, response.get(0).getId());
    }

    @Test
    public void findWriteAccess_noAccess_fails() {

        /* mock */
        databaseAccessRepository.deleteAll();

        /* test */
        final List<Database> response = databaseRepository.findWriteAccess(USER_1_ID);
        assertEquals(0, response.size());
    }

    @Test
    public void findWriteAccess_hasReadAccess_succeeds() {

        /* mock */
        databaseAccessRepository.deleteAll();
        databaseAccessRepository.save(DATABASE_2_USER_1_READ_ACCESS);

        /* test */
        final List<Database> response = databaseRepository.findWriteAccess(USER_1_ID);
        assertEquals(0, response.size());
    }

    @Test
    public void findWriteAccess_hasWriteOwnAccess_succeeds() {

        /* mock */
        databaseAccessRepository.deleteAll();
        databaseAccessRepository.save(DATABASE_2_USER_1_WRITE_OWN_ACCESS);

        /* test */
        final List<Database> response = databaseRepository.findWriteAccess(USER_1_ID);
        assertEquals(1, response.size());
        assertEquals(DATABASE_2_ID, response.get(0).getId());
    }

    @Test
    public void findWriteAccess_hasWriteAllAccess_succeeds() {

        /* mock */
        databaseAccessRepository.deleteAll();
        databaseAccessRepository.save(DATABASE_2_USER_1_WRITE_ALL_ACCESS);

        /* test */
        final List<Database> response = databaseRepository.findWriteAccess(USER_1_ID);
        assertEquals(1, response.size());
        assertEquals(DATABASE_2_ID, response.get(0).getId());
    }

    @Test
    public void findReadAccess_noAccess_fails() {

        /* mock */
        databaseAccessRepository.deleteAll();

        /* test */
        final List<Database> response = databaseRepository.findReadAccess(USER_1_ID);
        assertEquals(0, response.size());
    }

    @Test
    public void findReadAccess_hasReadAccess_succeeds() {

        /* mock */
        databaseAccessRepository.deleteAll();
        databaseAccessRepository.save(DATABASE_2_USER_1_READ_ACCESS);

        /* test */
        final List<Database> response = databaseRepository.findReadAccess(USER_1_ID);
        assertEquals(1, response.size());
        assertEquals(DATABASE_2_ID, response.get(0).getId());
    }

    @Test
    public void findReadAccess_hasWriteOwnAccess_succeeds() {

        /* mock */
        databaseAccessRepository.deleteAll();
        databaseAccessRepository.save(DATABASE_2_USER_1_WRITE_OWN_ACCESS);

        /* test */
        final List<Database> response = databaseRepository.findReadAccess(USER_1_ID);
        assertEquals(1, response.size());
        assertEquals(DATABASE_2_ID, response.get(0).getId());
    }

    @Test
    public void findReadAccess_hasWriteAllAccess_succeeds() {

        /* mock */
        databaseAccessRepository.deleteAll();
        databaseAccessRepository.save(DATABASE_2_USER_1_WRITE_ALL_ACCESS);

        /* test */
        final List<Database> response = databaseRepository.findReadAccess(USER_1_ID);
        assertEquals(1, response.size());
        assertEquals(DATABASE_2_ID, response.get(0).getId());
    }

}
