package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.config.H2Utils;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.exception.IdentifierNotFoundException;
import at.tuwien.repository.jpa.*;
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
import static org.junit.jupiter.api.Assertions.assertThrows;

@Log4j2
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
public class IdentifierServiceIntegrationTest extends BaseUnitTest {

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RealmRepository realmRepository;

    @Autowired
    private IdentifierRepository identifierRepository;

    @Autowired
    private IdentifierService identifierService;

    @Autowired
    private H2Utils h2Utils;

    @BeforeEach
    public void beforeEach() {
        /* schema */
        h2Utils.runScript("schema.sql");
        /* metadata database */
        imageRepository.save(IMAGE_1_SIMPLE);
        realmRepository.save(REALM_DBREPO);
        userRepository.save(USER_1_SIMPLE);
        containerRepository.save(CONTAINER_1_SIMPLE);
        databaseRepository.save(DATABASE_1_SIMPLE);
        identifierRepository.save(IDENTIFIER_1);
    }

    @Test
    public void findAll_succeeds() {

        /* test */
        final List<Identifier> response = identifierService.findAll();
        assertEquals(1, response.size());
    }

    @Test
    public void find_succeeds() throws IdentifierNotFoundException {

        /* test */
        final Identifier response = identifierService.find(IDENTIFIER_1_ID);
        assertEquals(IDENTIFIER_1_ID, response.getId());
        assertEquals(IDENTIFIER_1_TITLE, response.getTitle());
    }

    @Test
    public void find_fails() {

        /* test */
        assertThrows(IdentifierNotFoundException.class, () -> {
            identifierService.find(IDENTIFIER_2_ID);
        });
    }

}
