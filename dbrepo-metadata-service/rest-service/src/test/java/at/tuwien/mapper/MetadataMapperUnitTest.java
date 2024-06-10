package at.tuwien.mapper;

import at.tuwien.api.database.DatabaseDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.database.table.columns.ColumnTypeDto;
import at.tuwien.api.database.table.constraints.foreign.ForeignKeyDto;
import at.tuwien.api.database.table.constraints.foreign.ForeignKeyReferenceDto;
import at.tuwien.api.database.table.constraints.foreign.ReferenceTypeDto;
import at.tuwien.api.database.table.constraints.primary.PrimaryKeyDto;
import at.tuwien.api.database.table.constraints.unique.UniqueDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.api.identifier.IdentifierTypeDto;
import at.tuwien.api.user.UserBriefDto;
import at.tuwien.api.user.UserDto;
import at.tuwien.entities.container.Container;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.identifier.IdentifierType;
import at.tuwien.test.AbstractUnitTest;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@SpringBootTest
public class MetadataMapperUnitTest extends AbstractUnitTest {

    private final DateTimeFormatter mariaDbFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSS]")
            .withZone(ZoneId.of("UTC"));

    @Autowired
    private MetadataMapper metadataMapper;

    @BeforeEach
    public void beforeEach() {
        genesis();
    }

    @Test
    public void mapMariaDbInstant_succeeds() {
        final String timestamp = "2023-01-08 08:49:29";
        final Instant compare = Instant.ofEpochSecond(1673167769);

        /* test */
        final Instant response = LocalDateTime.parse(timestamp, mariaDbFormatter)
                .atZone(ZoneId.of("UTC"))
                .toInstant();
        assertEquals(compare, response);
    }

    @Test
    public void containerEquals_fails() {

        /* test */
        assertNotEquals(CONTAINER_1, CONTAINER_2);
    }

    @Test
    public void containerEquals_identity_succeeds() {

        /* test */
        assertEquals(CONTAINER_1, CONTAINER_1);
    }

    @Test
    public void containerEquals_similar_succeeds() {
        final Container tmp = Container.builder()
                .id(CONTAINER_1_ID)
                .build();

        /* test */
        assertEquals(CONTAINER_1, tmp);
    }

    @Test
    public void identifierTypeDtoToIdentifierType_succeeds() {

        /* test */
        assertEquals(IdentifierType.VIEW, metadataMapper.identifierTypeDtoToIdentifierType(IdentifierTypeDto.VIEW));
        assertEquals(IdentifierType.TABLE, metadataMapper.identifierTypeDtoToIdentifierType(IdentifierTypeDto.TABLE));
        assertEquals(IdentifierType.SUBSET, metadataMapper.identifierTypeDtoToIdentifierType(IdentifierTypeDto.SUBSET));
        assertEquals(IdentifierType.DATABASE, metadataMapper.identifierTypeDtoToIdentifierType(IdentifierTypeDto.DATABASE));
    }

    @Test
    public void identifierCreateDtoToIdentifier_succeeds() {

        /* test */
        final Identifier response = metadataMapper.identifierCreateDtoToIdentifier(IDENTIFIER_1_CREATE_DTO);
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
        final Identifier response = metadataMapper.identifierCreateDtoToIdentifier(IDENTIFIER_1_CREATE_WITH_DOI_DTO);
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
        final Identifier response = metadataMapper.identifierCreateDtoToIdentifier(IDENTIFIER_2_CREATE_DTO);
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
        final Identifier response = metadataMapper.identifierCreateDtoToIdentifier(IDENTIFIER_3_CREATE_DTO);
        assertNull(response.getDatabase());
        assertNull(response.getQueryId());
        assertNull(response.getTableId());
        assertEquals(IDENTIFIER_3_VIEW_ID, response.getViewId());
        assertNull(response.getDoi());
        assertEquals(IDENTIFIER_3_TYPE, response.getType());
    }

    @Test
    public void customDatabaseToDatabaseDto_succeeds() {

        /* test */
        final DatabaseDto response = metadataMapper.customDatabaseToDatabaseDto(DATABASE_1);
        assertEquals(DATABASE_1_ID, response.getId());
        assertNotNull(response.getContact());
        assertEquals(USER_1_ID, response.getContact().getId());
        /* identifiers formatted */
        assertEquals(4, response.getIdentifiers().size());
        final IdentifierDto identifier1 = response.getIdentifiers().get(0);
        assertEquals(DATABASE_1_ID, identifier1.getDatabaseId());
        assertNotNull(identifier1.getCreator());
        assertEquals(IDENTIFIER_1_CREATED_BY, identifier1.getCreator().getId());
        assertNotNull(identifier1.getCreated());
        assertNotNull(identifier1.getLastModified());
        final IdentifierDto identifier2 = response.getIdentifiers().get(1);
        assertEquals(DATABASE_1_ID, identifier2.getDatabaseId());
        assertNotNull(identifier2.getCreator());
        assertEquals(IDENTIFIER_2_CREATED_BY, identifier2.getCreator().getId());
        assertNotNull(identifier2.getCreated());
        assertNotNull(identifier2.getLastModified());
        final IdentifierDto identifier3 = response.getIdentifiers().get(2);
        assertEquals(DATABASE_1_ID, identifier3.getDatabaseId());
        assertNotNull(identifier3.getCreator());
        assertEquals(IDENTIFIER_3_CREATED_BY, identifier3.getCreator().getId());
        assertNotNull(identifier3.getCreated());
        assertNotNull(identifier3.getLastModified());
        final IdentifierDto identifier4 = response.getIdentifiers().get(3);
        assertEquals(DATABASE_1_ID, identifier4.getDatabaseId());
        assertNotNull(identifier4.getCreator());
        assertEquals(IDENTIFIER_4_CREATED_BY, identifier4.getCreator().getId());
        assertNotNull(identifier4.getCreated());
        assertNotNull(identifier4.getLastModified());
        /* Table 1 formatted */
        final TableDto table0 = response.getTables().get(0);
        assertEquals(TABLE_1_ID, table0.getId());
        assertEquals(TABLE_1_NAME, table0.getName());
        assertEquals(TABLE_1_INTERNALNAME, table0.getInternalName());
        assertEquals(TABLE_1_DESCRIPTION, table0.getDescription());
        assertEquals(DATABASE_1_ID, table0.getTdbid());
        assertEquals(USER_1_ID, table0.getCreatedBy());
        assertEquals(USER_1_ID, table0.getOwner().getId());
        assertEquals(USER_1_ID, table0.getCreator().getId());
        assertEquals(TABLE_1_AVG_ROW_LENGTH, table0.getAvgRowLength());
        assertEquals(TABLE_1_NUM_ROWS, table0.getNumRows());
        assertEquals(TABLE_1_DATA_LENGTH, table0.getDataLength());
        assertEquals(TABLE_1_MAX_DATA_LENGTH, table0.getMaxDataLength());
        assertNotNull(table0.getCreated());
        /* columns formatted */
        assertEquals(TABLE_1_COLUMNS.size(), table0.getColumns().size());
        for (int i = 0; i < TABLE_1_COLUMNS.size(); i++) {
            assertEquals(TABLE_1_COLUMNS.get(i).getId(), table0.getColumns().get(i).getId());
            assertEquals(TABLE_1_COLUMNS.get(i).getOrdinalPosition(), table0.getColumns().get(i).getOrdinalPosition());
            assertNotNull(table0.getColumns().get(i).getOrdinalPosition());
            assertEquals(TABLE_1_COLUMNS.get(i).getTable().getId(), table0.getColumns().get(i).getTableId());
            assertEquals(TABLE_1_COLUMNS.get(i).getName(), table0.getColumns().get(i).getName());
            assertEquals(TABLE_1_COLUMNS.get(i).getInternalName(), table0.getColumns().get(i).getInternalName());
            assertEquals(List.of(ColumnTypeDto.BIGINT, ColumnTypeDto.DATE, ColumnTypeDto.VARCHAR, ColumnTypeDto.DECIMAL, ColumnTypeDto.DECIMAL).get(i), table0.getColumns().get(i).getColumnType());
            assertEquals(TABLE_1_COLUMNS.get(i).getSize(), table0.getColumns().get(i).getSize());
            assertEquals(TABLE_1_COLUMNS.get(i).getD(), table0.getColumns().get(i).getD());
            assertEquals(TABLE_1_COLUMNS.get(i).getIsNullAllowed(), table0.getColumns().get(i).getIsNullAllowed());
            assertEquals(TABLE_1_COLUMNS.get(i).getAutoGenerated(), table0.getColumns().get(i).getAutoGenerated());
            assertEquals(TABLE_1_COLUMNS.get(i).getEnums(), table0.getColumns().get(i).getEnums());
            assertEquals(TABLE_1_COLUMNS.get(i).getSets(), table0.getColumns().get(i).getSets());
        }
        /* constraints formatted */
        assertNotNull(table0.getConstraints());
        assertEquals(0, table0.getConstraints().getUniques().size());
        assertEquals(0, table0.getConstraints().getChecks().size());
        assertEquals(0, table0.getConstraints().getForeignKeys().size());
        assertEquals(1, table0.getConstraints().getPrimaryKey().size());
        final PrimaryKeyDto table0pk = new ArrayList<>(table0.getConstraints().getPrimaryKey()).get(0);
        assertEquals(1L, table0pk.getId());
        assertEquals(TABLE_1_COLUMNS_BRIEF_0_DTO.getId(), table0pk.getColumn().getId());
        assertEquals(TABLE_1_COLUMNS_BRIEF_0_DTO.getName(), table0pk.getColumn().getName());
        assertEquals(TABLE_1_COLUMNS_BRIEF_0_DTO.getId(), table0pk.getColumn().getId());
        assertEquals(TABLE_1_COLUMNS_BRIEF_0_DTO.getName(), table0pk.getColumn().getName());
        assertEquals(TABLE_1_COLUMNS_BRIEF_0_DTO.getInternalName(), table0pk.getColumn().getInternalName());
        assertEquals(TABLE_1_ID, table0pk.getTable().getId());
        assertEquals(DATABASE_1_ID, table0pk.getTable().getDatabaseId());
        assertEquals(ColumnTypeDto.BIGINT, table0pk.getColumn().getColumnType());
        assertNull(table0pk.getColumn().getAlias());
        assertEquals(TABLE_1_ID, table0pk.getColumn().getTableId());
        assertEquals(DATABASE_1_ID, table0pk.getColumn().getDatabaseId());
        /* Table 2 formatted */
        final TableDto table1 = response.getTables().get(1);
        assertEquals(TABLE_2_ID, table1.getId());
        assertEquals(TABLE_2_NAME, table1.getName());
        assertEquals(TABLE_2_INTERNALNAME, table1.getInternalName());
        assertEquals(TABLE_2_DESCRIPTION, table1.getDescription());
        assertEquals(DATABASE_1_ID, table1.getTdbid());
        assertEquals(USER_2_ID, table1.getCreatedBy());
        assertEquals(USER_2_ID, table1.getOwner().getId());
        assertEquals(USER_2_ID, table1.getCreator().getId());
        assertEquals(TABLE_2_AVG_ROW_LENGTH, table1.getAvgRowLength());
        assertEquals(TABLE_2_NUM_ROWS, table1.getNumRows());
        assertEquals(TABLE_2_DATA_LENGTH, table1.getDataLength());
        assertEquals(TABLE_2_MAX_DATA_LENGTH, table1.getMaxDataLength());
        assertNotNull(table1.getCreated());
        /* columns formatted */
        assertEquals(TABLE_2_COLUMNS.size(), table1.getColumns().size());
        for (int i = 0; i < TABLE_2_COLUMNS.size(); i++) {
            assertEquals(TABLE_2_COLUMNS.get(i).getId(), table1.getColumns().get(i).getId());
            assertEquals(TABLE_2_COLUMNS.get(i).getOrdinalPosition(), table1.getColumns().get(i).getOrdinalPosition());
            assertNotNull(table1.getColumns().get(i).getOrdinalPosition());
            assertEquals(TABLE_2_COLUMNS.get(i).getTable().getId(), table1.getColumns().get(i).getTableId());
            assertEquals(TABLE_2_COLUMNS.get(i).getName(), table1.getColumns().get(i).getName());
            assertEquals(TABLE_2_COLUMNS.get(i).getInternalName(), table1.getColumns().get(i).getInternalName());
            assertEquals(List.of(ColumnTypeDto.VARCHAR, ColumnTypeDto.DECIMAL, ColumnTypeDto.DECIMAL).get(i), table1.getColumns().get(i).getColumnType());
            assertEquals(TABLE_2_COLUMNS.get(i).getSize(), table1.getColumns().get(i).getSize());
            assertEquals(TABLE_2_COLUMNS.get(i).getD(), table1.getColumns().get(i).getD());
            assertEquals(TABLE_2_COLUMNS.get(i).getIsNullAllowed(), table1.getColumns().get(i).getIsNullAllowed());
            assertEquals(TABLE_2_COLUMNS.get(i).getAutoGenerated(), table1.getColumns().get(i).getAutoGenerated());
            assertEquals(TABLE_2_COLUMNS.get(i).getEnums(), table1.getColumns().get(i).getEnums());
            assertEquals(TABLE_2_COLUMNS.get(i).getSets(), table1.getColumns().get(i).getSets());
        }
        /* constraints formatted */
        assertNotNull(table1.getConstraints());
        assertEquals(1, table1.getConstraints().getUniques().size());
        final UniqueDto table1uk = table1.getConstraints().getUniques().get(0);
        assertEquals(1L, table1uk.getId());
        assertEquals(TABLE_2_ID, table1uk.getTable().getId());
        assertEquals(DATABASE_1_ID, table1uk.getTable().getDatabaseId());
        assertEquals("uk_1", table1uk.getName());
        assertEquals(TABLE_2_COLUMNS_DTO.get(1).getId(), table1uk.getColumns().get(0).getId());
        assertEquals(1, table1.getConstraints().getChecks().size());
        assertEquals("`mintemp` > 0", new ArrayList<>(table1.getConstraints().getChecks()).get(0));
        assertEquals(1, table1.getConstraints().getForeignKeys().size());
        final ForeignKeyDto table1fk = new ArrayList<>(table1.getConstraints().getForeignKeys()).get(0);
        assertEquals("fk_location", table1fk.getName());
        assertEquals(ReferenceTypeDto.NO_ACTION, table1fk.getOnDelete());
        assertEquals(ReferenceTypeDto.NO_ACTION, table1fk.getOnUpdate());
        assertEquals(TABLE_1_ID, table1fk.getTable().getId());
        assertEquals(TABLE_2_ID, table1fk.getReferencedTable().getId());
        final ForeignKeyReferenceDto table1fkr = table1fk.getReferences().get(0);
        assertEquals(1L, table1fkr.getId());
        assertEquals(TABLE_2_COLUMNS_DTO.get(2).getId(), table1fkr.getColumn().getId());
        assertEquals(TABLE_2_COLUMNS_DTO.get(2).getTable().getId(), table1fkr.getColumn().getTableId());
        assertEquals(TABLE_2_COLUMNS_DTO.get(2).getDatabaseId(), table1fkr.getColumn().getDatabaseId());
        assertEquals(TABLE_1_COLUMNS_DTO.get(0).getDatabaseId(), table1fkr.getReferencedColumn().getId());
        assertEquals(TABLE_1_COLUMNS_DTO.get(0).getDatabaseId(), table1fkr.getReferencedColumn().getTableId());
        assertEquals(TABLE_1_COLUMNS_DTO.get(0).getDatabaseId(), table1fkr.getReferencedColumn().getDatabaseId());
        assertEquals(1, table1.getConstraints().getPrimaryKey().size());
        final PrimaryKeyDto table1pk = new ArrayList<>(table1.getConstraints().getPrimaryKey()).get(0);
        assertEquals(2L, table1pk.getId());
        assertEquals(TABLE_2_COLUMNS_BRIEF_0_DTO.getId(), table1pk.getColumn().getId());
        assertEquals(TABLE_2_COLUMNS_BRIEF_0_DTO.getName(), table1pk.getColumn().getName());
        assertEquals(TABLE_2_COLUMNS_BRIEF_0_DTO.getId(), table1pk.getColumn().getId());
        assertEquals(TABLE_2_COLUMNS_BRIEF_0_DTO.getName(), table1pk.getColumn().getName());
        assertEquals(TABLE_2_COLUMNS_BRIEF_0_DTO.getInternalName(), table1pk.getColumn().getInternalName());
        assertEquals(ColumnTypeDto.VARCHAR, table1pk.getColumn().getColumnType());
        assertNull(table1pk.getColumn().getAlias());
        assertEquals(TABLE_2_ID, table1pk.getColumn().getTableId());
        assertEquals(DATABASE_1_ID, table1pk.getColumn().getDatabaseId());
        /* Table 3 formatted */
        final TableDto table2 = response.getTables().get(2);
        assertEquals(TABLE_3_ID, table2.getId());
        assertEquals(TABLE_3_NAME, table2.getName());
        assertEquals(TABLE_3_INTERNALNAME, table2.getInternalName());
        assertEquals(TABLE_3_DESCRIPTION, table2.getDescription());
        assertEquals(DATABASE_1_ID, table2.getTdbid());
        assertEquals(USER_3_ID, table2.getCreatedBy());
        assertEquals(USER_3_ID, table2.getOwner().getId());
        assertEquals(USER_3_ID, table2.getCreator().getId());
        assertEquals(TABLE_3_AVG_ROW_LENGTH, table2.getAvgRowLength());
        assertEquals(TABLE_3_NUM_ROWS, table2.getNumRows());
        assertEquals(TABLE_3_DATA_LENGTH, table2.getDataLength());
        assertEquals(TABLE_3_MAX_DATA_LENGTH, table2.getMaxDataLength());
        assertNotNull(table2.getCreated());
        /* columns formatted */
        assertEquals(TABLE_3_COLUMNS.size(), table2.getColumns().size());
        for (int i = 0; i < TABLE_3_COLUMNS.size(); i++) {
            assertEquals(TABLE_3_COLUMNS.get(i).getId(), table2.getColumns().get(i).getId());
            assertEquals(TABLE_3_COLUMNS.get(i).getOrdinalPosition(), table2.getColumns().get(i).getOrdinalPosition());
            assertNotNull(table2.getColumns().get(i).getOrdinalPosition());
            assertEquals(TABLE_3_COLUMNS.get(i).getTable().getId(), table2.getColumns().get(i).getTableId());
            assertEquals(TABLE_3_COLUMNS.get(i).getName(), table2.getColumns().get(i).getName());
            assertEquals(TABLE_3_COLUMNS.get(i).getInternalName(), table2.getColumns().get(i).getInternalName());
            assertEquals(List.of(ColumnTypeDto.BIGINT, ColumnTypeDto.INT, ColumnTypeDto.INT, ColumnTypeDto.DATE, ColumnTypeDto.INT, ColumnTypeDto.INT, ColumnTypeDto.INT, ColumnTypeDto.INT, ColumnTypeDto.INT, ColumnTypeDto.INT, ColumnTypeDto.DATE, ColumnTypeDto.INT, ColumnTypeDto.INT, ColumnTypeDto.INT, ColumnTypeDto.INT, ColumnTypeDto.INT, ColumnTypeDto.INT, ColumnTypeDto.INT, ColumnTypeDto.INT, ColumnTypeDto.DATE, ColumnTypeDto.INT, ColumnTypeDto.INT, ColumnTypeDto.INT, ColumnTypeDto.INT, ColumnTypeDto.INT, ColumnTypeDto.INT, ColumnTypeDto.INT, ColumnTypeDto.INT, ColumnTypeDto.INT, ColumnTypeDto.INT, ColumnTypeDto.INT, ColumnTypeDto.INT, ColumnTypeDto.INT, ColumnTypeDto.INT, ColumnTypeDto.INT).get(i), table2.getColumns().get(i).getColumnType());
            assertEquals(TABLE_3_COLUMNS.get(i).getSize(), table2.getColumns().get(i).getSize());
            assertEquals(TABLE_3_COLUMNS.get(i).getD(), table2.getColumns().get(i).getD());
            assertEquals(TABLE_3_COLUMNS.get(i).getIsNullAllowed(), table2.getColumns().get(i).getIsNullAllowed());
            assertEquals(TABLE_3_COLUMNS.get(i).getAutoGenerated(), table2.getColumns().get(i).getAutoGenerated());
            assertEquals(TABLE_3_COLUMNS.get(i).getEnums(), table2.getColumns().get(i).getEnums());
            assertEquals(TABLE_3_COLUMNS.get(i).getSets(), table2.getColumns().get(i).getSets());
        }
        /* constraints formatted */
        final PrimaryKeyDto table2pk = new ArrayList<>(table2.getConstraints().getPrimaryKey()).get(0);
        assertEquals(TABLE_3_COLUMNS_BRIEF_0_DTO.getId(), table2pk.getColumn().getId());
        assertEquals(TABLE_3_COLUMNS_BRIEF_0_DTO.getName(), table2pk.getColumn().getName());
        assertEquals(TABLE_3_COLUMNS_BRIEF_0_DTO.getId(), table2pk.getColumn().getId());
        assertEquals(TABLE_3_COLUMNS_BRIEF_0_DTO.getName(), table2pk.getColumn().getName());
        assertEquals(TABLE_3_COLUMNS_BRIEF_0_DTO.getInternalName(), table2pk.getColumn().getInternalName());
        assertEquals(ColumnTypeDto.BIGINT, table2pk.getColumn().getColumnType());
        assertNull(table2pk.getColumn().getAlias());
        assertEquals(TABLE_3_ID, table2pk.getColumn().getTableId());
        assertEquals(DATABASE_1_ID, table2pk.getColumn().getDatabaseId());
        /* Table 4 formatted */
        final TableDto table3 = response.getTables().get(3);
        assertEquals(TABLE_4_ID, table3.getId());
        assertEquals(TABLE_4_NAME, table3.getName());
        assertEquals(TABLE_4_INTERNALNAME, table3.getInternalName());
        assertEquals(TABLE_4_DESCRIPTION, table3.getDescription());
        assertEquals(DATABASE_1_ID, table3.getTdbid());
        assertEquals(USER_1_ID, table3.getCreatedBy());
        assertEquals(USER_1_ID, table3.getOwner().getId());
        assertEquals(USER_1_ID, table3.getCreator().getId());
        assertEquals(TABLE_4_AVG_ROW_LENGTH, table3.getAvgRowLength());
        assertEquals(TABLE_4_NUM_ROWS, table3.getNumRows());
        assertEquals(TABLE_4_DATA_LENGTH, table3.getDataLength());
        assertEquals(TABLE_4_MAX_DATA_LENGTH, table3.getMaxDataLength());
        assertNotNull(table3.getCreated());
        /* columns formatted */
        assertEquals(TABLE_4_COLUMNS.size(), table3.getColumns().size());
        for (int i = 0; i < TABLE_4_COLUMNS.size(); i++) {
            assertEquals(TABLE_4_COLUMNS.get(i).getId(), table3.getColumns().get(i).getId());
            assertEquals(TABLE_4_COLUMNS.get(i).getOrdinalPosition(), table3.getColumns().get(i).getOrdinalPosition());
            assertNotNull(table3.getColumns().get(i).getOrdinalPosition());
            assertEquals(TABLE_4_COLUMNS.get(i).getTable().getId(), table3.getColumns().get(i).getTableId());
            assertEquals(TABLE_4_COLUMNS.get(i).getName(), table3.getColumns().get(i).getName());
            assertEquals(TABLE_4_COLUMNS.get(i).getInternalName(), table3.getColumns().get(i).getInternalName());
            assertEquals(List.of(ColumnTypeDto.TIMESTAMP, ColumnTypeDto.DECIMAL).get(i), table3.getColumns().get(i).getColumnType());
            assertEquals(TABLE_4_COLUMNS.get(i).getSize(), table3.getColumns().get(i).getSize());
            assertEquals(TABLE_4_COLUMNS.get(i).getD(), table3.getColumns().get(i).getD());
            assertEquals(TABLE_4_COLUMNS.get(i).getIsNullAllowed(), table3.getColumns().get(i).getIsNullAllowed());
            assertEquals(TABLE_4_COLUMNS.get(i).getAutoGenerated(), table3.getColumns().get(i).getAutoGenerated());
            assertEquals(TABLE_4_COLUMNS.get(i).getEnums(), table3.getColumns().get(i).getEnums());
            assertEquals(TABLE_4_COLUMNS.get(i).getSets(), table3.getColumns().get(i).getSets());
        }
        /* constraints formatted */
        final PrimaryKeyDto table3pk = new ArrayList<>(table3.getConstraints().getPrimaryKey()).get(0);
        assertEquals(TABLE_4_COLUMNS_BRIEF_0_DTO.getId(), table3pk.getColumn().getId());
        assertEquals(TABLE_4_COLUMNS_BRIEF_0_DTO.getName(), table3pk.getColumn().getName());
        assertEquals(TABLE_4_COLUMNS_BRIEF_0_DTO.getId(), table3pk.getColumn().getId());
        assertEquals(TABLE_4_COLUMNS_BRIEF_0_DTO.getName(), table3pk.getColumn().getName());
        assertEquals(TABLE_4_COLUMNS_BRIEF_0_DTO.getInternalName(), table3pk.getColumn().getInternalName());
        assertEquals(ColumnTypeDto.TIMESTAMP, table3pk.getColumn().getColumnType());
        assertNull(table3pk.getColumn().getAlias());
        assertEquals(TABLE_4_ID, table3pk.getColumn().getTableId());
        assertEquals(DATABASE_1_ID, table3pk.getColumn().getDatabaseId());
    }

    public static Stream<Arguments> nameToInternalName_parameters() {
        return Stream.of(
                Arguments.arguments("dash_minus", "OE/NO-027", "oeno-027"),
                Arguments.arguments("percent", "OE%NO-027", "oeno-027"),
                Arguments.arguments("umlaut", "OE/NÖ-027", "oeno-027"),
                Arguments.arguments("dot", "OE.NO-027", "oeno-027")
        );
    }

    @ParameterizedTest
    @MethodSource("nameToInternalName_parameters")
    public void nameToInternalName_succeeds(String name, String request, String compare) {

        /* test */
        final String response = metadataMapper.nameToInternalName(request);
        assertEquals(compare, response);
    }

    @Test
    public void userEquals_fails() {

        /* test */
        assertNotEquals(USER_1_DTO, USER_2_DTO);
    }

    @Test
    public void userEquals_identity_succeeds() {

        /* test */
        assertEquals(USER_1_DTO, USER_1_DTO);
    }

    @Test
    public void userEquals_similar_succeeds() {
        final UserDto tmp = UserDto.builder()
                .id(USER_1_ID)
                .build();

        /* test */
        assertEquals(USER_1_DTO, tmp);
    }

    @Test
    public void userToUserBriefDto_succeeds() {

        /* test */
        final UserBriefDto response = metadataMapper.userToUserBriefDto(USER_1);
        assertEquals(USER_1_NAME, response.getName());
        assertEquals(USER_1_NAME + " — @" + USER_1_USERNAME, response.getQualifiedName());
    }

    @Test
    public void userToUserDto_succeeds() {

        /* test */
        final UserDto response = metadataMapper.userToUserDto(USER_1);
        assertEquals(USER_1_NAME, response.getName());
        assertEquals(USER_1_NAME + " — @" + USER_1_USERNAME, response.getQualifiedName());
    }

    @Test
    public void viewToViewDto_succeeds() {

        /* test */
        final ViewDto response = metadataMapper.viewToViewDto(VIEW_1);
        assertEquals(VIEW_1_ID, response.getId());
        assertEquals(VIEW_1_DATABASE_ID, response.getVdbid());
        assertEquals(VIEW_1_NAME, response.getName());
        assertEquals(VIEW_1_INTERNAL_NAME, response.getInternalName());
        assertNotNull(response.getDatabase());
        assertEquals(VIEW_1_DATABASE_ID, response.getDatabase().getId());
        assertEquals(VIEW_1_QUERY, response.getQuery());
        assertEquals(VIEW_1_QUERY_HASH, response.getQueryHash());
        assertNotNull(response.getIdentifiers());
        assertEquals(1, response.getIdentifiers().size());
        final IdentifierDto identifier0 = response.getIdentifiers().get(0);
        assertEquals(IDENTIFIER_3_ID, identifier0.getId());
        assertEquals(VIEW_1_DATABASE_ID, identifier0.getDatabaseId());
        assertEquals(VIEW_1_ID, identifier0.getViewId());
        assertEquals(VIEW_1_QUERY, identifier0.getQuery());
        assertEquals(VIEW_1_QUERY_HASH, identifier0.getQueryHash());
    }

}
