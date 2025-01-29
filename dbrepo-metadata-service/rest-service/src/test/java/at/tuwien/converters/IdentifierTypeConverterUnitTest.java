package at.tuwien.converters;

import at.tuwien.api.identifier.IdentifierTypeDto;
import at.tuwien.test.AbstractUnitTest;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@SpringBootTest
public class IdentifierTypeConverterUnitTest extends AbstractUnitTest {

    @Autowired
    private IdentifierTypeDtoConverter identifierTypeConverter;

    @BeforeEach
    public void beforeEach() {
        genesis();
    }

    @Test
    public void identifierTypeConverter_succeeds() {

        /* test */
        final IdentifierTypeDto response = identifierTypeConverter.convert(IdentifierTypeDto.DATABASE.getName());
        assertEquals(IdentifierTypeDto.DATABASE, response);
    }

    @Test
    public void identifierTypeConverter_fails() {

        /* test */
        assertThrows(IllegalArgumentException.class, () -> {
            identifierTypeConverter.convert("i_do_not_exist");
        });
    }
}
