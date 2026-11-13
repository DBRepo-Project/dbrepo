package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.handlers.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TableRequestValidationTest {
    private final TableEndpoint endpoint = mock(TableEndpoint.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(endpoint)
            .setControllerAdvice(new ApiExceptionHandler()).build();
    private static final String PATH = "/api/v1/database/00000000-0000-0000-0000-000000000001/table";
    private static final String CONSTRAINTS = "{\"uniques\":[],\"checks\":[],\"primary_key\":[],\"foreign_keys\":[]}";
    private static final String COLUMN = "{\"name\":\"id\",\"type\":\"bigint\",\"null_allowed\":false}";

    private String body(String columns, String constraints) {
        return "{\"name\":\"measurements\",\"is_public\":false,\"is_schema_public\":false,\"columns\":"
                + columns + ",\"constraints\":" + constraints + "}";
    }

    @ParameterizedTest
    @CsvSource({"foreign_keys,foreignKeys", "primary_key,primaryKey", "checks,checks", "uniques,uniques"})
    void rejectsMissingAndNullConstraintLists(String jsonField, String javaField) throws Exception {
        for (String replacement : new String[]{"", "\"" + jsonField + "\":null,"}) {
            String constraints = CONSTRAINTS.replace("\"" + jsonField + "\":[]", "\"" + jsonField + "\":null");
            if (replacement.isEmpty()) constraints = constraints.replace("\"" + jsonField + "\":null", "").replace(",}", "}").replace("{,", "{").replace(",,", ",");
            mvc.perform(post(PATH).contentType("application/json").content(body("[" + COLUMN + "]", constraints)))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("code").value("error.form.malformed"))
                    .andExpect(jsonPath("message", containsString("constraints." + javaField)));
        }
        verifyNoInteractions(endpoint);
    }

    @Test
    void validatesNestedColumnsAndForeignKeysBeforeCallingTheEndpoint() throws Exception {
        mvc.perform(post(PATH).contentType("application/json").content(body("[{}]", CONSTRAINTS)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("message", containsString("columns[0].name")));
        mvc.perform(post(PATH).contentType("application/json").content(body("[" + COLUMN + "]",
                        CONSTRAINTS.replace("\"foreign_keys\":[]", "\"foreign_keys\":[{}]"))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("message", containsString("foreignKeys[0].referencedTable")));
        mvc.perform(post(PATH).contentType("application/json").content(body("[null]", CONSTRAINTS)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(endpoint);
    }

    @Test
    void acceptsCompleteConstraintsWithEmptyLists() throws Exception {
        mvc.perform(post(PATH).contentType("application/json").content(body("[" + COLUMN + "]", CONSTRAINTS)))
                .andExpect(status().is2xxSuccessful());
        verify(endpoint).create(any(), any(), any());
    }
}
