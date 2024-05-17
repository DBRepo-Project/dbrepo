package at.tuwien.mapper;

import at.tuwien.api.database.DatabaseDto;
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
public class DatabaseMapperUnitTest extends AbstractUnitTest {

    @Autowired
    private DatabaseMapper databaseMapper;

    @BeforeEach
    public void beforeEach() {
        genesis();
    }

    @Test
    public void databaseToDatabaseDto_succeeds() {

        /* test */
        final DatabaseDto response = databaseMapper.databaseToDatabaseDto(DATABASE_1);
        assertEquals(DATABASE_1_ID, response.getId());
        assertEquals(4, response.getIdentifiers().size());
        /* identifier 1 */
        final IdentifierDto identifier1 = response.getIdentifiers().get(0);
        assertEquals(DATABASE_1_ID, identifier1.getDatabaseId());
        assertNotNull(identifier1.getCreator());
        assertEquals(IDENTIFIER_1_CREATED_BY, identifier1.getCreator().getId());
        assertNotNull(identifier1.getCreated());
        assertNotNull(identifier1.getLastModified());
        /* identifier 2 */
        final IdentifierDto identifier2 = response.getIdentifiers().get(1);
        assertEquals(DATABASE_1_ID, identifier2.getDatabaseId());
        assertNotNull(identifier2.getCreator());
        assertEquals(IDENTIFIER_2_CREATED_BY, identifier2.getCreator().getId());
        assertNotNull(identifier2.getCreated());
        assertNotNull(identifier2.getLastModified());
        /* identifier 3 */
        final IdentifierDto identifier3 = response.getIdentifiers().get(2);
        assertEquals(DATABASE_1_ID, identifier3.getDatabaseId());
        assertNotNull(identifier3.getCreator());
        assertEquals(IDENTIFIER_3_CREATED_BY, identifier3.getCreator().getId());
        assertNotNull(identifier3.getCreated());
        assertNotNull(identifier3.getLastModified());
        /* identifier 4 */
        final IdentifierDto identifier4 = response.getIdentifiers().get(3);
        assertEquals(DATABASE_1_ID, identifier4.getDatabaseId());
        assertNotNull(identifier4.getCreator());
        assertEquals(IDENTIFIER_4_CREATED_BY, identifier4.getCreator().getId());
        assertNotNull(identifier4.getCreated());
        assertNotNull(identifier4.getLastModified());
    }

}
