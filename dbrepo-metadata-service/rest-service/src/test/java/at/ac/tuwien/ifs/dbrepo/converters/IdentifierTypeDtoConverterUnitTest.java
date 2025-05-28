package at.ac.tuwien.ifs.dbrepo.converters;

import at.ac.tuwien.ifs.dbrepo.core.api.identifier.IdentifierTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
@SpringBootTest
public class IdentifierTypeDtoConverterUnitTest extends BaseTest {

    @Autowired
    private IdentifierTypeDtoConverter identifierTypeDtoConverter;

    @Test
    public void IdentifierTypeDtoConverter_succeeds() {

        /* test */
        final IdentifierTypeDto response = identifierTypeDtoConverter.convert(IdentifierTypeDto.DATABASE.getName());
        assertEquals(IdentifierTypeDto.DATABASE, response);
    }

    @Test
    public void IdentifierTypeDtoConverter_fails() {

        /* test */
        assertThrows(IllegalArgumentException.class, () -> {
            identifierTypeDtoConverter.convert("i_do_not_exist");
        });
    }
}
