package at.tuwien.converters;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.identifier.IdentifierTypeDto;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@SpringBootTest
@MockAmqp
@MockOpensearch
public class IdentifierTypeConverterUnitTest extends BaseUnitTest {

    @Autowired
    private IdentifierTypeConverter identifierTypeConverter;

    @Test
    public void convert_succeeds() {

        /* test */
        for (String name : List.of("DATABASE", "SUBSET", "VIEW")) {
            final IdentifierTypeDto response = identifierTypeConverter.convert(name);
            assertEquals(IdentifierTypeDto.valueOf(name), response);
        }
    }

    @Test
    public void convert_fails() {

        /* test */
        assertThrows(Exception.class, () -> {
            identifierTypeConverter.convert("idonotexist");
        });
    }

}
