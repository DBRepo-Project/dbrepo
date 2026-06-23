package at.ac.tuwien.ifs.dbrepo.validation;

import at.ac.tuwien.ifs.dbrepo.core.api.database.query.ConditionalDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.FilterDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.FilterTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.JoinDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.JoinTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.OrderDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.SubsetColumnDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.SubsetDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.PaginationException;
import at.ac.tuwien.ifs.dbrepo.core.exception.QueryMalformedException;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@SpringBootTest
@AutoConfigureObservability
public class EndpointValidatorUnitTest extends BaseTest {

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
    public void validateDataSortParams_nullNull_succeeds() throws Exception {

        /* test */
        assertDoesNotThrow(() -> endpointValidator.validateDataSortParams(null, null));
    }

    @Test
    public void validateDataSortParams_nameAsc_succeeds() throws Exception {

        /* test */
        assertDoesNotThrow(() -> endpointValidator.validateDataSortParams("name_en", "asc"));
    }

    @Test
    public void validateDataSortParams_nameDesc_succeeds() throws Exception {

        /* test */
        assertDoesNotThrow(() -> endpointValidator.validateDataSortParams("name_en", "desc"));
    }

    @Test
    public void validateDataSortParams_blankColumn_fails() {

        /* test */
        assertThrows(PaginationException.class, () -> endpointValidator.validateDataSortParams("", "asc"));
    }

    @Test
    public void validateDataSortParams_whitespaceColumn_fails() {

        /* test */
        assertThrows(PaginationException.class, () -> endpointValidator.validateDataSortParams("   ", null));
    }

    @Test
    public void validateDataSortParams_onlyColumn_fails() {

        /* test */
        assertThrows(PaginationException.class, () -> endpointValidator.validateDataSortParams("name_en", null));
    }

    @Test
    public void validateDataSortParams_onlyDirection_fails() {

        /* test */
        assertThrows(PaginationException.class, () -> endpointValidator.validateDataSortParams(null, "asc"));
    }

    @Test
    public void validateDataSortParams_invalidDirection_fails() {

        /* test */
        assertThrows(PaginationException.class, () -> endpointValidator.validateDataSortParams("name_en", "ascending"));
    }

    @Test
    public void validateSubsetParams_nullOrderColumn_fails() {

        /* mock */
        final SubsetDto request = validSubset()
                .orders(new LinkedHashSet<>(Set.of(OrderDto.builder()
                        .columnId(null)
                        .build())))
                .build();

        /* test */
        assertThrows(QueryMalformedException.class, () -> endpointValidator.validateSubsetParams(request));
    }

    @Test
    public void validateSubsetParams_nullFilterColumn_fails() {

        /* mock */
        final SubsetDto request = validSubset()
                .filters(List.of(FilterDto.builder()
                        .type(FilterTypeDto.WHERE)
                        .columnId(null)
                        .operatorId(UUID.randomUUID())
                        .value("1")
                        .build()))
                .build();

        /* test */
        assertThrows(QueryMalformedException.class, () -> endpointValidator.validateSubsetParams(request));
    }

    @Test
    public void validateSubsetParams_nullJoinColumn_fails() {

        /* mock */
        final SubsetDto request = validSubset()
                .joins(new LinkedHashSet<>(Set.of(JoinDto.builder()
                        .type(JoinTypeDto.INNER)
                        .datasourceId(UUID.randomUUID())
                        .conditionals(new LinkedHashSet<>(Set.of(ConditionalDto.builder()
                                .columnId(null)
                                .foreignColumnId(UUID.randomUUID())
                                .build())))
                        .build())))
                .build();

        /* test */
        assertThrows(QueryMalformedException.class, () -> endpointValidator.validateSubsetParams(request));
    }

    private SubsetDto.SubsetDtoBuilder validSubset() {
        return SubsetDto.builder()
                .datasourceIds(new LinkedHashSet<>(Set.of(UUID.randomUUID())))
                .columns(new LinkedHashSet<>(Set.of(SubsetColumnDto.builder()
                        .id(UUID.randomUUID())
                        .build())));
    }

}
