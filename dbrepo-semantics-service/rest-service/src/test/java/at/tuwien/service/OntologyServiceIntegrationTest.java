package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.entities.semantics.Ontology;
import at.tuwien.exception.OntologyNotFoundException;
import at.tuwien.exception.UserNotFoundException;
import at.tuwien.repository.mdb.OntologyRepository;
import at.tuwien.repository.mdb.RealmRepository;
import at.tuwien.repository.mdb.UserRepository;
import lombok.extern.log4j.Log4j2;
import org.apache.jena.sys.JenaSystem;
import org.junit.jupiter.api.BeforeAll;
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
public class OntologyServiceIntegrationTest extends BaseUnitTest {

    @Autowired
    private OntologyService ontologyService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RealmRepository realmRepository;

    @Autowired
    private OntologyRepository ontologyRepository;

    @BeforeAll
    public static void beforeAll() {
        JenaSystem.init();
    }

    @BeforeEach
    public void beforeEach() {
        realmRepository.save(REALM_DBREPO);
        userRepository.save(USER_1);
        ontologyRepository.save(ONTOLOGY_1);
        ontologyRepository.save(ONTOLOGY_2);
    }

    @Test
    public void findAll_succeeds() {

        /* test */
        final List<Ontology> response = ontologyService.findAll();
        assertEquals(2, response.size());
    }

    @Test
    public void find_succeeds() throws OntologyNotFoundException {

        /* test */
        final Ontology response = ontologyService.find(ONTOLOGY_1_ID);
        assertEquals(ONTOLOGY_1_ID, response.getId());
        assertEquals(ONTOLOGY_1_URI, response.getUri());
    }

    @Test
    public void find_fails() {

        /* test */
        assertThrows(OntologyNotFoundException.class, () -> {
            ontologyService.find(9999L);
        });
    }

    @Test
    public void create_succeeds() throws UserNotFoundException {

        /* test */
        final Ontology response = ontologyService.create(ONTOLOGY_3_CREATE_DTO, USER_1_PRINCIPAL);
        assertEquals(ONTOLOGY_3_ID, response.getId());
        assertEquals(ONTOLOGY_3_URI, response.getUri());
    }

    @Test
    public void create_notFound_fails() {

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            ontologyService.create(ONTOLOGY_3_CREATE_DTO, USER_3_PRINCIPAL);
        });
    }

    @Test
    public void delete_succeeds() throws OntologyNotFoundException {

        /* test */
        ontologyService.delete(ONTOLOGY_1_ID);
    }

    @Test
    public void delete_fails() {

        /* test */
        assertThrows(OntologyNotFoundException.class, () -> {
            ontologyService.delete(9999L);
        });
    }
}
