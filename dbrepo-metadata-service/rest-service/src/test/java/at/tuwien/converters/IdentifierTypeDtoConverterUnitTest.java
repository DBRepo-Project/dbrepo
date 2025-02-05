package at.tuwien.converters;

import at.tuwien.api.identifier.IdentifierTypeDto;
import at.tuwien.test.AbstractUnitTest;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Log4j2
@SpringBootTest
public class IdentifierTypeDtoConverterUnitTest extends AbstractUnitTest {

    @Autowired
    private IdentifierTypeDtoConverter identifierTypeDtoConverter;

    @BeforeEach
    public void beforeEach() {
        genesis();
    }

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
