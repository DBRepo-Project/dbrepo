package at.ac.tuwien.ifs.dbrepo.mapper;

import at.ac.tuwien.ifs.dbrepo.core.api.database.query.*;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.CreateTableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.ColumnTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.CreateTableColumnDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.CreateTableConstraintsDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.foreign.CreateForeignKeyDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints.foreign.ReferenceTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.ColumnNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.ImageNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class MariaDbMapperUnitTest extends BaseTest {

    @Autowired
    private MariaDbMapper mariaDbMapper;

    @Autowired
    private DSLContext context;

    public static Stream<Arguments> nameToInternalName_parameters() {
        return Stream.of(
                Arguments.arguments("dash_minus", "OE/NO-027", "oe_no_027"),
                Arguments.arguments("percent", "OE%NO-027", "oe_no_027"),
                Arguments.arguments("umlaut", "OE/NÖ-027", "oe_no__027"),
                Arguments.arguments("dot", "OE.NO-027", "oe_no_027"),
                Arguments.arguments("double_dot", "OE:NO-027", "oe_no_027")
        );
    }

    @ParameterizedTest
    @MethodSource("nameToInternalName_parameters")
    public void nameToInternalName_succeeds(String name, String input, String expected) {

        /* test */
        assertEquals(expected, mariaDbMapper.nameToInternalName(input));
    }

    @Test
    public void tableCreateDtoToCreateTableRawQuery_descriptionWithForeignKey_succeeds() {
        final CreateTableDto request = CreateTableDto.builder()
                .name("Weather Measurement")
                .description("Measurements with station reference")
                .isPublic(true)
                .isSchemaPublic(true)
                .columns(List.of(CreateTableColumnDto.builder()
                                .name("measurement_id")
                                .type(ColumnTypeDto.BIGINT)
                                .nullAllowed(false)
                                .build(),
                        CreateTableColumnDto.builder()
                                .name("station_id")
                                .type(ColumnTypeDto.BIGINT)
                                .nullAllowed(false)
                                .build()))
                .constraints(CreateTableConstraintsDto.builder()
                        .primaryKey(Set.of("measurement_id"))
                        .foreignKeys(List.of(CreateForeignKeyDto.builder()
                                .columns(List.of("station_id"))
                                .referencedTable("Station")
                                .referencedColumns(List.of("station_id"))
                                .onDelete(ReferenceTypeDto.CASCADE)
                                .onUpdate(ReferenceTypeDto.CASCADE)
                                .build()))
                        .uniques(List.of())
                        .checks(Set.of())
                        .build())
                .build();

        /* test */
        assertEquals("CREATE TABLE `weather`.`weather_measurement` (`measurement_id` BIGINT(255) NOT NULL, `station_id` BIGINT(255) NOT NULL, PRIMARY KEY (`measurement_id`), FOREIGN KEY (`station_id`) REFERENCES `station` (`station_id`) ON DELETE CASCADE ON UPDATE CASCADE) WITH SYSTEM VERSIONING COMMENT \"Measurements with station reference\";",
                mariaDbMapper.tableCreateDtoToCreateTableRawQuery("weather", request));
    }

    @Test
    public void subsetDtoToNormalizedTimestampedQuery_succeeds() throws ColumnNotFoundException, ImageNotFoundException {
        final Instant timestamp = Instant.ofEpochSecond(1751363087);
        final SubsetDto request = SubsetDto.builder()
                .datasourceIds(new HashSet<>(Set.of(TABLE_1_ID)))
                .columns(new HashSet<>(Set.of(SubsetColumnDto.builder().id(COLUMN_1_1_ID).build(),
                        SubsetColumnDto.builder().id(COLUMN_1_2_ID).build())))
                .orders(new HashSet<>(Set.of(OrderDto.builder()
                        .columnId(COLUMN_1_1_ID)
                        .direction(OrderTypeDto.DESC)
                        .build())))
                .build();

        /* test */
        assertEquals("select `weather`.`weather_aus`.`date`, `weather`.`weather_aus`.`id` from `weather_aus` FOR SYSTEM_TIME AS OF TIMESTAMP '2025-07-01 09:44:47.000000' order by `weather`.`weather_aus`.`id` desc",
                mariaDbMapper.subsetDtoToNormalizedTimestampedQuery(context, DATABASE_1_CACHE, request, timestamp));
    }

    @Test
    public void subsetDtoToNormalizedTimestampedQuery_join_succeeds() throws ColumnNotFoundException, ImageNotFoundException {
        final Instant timestamp = Instant.ofEpochSecond(1751363087);
        final SubsetDto request = SubsetDto.builder()
                .datasourceIds(new HashSet<>(Set.of(TABLE_1_ID)))
                .columns(new HashSet<>(Set.of(SubsetColumnDto.builder().id(COLUMN_1_1_ID).build(),
                        SubsetColumnDto.builder().id(COLUMN_3_2_ID).build())))
                .joins(new HashSet<>(Set.of(JoinDto.builder()
                        .datasourceId(TABLE_3_ID)
                        .conditionals(new HashSet<>(Set.of(ConditionalDto.builder()
                                .columnId(COLUMN_1_1_ID)
                                .foreignColumnId(COLUMN_3_1_ID)
                                .build())))
                        .build())))
                .orders(new HashSet<>(Set.of(OrderDto.builder()
                        .columnId(COLUMN_1_1_ID)
                        .direction(OrderTypeDto.DESC)
                        .build())))
                .build();

        /* test */
        assertEquals("select `weather`.`sensor`.`linie`, `weather`.`weather_aus`.`id` from `weather_aus` FOR SYSTEM_TIME AS OF TIMESTAMP '2025-07-01 09:44:47.000000' join `sensor` on `weather`.`weather_aus`.`id` = `weather`.`sensor`.`id` order by `weather`.`weather_aus`.`id` desc",
                mariaDbMapper.subsetDtoToNormalizedTimestampedQuery(context, DATABASE_1_CACHE, request, timestamp));
    }

    @Test
    public void subsetDtoToNormalizedQuery_crossJoin_succeeds() throws ColumnNotFoundException, ImageNotFoundException {
        final SubsetDto request = SubsetDto.builder()
                .datasourceIds(new LinkedHashSet<>(Set.of(TABLE_1_ID, TABLE_2_ID)))
                .columns(new LinkedHashSet<>(Set.of(SubsetColumnDto.builder().id(COLUMN_1_1_ID).build(),
                        SubsetColumnDto.builder().id(COLUMN_2_1_ID).build())))
                .build();

        /* test */
        assertEquals("select `weather`.`weather_location`.`location`, `weather`.`weather_aus`.`id` from `weather_aus`, `weather_location`", mariaDbMapper.subsetDtoToNormalizedQuery(context, DATABASE_1_CACHE, request));
    }

    @Test
    public void subsetDtoToNormalizedQuery_duplicateColumnNames_succeeds() throws ColumnNotFoundException, ImageNotFoundException {
        final SubsetDto request = SubsetDto.builder()
                .datasourceIds(new LinkedHashSet<>(Set.of(TABLE_1_ID, TABLE_3_ID)))
                .columns(new LinkedHashSet<>(Set.of(SubsetColumnDto.builder().id(COLUMN_1_1_ID).build(),
                        SubsetColumnDto.builder().id(COLUMN_3_1_ID).alias("sensor_id").build())))
                .build();

        /* test */
        assertEquals("select `weather`.`weather_aus`.`id`, `weather`.`sensor`.`id` as `sensor_id` from `sensor`, `weather_aus`", mariaDbMapper.subsetDtoToNormalizedQuery(context, DATABASE_1_CACHE, request));
    }

    @Test
    public void subsetDtoToNormalizedQuery_multipleFilters_succeeds() throws ColumnNotFoundException, ImageNotFoundException {
        final SubsetDto request = SubsetDto.builder()
                .datasourceIds(new LinkedHashSet<>(Set.of(TABLE_1_ID)))
                .columns(new LinkedHashSet<>(Set.of(SubsetColumnDto.builder().id(COLUMN_1_1_ID).build())))
                .filters(List.of(FilterDto.builder()
                                .type(FilterTypeDto.WHERE)
                                .columnId(COLUMN_1_1_ID)
                                .operatorId(IMAGE_1_OPERATORS_2_ID)
                                .value("1")
                                .build(),
                        FilterDto.builder()
                                .type(FilterTypeDto.AND)
                                .build(),
                        FilterDto.builder()
                                .type(FilterTypeDto.WHERE)
                                .columnId(COLUMN_1_3_ID)
                                .operatorId(IMAGE_1_OPERATORS_2_ID)
                                .value("Vienna")
                                .build()))
                .build();

        /* test */
        assertEquals("select `weather`.`weather_aus`.`id` from `weather_aus` where (`weather`.`weather_aus`.`id` = '1' and `weather`.`weather_aus`.`location` = 'Vienna')", mariaDbMapper.subsetDtoToNormalizedQuery(context, DATABASE_1_CACHE, request));
    }

    @Test
    public void subsetDtoToNormalizedQuery_fiveFilters_succeeds() throws ColumnNotFoundException, ImageNotFoundException {
        final SubsetDto request = SubsetDto.builder()
                .datasourceIds(new LinkedHashSet<>(Set.of(TABLE_1_ID)))
                .columns(new LinkedHashSet<>(Set.of(SubsetColumnDto.builder().id(COLUMN_1_1_ID).build())))
                .filters(List.of(FilterDto.builder()
                                .type(FilterTypeDto.WHERE)
                                .columnId(COLUMN_1_1_ID)
                                .operatorId(IMAGE_1_OPERATORS_2_ID)
                                .value("1")
                                .build(),
                        FilterDto.builder()
                                .type(FilterTypeDto.AND)
                                .build(),
                        FilterDto.builder()
                                .type(FilterTypeDto.WHERE)
                                .columnId(COLUMN_1_2_ID)
                                .operatorId(IMAGE_1_OPERATORS_2_ID)
                                .value("2024-01-01")
                                .build(),
                        FilterDto.builder()
                                .type(FilterTypeDto.AND)
                                .build(),
                        FilterDto.builder()
                                .type(FilterTypeDto.WHERE)
                                .columnId(COLUMN_1_3_ID)
                                .operatorId(IMAGE_1_OPERATORS_2_ID)
                                .value("Vienna")
                                .build(),
                        FilterDto.builder()
                                .type(FilterTypeDto.AND)
                                .build(),
                        FilterDto.builder()
                                .type(FilterTypeDto.WHERE)
                                .columnId(COLUMN_1_4_ID)
                                .operatorId(IMAGE_1_OPERATORS_2_ID)
                                .value("10")
                                .build(),
                        FilterDto.builder()
                                .type(FilterTypeDto.AND)
                                .build(),
                        FilterDto.builder()
                                .type(FilterTypeDto.WHERE)
                                .columnId(COLUMN_1_5_ID)
                                .operatorId(IMAGE_1_OPERATORS_2_ID)
                                .value("0")
                                .build()))
                .build();

        /* test */
        assertEquals("select `weather`.`weather_aus`.`id` from `weather_aus` where (`weather`.`weather_aus`.`id` = '1' and `weather`.`weather_aus`.`date` = '2024-01-01' and `weather`.`weather_aus`.`location` = 'Vienna' and `weather`.`weather_aus`.`mintemp` = '10' and `weather`.`weather_aus`.`rainfall` = '0')", mariaDbMapper.subsetDtoToNormalizedQuery(context, DATABASE_1_CACHE, request));
    }

    @Test
    public void defaultRawSelectQuery_primaryKeyOrdering_succeeds() {
        final Instant timestamp = Instant.ofEpochSecond(1751363087);

        /* test */
        assertEquals("SELECT * FROM (SELECT * FROM `weather`.`weather_aus` FOR SYSTEM_TIME AS OF TIMESTAMP '2025-07-01 09:44:47.000000' as tbl) as tbl2 ORDER BY `tbl2`.`id` ASC, `tbl2`.`date` ASC LIMIT 10 OFFSET 0",
                mariaDbMapper.defaultRawSelectQuery("weather", "weather_aus", timestamp, 0L, 10L, List.of("id", "date"), "asc"));
    }

    @Test
    public void defaultRawSelectQuery_explicitOrdering_succeeds() {
        final Instant timestamp = Instant.ofEpochSecond(1751363087);

        /* test */
        assertEquals("SELECT * FROM (SELECT * FROM `weather`.`weather_aus` FOR SYSTEM_TIME AS OF TIMESTAMP '2025-07-01 09:44:47.000000' as tbl) as tbl2 ORDER BY `tbl2`.`date` DESC LIMIT 10 OFFSET 20",
                mariaDbMapper.defaultRawSelectQuery("weather", "weather_aus", timestamp, 2L, 10L, List.of("date"), "desc"));
    }

    @Test
    public void defaultRawSelectQuery_unpaginatedOrdering_succeeds() {
        final Instant timestamp = Instant.ofEpochSecond(1751363087);

        /* test */
        assertEquals("SELECT * FROM (SELECT * FROM `weather`.`weather_aus` FOR SYSTEM_TIME AS OF TIMESTAMP '2025-07-01 09:44:47.000000' as tbl) as tbl2 ORDER BY `tbl2`.`date` DESC",
                mariaDbMapper.defaultRawSelectQuery("weather", "weather_aus", timestamp, null, null, List.of("date"), "desc"));
    }

}
