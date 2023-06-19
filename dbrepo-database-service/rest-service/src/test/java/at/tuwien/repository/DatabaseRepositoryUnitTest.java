package at.tuwien.repository;

import at.tuwien.BaseUnitTest;
import at.tuwien.config.IndexConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.container.Container;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.user.User;
import at.tuwien.repository.mdb.*;
import at.tuwien.repository.sdb.DatabaseIdxRepository;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@Testcontainers
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
public class DatabaseRepositoryUnitTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private IndexConfig indexConfig;

    @MockBean
    private DatabaseIdxRepository databaseIdxRepository;

    @MockBean
    private Channel channel;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RealmRepository realmRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private DatabaseAccessRepository databaseAccessRepository;

    @Test
    public void findConfigureAccess_noAccess_succeeds() {

        /* test */
        final List<Database> response = findConfigureAccess_generic(USER_1_ID, USER_1, CONTAINER_1, DATABASE_1, null);
        assertEquals(1, response.size());
        assertEquals(DATABASE_1_ID, response.get(0).getId());
    }

    @Test
    public void findConfigureAccess_hasReadAccess_succeeds() {

        /* test */
        final List<Database> response = findConfigureAccess_generic(USER_1_ID, USER_1, CONTAINER_1, DATABASE_1, DATABASE_1_USER_1_READ_ACCESS);
        assertEquals(1, response.size());
        assertEquals(DATABASE_1_ID, response.get(0).getId());
    }

    @Test
    public void findConfigureAccess_hasWriteOwnAccess_succeeds() {

        /* test */
        final List<Database> response = findConfigureAccess_generic(USER_1_ID, USER_1, CONTAINER_1, DATABASE_1, DATABASE_1_USER_1_WRITE_OWN_ACCESS);
        assertEquals(1, response.size());
        assertEquals(DATABASE_1_ID, response.get(0).getId());
    }

    @Test
    public void findConfigureAccess_hasWriteAllAccess_succeeds() {

        /* test */
        final List<Database> response = findConfigureAccess_generic(USER_1_ID, USER_1, CONTAINER_1, DATABASE_1, DATABASE_1_USER_1_WRITE_ALL_ACCESS);
        assertEquals(1, response.size());
        assertEquals(DATABASE_1_ID, response.get(0).getId());
    }

    @Test
    public void findWriteAccess_noAccess_fails() {

        /* mock */
        userRepository.save(USER_2);

        /* test */
        final List<Database> response = findWriteAccess_generic(USER_1_ID, USER_1, CONTAINER_2, DATABASE_2, null);
        assertEquals(0, response.size());
    }

    @Test
    public void findWriteAccess_hasReadAccess_succeeds() {

        /* mock */
        userRepository.save(USER_2);

        /* test */
        final List<Database> response = findWriteAccess_generic(USER_1_ID, USER_1, CONTAINER_2, DATABASE_2, DATABASE_2_USER_1_READ_ACCESS);
        assertEquals(0, response.size());
    }

    @Test
    public void findWriteAccess_hasWriteOwnAccess_succeeds() {

        /* mock */
        userRepository.save(USER_2);

        /* test */
        final List<Database> response = findWriteAccess_generic(USER_1_ID, USER_1, CONTAINER_2, DATABASE_2, DATABASE_2_USER_1_WRITE_OWN_ACCESS);
        assertEquals(1, response.size());
        assertEquals(DATABASE_2_ID, response.get(0).getId());
    }

    @Test
    public void findWriteAccess_hasWriteAllAccess_succeeds() {

        /* mock */
        userRepository.save(USER_2);

        /* test */
        final List<Database> response = findWriteAccess_generic(USER_1_ID, USER_1, CONTAINER_2, DATABASE_2, DATABASE_2_USER_1_WRITE_ALL_ACCESS);
        assertEquals(1, response.size());
        assertEquals(DATABASE_2_ID, response.get(0).getId());
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    public List<Database> findConfigureAccess_generic(UUID id, User user, Container container, Database database,
                                                      DatabaseAccess access) {

        /* mock */
        realmRepository.save(REALM_DBREPO);
        userRepository.save(user);
        imageRepository.save(IMAGE_1);
        containerRepository.save(container);
        databaseRepository.save(database);
        if (access != null) {
            databaseAccessRepository.save(access);
        }

        /* test */
        return databaseRepository.findConfigureAccess(id);
    }

    public List<Database> findWriteAccess_generic(UUID id, User user, Container container, Database database,
                                                  DatabaseAccess access) {

        /* mock */
        realmRepository.save(REALM_DBREPO);
        userRepository.save(user);
        imageRepository.save(IMAGE_1);
        containerRepository.save(container);
        databaseRepository.save(database);
        if (access != null) {
            log.trace("insert access: database={}, type={}", access.getHdbid(), access.getType());
            databaseAccessRepository.save(access);
        }

        /* test */
        return databaseRepository.findWriteAccess(id);
    }

    public List<Database> findReadAccess_generic(UUID id, User user, Container container, Database database,
                                                 DatabaseAccess access) {

        /* mock */
        realmRepository.save(REALM_DBREPO);
        userRepository.save(user);
        imageRepository.save(IMAGE_1);
        containerRepository.save(container);
        databaseRepository.save(database);
        if (access != null) {
            databaseAccessRepository.save(access);
        }

        /* test */
        return databaseRepository.findReadAccess(id);
    }

}
