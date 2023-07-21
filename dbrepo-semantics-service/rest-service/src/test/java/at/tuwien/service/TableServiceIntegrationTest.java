package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.semantics.EntityDto;
import at.tuwien.api.semantics.TableColumnEntityDto;
import at.tuwien.exception.QueryMalformedException;
import at.tuwien.exception.TableColumnNotFoundException;
import at.tuwien.exception.TableNotFoundException;
import at.tuwien.repository.mdb.OntologyRepository;
import at.tuwien.repository.mdb.TableColumnRepository;
import at.tuwien.repository.mdb.TableRepository;
import at.tuwien.repository.sdb.*;
import lombok.extern.log4j.Log4j2;
import org.apache.jena.sys.JenaSystem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class TableServiceIntegrationTest extends BaseUnitTest {

    @MockBean
    private UnitIdxRepository unitIdxRepository;

    @MockBean
    private ConceptIdxRepository conceptIdxRepository;

    @MockBean
    private TableRepository tableRepository;

    @MockBean
    private OntologyRepository ontologyRepository;

    @MockBean
    private TableColumnRepository tableColumnRepository;

    @Autowired
    private TableService tableService;

    @BeforeAll
    public static void beforeAll() {
        JenaSystem.init();
    }

    @Test
    public void suggestTableSemantics_success() throws TableNotFoundException, QueryMalformedException {

        /* mock */
        when(tableRepository.findByDatabaseIdAndId(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(Optional.of(TABLE_1));
        when(ontologyRepository.findAll())
                .thenReturn(List.of(ONTOLOGY_1, ONTOLOGY_2, ONTOLOGY_3, ONTOLOGY_4, ONTOLOGY_5));

        /* test */
        final List<EntityDto> response = tableService.suggestTableSemantics(DATABASE_1_ID, TABLE_1_ID);
        assertNotNull(response);
    }

    @Test
    public void suggestTableColumnSemantics_success() throws QueryMalformedException, TableColumnNotFoundException {

        /* mock */
        when(tableColumnRepository.findById(anyLong()))
                .thenReturn(Optional.of(TABLE_1_COLUMNS.get(0)));
        when(ontologyRepository.findAll())
                .thenReturn(List.of(ONTOLOGY_1, ONTOLOGY_2, ONTOLOGY_3, ONTOLOGY_4, ONTOLOGY_5));

        /* test */
        final List<TableColumnEntityDto> response = tableService.suggestTableColumnSemantics(DATABASE_1_ID, TABLE_1_ID, COLUMN_1_1_ID);
        assertNotNull(response);
    }
}
