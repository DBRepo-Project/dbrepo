package at.tuwien.validation;

import at.tuwien.annotations.MockAmqp;
import at.tuwien.exception.PaginationException;
import at.tuwien.exception.QueryNotSupportedException;
import at.tuwien.test.AbstractUnitTest;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Log4j2
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@SpringBootTest
@AutoConfigureObservability
@MockAmqp
public class EndpointValidatorUnitTest extends AbstractUnitTest {

    @Autowired
    private EndpointValidator endpointValidator;

    @Test
    public void validateDataParams_succeeds() throws Exception {

        /* test */
        endpointValidator.validateDataParams(null, null);
    }

    @Test
    public void validateDataParams_onlyPage_fails() {

        /* test */
        assertThrows(PaginationException.class, () -> {
            endpointValidator.validateDataParams(0L, null);
        });
    }

    @Test
    public void validateDataParams_negativePage_fails() {

        /* test */
        assertThrows(PaginationException.class, () -> {
            endpointValidator.validateDataParams(-1L, 10L);
        });
    }

    @Test
    public void validateDataParams_onlySize_fails() {

        /* test */
        assertThrows(PaginationException.class, () -> {
            endpointValidator.validateDataParams(null, 10L);
        });
    }

    @Test
    public void validateDataParams_zeroSize_fails() {

        /* test */
        assertThrows(PaginationException.class, () -> {
            endpointValidator.validateDataParams(0L, 0L);
        });
    }

    @Test
    public void validateForbiddenStatements_succeeds() throws QueryNotSupportedException {

        /* test */
        endpointValidator.validateForbiddenStatements("SELECT country FROM some_table");
    }

    @Test
    public void validateForbiddenStatements_fails() {

        /* test */
        assertThrows(QueryNotSupportedException.class, () -> {
            endpointValidator.validateForbiddenStatements("SELECT COUNT(id) FROM some_table");
        });
    }

    @Test
    public void validateForbiddenStatements_lowercase_fails() {

        /* test */
        assertThrows(QueryNotSupportedException.class, () -> {
            endpointValidator.validateForbiddenStatements("SELECT COUNT(id) FROM some_table");
        });
    }

}
