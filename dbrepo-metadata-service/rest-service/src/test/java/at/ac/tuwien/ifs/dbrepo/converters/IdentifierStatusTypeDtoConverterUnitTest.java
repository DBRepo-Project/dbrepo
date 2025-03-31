package at.ac.tuwien.ifs.dbrepo.converters;

import at.ac.tuwien.ifs.dbrepo.core.api.identifier.IdentifierStatusTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Log4j2
@SpringBootTest
public class IdentifierStatusTypeDtoConverterUnitTest extends BaseTest {

    @Autowired
    private IdentifierStatusTypeDtoConverter identifierStatusTypeDtoConverter;

    @Test
    public void identifierStatusTypeDtoConverter_succeeds() {

        /* test */
        final IdentifierStatusTypeDto response = identifierStatusTypeDtoConverter.convert(IdentifierStatusTypeDto.DRAFT.getName());
        assertEquals(IdentifierStatusTypeDto.DRAFT, response);
    }

    @Test
    public void identifierStatusTypeDtoConverter_fails() {

        /* test */
        assertThrows(IllegalArgumentException.class, () -> {
            identifierStatusTypeDtoConverter.convert("i_do_not_exist");
        });
    }
}
