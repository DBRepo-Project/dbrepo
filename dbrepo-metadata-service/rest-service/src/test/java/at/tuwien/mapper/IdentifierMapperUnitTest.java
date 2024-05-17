package at.tuwien.mapper;

import at.tuwien.api.identifier.IdentifierTypeDto;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.identifier.IdentifierType;
import at.tuwien.test.AbstractUnitTest;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Log4j2
@SpringBootTest
public class IdentifierMapperUnitTest extends AbstractUnitTest {

    @Autowired
    private IdentifierMapper identifierMapper;

    @Test
    public void identifierTypeDtoToIdentifierType_succeeds() {

        /* test */
        assertEquals(IdentifierType.VIEW, identifierMapper.identifierTypeDtoToIdentifierType(IdentifierTypeDto.VIEW));
        assertEquals(IdentifierType.TABLE, identifierMapper.identifierTypeDtoToIdentifierType(IdentifierTypeDto.TABLE));
        assertEquals(IdentifierType.SUBSET, identifierMapper.identifierTypeDtoToIdentifierType(IdentifierTypeDto.SUBSET));
        assertEquals(IdentifierType.DATABASE, identifierMapper.identifierTypeDtoToIdentifierType(IdentifierTypeDto.DATABASE));
    }

    @Test
    public void identifierCreateDtoToIdentifier_succeeds() {

        /* test */
        final Identifier response = identifierMapper.identifierCreateDtoToIdentifier(IDENTIFIER_1_CREATE_DTO);
        assertNull(response.getDatabase());
        assertNull(response.getViewId());
        assertNull(response.getQueryId());
        assertNull(response.getTableId());
        assertNull(response.getDoi());
        assertEquals(IDENTIFIER_1_TYPE, response.getType());
    }

    @Test
    public void identifierCreateDtoToIdentifier_withDoi_succeeds() {

        /* test */
        final Identifier response = identifierMapper.identifierCreateDtoToIdentifier(IDENTIFIER_1_CREATE_WITH_DOI_DTO);
        assertNull(response.getDatabase());
        assertNull(response.getViewId());
        assertNull(response.getQueryId());
        assertNull(response.getTableId());
        assertEquals(IDENTIFIER_1_DOI_NOT_NULL, response.getDoi());
        assertEquals(IDENTIFIER_1_TYPE, response.getType());
    }

    @Test
    public void identifierCreateDtoToIdentifier_subset_succeeds() {

        /* test */
        final Identifier response = identifierMapper.identifierCreateDtoToIdentifier(IDENTIFIER_2_CREATE_DTO);
        assertNull(response.getDatabase());
        assertNull(response.getViewId());
        assertNull(response.getTableId());
        assertEquals(IDENTIFIER_2_QUERY_ID, response.getQueryId());
        assertNull(response.getDoi());
        assertEquals(IDENTIFIER_2_TYPE, response.getType());
    }

    @Test
    public void identifierCreateDtoToIdentifier_view_succeeds() {

        /* test */
        final Identifier response = identifierMapper.identifierCreateDtoToIdentifier(IDENTIFIER_3_CREATE_DTO);
        assertNull(response.getDatabase());
        assertNull(response.getQueryId());
        assertNull(response.getTableId());
        assertEquals(IDENTIFIER_3_VIEW_ID, response.getViewId());
        assertNull(response.getDoi());
        assertEquals(IDENTIFIER_3_TYPE, response.getType());
    }

}
