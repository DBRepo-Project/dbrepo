package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.columns.TableColumnConcept;
import at.ac.tuwien.ifs.dbrepo.core.exception.ConceptNotFoundException;
import at.ac.tuwien.ifs.dbrepo.repository.ConceptRepository;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class ConceptServiceUnitTest extends BaseTest {

    @MockBean
    private ConceptRepository conceptRepository;

    @Autowired
    private ConceptService conceptService;

    @Test
    public void findAll_succeeds() {

        /* mock */
        when(conceptRepository.findAll())
                .thenReturn(List.of(CONCEPT_1));

        /* test */
        final List<TableColumnConcept> response = conceptService.findAll();
        assertEquals(1, response.size());
        assertTrue(response.stream().anyMatch(c -> c.getUri().equals(CONCEPT_1_URI)));
    }

    @Test
    public void find_succeeds() throws ConceptNotFoundException {

        /* mock */
        when(conceptRepository.findByUri(CONCEPT_1_URI))
                .thenReturn(Optional.of(CONCEPT_1));

        /* test */
        final TableColumnConcept response = conceptService.find(CONCEPT_1_URI);
        assertEquals(CONCEPT_1_URI, response.getUri());
        assertEquals(CONCEPT_1_NAME, response.getName());
        assertEquals(CONCEPT_1_DESCRIPTION, response.getDescription());
    }

    @Test
    public void findConcept_fails() {

        /* mock */
        when(conceptRepository.findByUri(anyString()))
                .thenReturn(Optional.empty());

        /* test */
        assertThrows(ConceptNotFoundException.class, () -> {
            conceptService.find("http://example.com/rdf");
        });
    }

}
