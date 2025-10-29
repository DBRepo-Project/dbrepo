package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseNotFoundException;
import at.ac.tuwien.ifs.dbrepo.repository.ContainerRepository;
import at.ac.tuwien.ifs.dbrepo.repository.DatabaseRepository;
import at.ac.tuwien.ifs.dbrepo.repository.LicenseRepository;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
public class DatabaseServicePersistenceTest extends BaseTest {

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @BeforeEach
    public void beforeEach() {
        /* metadata database */
        licenseRepository.save(LICENSE_1);
        containerRepository.saveAll(List.of(CONTAINER_1, CONTAINER_2, CONTAINER_3, CONTAINER_4));
        databaseRepository.saveAll(List.of(DATABASE_1, DATABASE_2, DATABASE_3, DATABASE_4));
    }

    @Test
    @Transactional(readOnly = true)
    public void findById_succeeds() throws DatabaseNotFoundException {

        /* test */
        final Database response = databaseService.findById(DATABASE_1_ID);
        assertEquals(DATABASE_1_ID, response.getId());
    }

    @Test
    @Transactional(readOnly = true)
    public void findAllPublicOrSchemaPublicByInternalName_privateEmpty_succeeds() {

        /* test */
        final List<Database> response = databaseService.findAllPublicOrSchemaPublicByInternalName(DATABASE_1_INTERNAL_NAME);
        assertEquals(0, response.size());
    }

}
