package at.tuwien.mapper;

import at.tuwien.api.database.ViewDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.test.AbstractUnitTest;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Log4j2
@SpringBootTest
public class ViewMapperUnitTest extends AbstractUnitTest {

    @Autowired
    private ViewMapper viewMapper;

    @BeforeEach
    public void beforeEach() {
        genesis();
    }

    @Test
    public void viewToViewDto_succeeds() {

        /* test */
        final ViewDto response = viewMapper.viewToViewDto(VIEW_1);
        assertEquals(VIEW_1_ID, response.getId());
        assertEquals(VIEW_1_DATABASE_ID, response.getVdbid());
        assertEquals(VIEW_1_NAME, response.getName());
        assertEquals(VIEW_1_INTERNAL_NAME, response.getInternalName());
        assertNotNull(response.getDatabase());
        assertEquals(VIEW_1_DATABASE_ID, response.getDatabase().getId());
        assertEquals(VIEW_1_QUERY, response.getQuery());
        assertEquals(VIEW_1_QUERY_HASH, response.getQueryHash());
        assertNotNull(response.getIdentifiers());
        assertEquals(1, response.getIdentifiers().size());
        final IdentifierDto identifier0 = response.getIdentifiers().get(0);
        assertEquals(IDENTIFIER_3_ID, identifier0.getId());
        assertEquals(VIEW_1_DATABASE_ID, identifier0.getDatabaseId());
        assertEquals(VIEW_1_ID, identifier0.getViewId());
        assertEquals(VIEW_1_QUERY, identifier0.getQuery());
        assertEquals(VIEW_1_QUERY_HASH, identifier0.getQueryHash());
    }

}
