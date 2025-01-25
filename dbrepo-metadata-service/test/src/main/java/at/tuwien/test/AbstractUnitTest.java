package at.tuwien.test;

import at.tuwien.entities.database.AccessType;
import at.tuwien.entities.identifier.IdentifierStatusType;
import org.springframework.test.context.TestPropertySource;

import java.util.LinkedList;
import java.util.List;

@TestPropertySource(locations = "classpath:application.properties")
public abstract class AbstractUnitTest extends BaseTest {

    public void genesis() {
        IMAGE_1_DTO.setOperators(IMAGE_1_OPERATORS_DTO);
        CONTAINER_1_DTO.setImage(IMAGE_1_DTO);
        IMAGE_1.setOperators(new LinkedList<>(IMAGE_1_OPERATORS));
        CONTAINER_1.setDatabases(new LinkedList<>(List.of(DATABASE_1, DATABASE_2, DATABASE_3)));
        CONTAINER_4.setDatabases(new LinkedList<>(List.of(DATABASE_4)));
        /* USER_1 */
        USER_1.setAccesses(new LinkedList<>());
        USER_1.setTheme(USER_1_THEME);
        /* USER_2 */
        USER_2.setAccesses(new LinkedList<>());
        /* USER_3 */
        USER_3.setAccesses(new LinkedList<>());
        /* USER_4 */
        USER_4.setAccesses(new LinkedList<>());
        /* USER_4 */
        USER_5.setAccesses(new LinkedList<>());
        /* DATABASE 1 */
        TABLE_1_COLUMNS.get(0).setUnit(null);
        TABLE_1_COLUMNS.get(0).setConcept(null);
        DATABASE_1.setOwner(USER_1);
        DATABASE_1.setSubsets(new LinkedList<>());
        DATABASE_1.setIsPublic(DATABASE_1_PUBLIC);
        DATABASE_1.setIsSchemaPublic(DATABASE_1_SCHEMA_PUBLIC);
        DATABASE_1_USER_1_READ_ACCESS.setType(AccessType.READ);
        DATABASE_1.setAccesses(new LinkedList<>(List.of(DATABASE_1_USER_1_READ_ACCESS, DATABASE_1_USER_2_WRITE_OWN_ACCESS, DATABASE_1_USER_3_WRITE_ALL_ACCESS)));
        DATABASE_1_DTO.setAccesses(new LinkedList<>(List.of(DATABASE_1_USER_1_READ_ACCESS_DTO, DATABASE_1_USER_2_WRITE_OWN_ACCESS_DTO, DATABASE_1_USER_3_WRITE_ALL_ACCESS_DTO)));
        DATABASE_1_PRIVILEGED_DTO.setAccesses(new LinkedList<>(List.of(DATABASE_1_USER_1_READ_ACCESS_DTO, DATABASE_1_USER_2_WRITE_OWN_ACCESS_DTO, DATABASE_1_USER_3_WRITE_ALL_ACCESS_DTO)));
        UNIT_1.setId(UNIT_1_ID);
        TABLE_1.setDatabase(DATABASE_1);
        TABLE_1.setColumns(new LinkedList<>(TABLE_1_COLUMNS));
        TABLE_1.setConstraints(TABLE_1_CONSTRAINTS);
        VIEW_1_DTO.setIdentifiers(VIEW_1_DTO_IDENTIFIERS);
        DATABASE_1.setIdentifiers(new LinkedList<>(List.of(IDENTIFIER_1, IDENTIFIER_2, IDENTIFIER_3, IDENTIFIER_4)));
        IDENTIFIER_1.setDatabase(DATABASE_1);
        IDENTIFIER_2.setDatabase(DATABASE_1);
        IDENTIFIER_3.setDatabase(DATABASE_1);
        IDENTIFIER_4.setDatabase(DATABASE_1);
        DATABASE_1.setTables(new LinkedList<>(List.of(TABLE_1, TABLE_2, TABLE_3, TABLE_4)));
        DATABASE_1.setViews(new LinkedList<>(List.of(VIEW_1, VIEW_2, VIEW_3)));
        DATABASE_1_DTO.setContainer(CONTAINER_1_DTO);
        DATABASE_1_DTO.setIdentifiers(new LinkedList<>(List.of(IDENTIFIER_1_DTO, IDENTIFIER_2_DTO, IDENTIFIER_3_DTO, IDENTIFIER_4_DTO)));
        DATABASE_1_DTO.setTables(new LinkedList<>(List.of(TABLE_1_DTO, TABLE_2_DTO, TABLE_3_DTO, TABLE_4_DTO)));
        DATABASE_1_DTO.setViews(new LinkedList<>(List.of(VIEW_1_DTO, VIEW_2_DTO, VIEW_3_DTO)));
        TABLE_1_DTO.setColumns(new LinkedList<>(TABLE_1_COLUMNS_DTO));
        TABLE_1_DTO.setConstraints(TABLE_1_CONSTRAINTS_DTO);
        TABLE_1_PRIVILEGED_DTO.setDatabase(DATABASE_1_PRIVILEGED_DTO);
        TABLE_1_PRIVILEGED_DTO.setColumns(new LinkedList<>(TABLE_1_COLUMNS_DTO));
        TABLE_1_PRIVILEGED_DTO.setConstraints(TABLE_1_CONSTRAINTS_DTO);
        TABLE_2.setDatabase(DATABASE_1);
        TABLE_2.setColumns(new LinkedList<>(TABLE_2_COLUMNS));
        TABLE_2_CONSTRAINTS.getForeignKeys().get(0).getReferences().get(0).setForeignKey(TABLE_2_CONSTRAINTS.getForeignKeys().get(0));
        TABLE_2.setConstraints(TABLE_2_CONSTRAINTS);
        TABLE_2_DTO.setColumns(new LinkedList<>(TABLE_2_COLUMNS_DTO));
        TABLE_2_DTO.setConstraints(TABLE_2_CONSTRAINTS_DTO);
        TABLE_2_PRIVILEGED_DTO.setDatabase(DATABASE_1_PRIVILEGED_DTO);
        TABLE_2_PRIVILEGED_DTO.setColumns(new LinkedList<>(TABLE_2_COLUMNS_DTO));
        TABLE_2_PRIVILEGED_DTO.setConstraints(TABLE_2_CONSTRAINTS_DTO);
        TABLE_3.setDatabase(DATABASE_1);
        TABLE_3.setColumns(new LinkedList<>(TABLE_3_COLUMNS));
        TABLE_3.setConstraints(TABLE_3_CONSTRAINTS);
        TABLE_3_DTO.setColumns(new LinkedList<>(TABLE_3_COLUMNS_DTO));
        TABLE_3_DTO.setConstraints(TABLE_3_CONSTRAINTS_DTO);
        TABLE_4.setDatabase(DATABASE_1);
        TABLE_4.setColumns(new LinkedList<>(TABLE_4_COLUMNS));
        TABLE_4.setConstraints(TABLE_4_CONSTRAINTS);
        TABLE_4_DTO.setColumns(TABLE_4_COLUMNS_DTO);
        TABLE_4_DTO.setConstraints(TABLE_4_CONSTRAINTS_DTO);
        TABLE_4_PRIVILEGED_DTO.setDatabase(DATABASE_1_PRIVILEGED_DTO);
        TABLE_4_PRIVILEGED_DTO.setColumns(new LinkedList<>(TABLE_4_COLUMNS_DTO));
        TABLE_4_PRIVILEGED_DTO.setConstraints(TABLE_4_CONSTRAINTS_DTO);
        VIEW_1.setDatabase(DATABASE_1);
        VIEW_1.setColumns(new LinkedList<>(VIEW_1_COLUMNS));
        VIEW_1.setIdentifiers(new LinkedList<>(List.of(IDENTIFIER_3)));
        VIEW_1_PRIVILEGED_DTO.setDatabase(DATABASE_1_PRIVILEGED_DTO);
        VIEW_2.setDatabase(DATABASE_1);
        VIEW_2.setColumns(new LinkedList<>(VIEW_2_COLUMNS));
        VIEW_2_PRIVILEGED_DTO.setDatabase(DATABASE_1_PRIVILEGED_DTO);
        VIEW_3.setDatabase(DATABASE_1);
        VIEW_3.setColumns(new LinkedList<>(VIEW_3_COLUMNS));
        VIEW_3_PRIVILEGED_DTO.setDatabase(DATABASE_1_PRIVILEGED_DTO);
        IDENTIFIER_1.setDatabase(DATABASE_1);
        IDENTIFIER_2.setDatabase(DATABASE_1);
        IDENTIFIER_3.setDatabase(DATABASE_1);
        IDENTIFIER_4.setDatabase(DATABASE_1);
        /* DATABASE 2 */
        DATABASE_2.setSubsets(new LinkedList<>());
        DATABASE_2.setAccesses(new LinkedList<>(List.of(DATABASE_2_USER_2_WRITE_ALL_ACCESS, DATABASE_2_USER_3_READ_ACCESS)));
        DATABASE_2_DTO.setAccesses(new LinkedList<>(List.of(DATABASE_2_USER_2_WRITE_ALL_ACCESS_DTO, DATABASE_2_USER_3_READ_ACCESS_DTO)));
        DATABASE_2_PRIVILEGED_DTO.setAccesses(new LinkedList<>(List.of(DATABASE_2_USER_2_WRITE_ALL_ACCESS_DTO, DATABASE_2_USER_3_READ_ACCESS_DTO)));
        DATABASE_2.setTables(new LinkedList<>(List.of(TABLE_5, TABLE_6, TABLE_7)));
        VIEW_4.setColumns(new LinkedList<>(VIEW_4_COLUMNS));
        DATABASE_2.setViews(new LinkedList<>(List.of(VIEW_4)));
        DATABASE_2.setIdentifiers(new LinkedList<>(List.of(IDENTIFIER_5)));
        DATABASE_2_DTO.setTables(new LinkedList<>(List.of(TABLE_5_DTO, TABLE_6_DTO, TABLE_7_DTO)));
        DATABASE_2_DTO.setViews(new LinkedList<>(List.of(VIEW_4_DTO)));
        DATABASE_2_DTO.setIdentifiers(new LinkedList<>(List.of(IDENTIFIER_5_DTO)));
        TABLE_5.setDatabase(DATABASE_2);
        TABLE_5.setColumns(new LinkedList<>(TABLE_5_COLUMNS));
        TABLE_5.setConstraints(TABLE_5_CONSTRAINTS);
        TABLE_5_DTO.setColumns(new LinkedList<>(TABLE_5_COLUMNS_DTO));
        TABLE_5_DTO.setConstraints(TABLE_5_CONSTRAINTS_DTO);
        TABLE_5_PRIVILEGED_DTO.setDatabase(DATABASE_2_PRIVILEGED_DTO);
        TABLE_6.setDatabase(DATABASE_2);
        TABLE_6.setColumns(new LinkedList<>(TABLE_6_COLUMNS));
        TABLE_6.setConstraints(TABLE_6_CONSTRAINTS);
        TABLE_7.setDatabase(DATABASE_2);
        TABLE_7.setColumns(new LinkedList<>(TABLE_7_COLUMNS));
        TABLE_7.setConstraints(TABLE_7_CONSTRAINTS);
        TABLE_7_CONSTRAINTS.getForeignKeys().get(0).getReferences().get(0).setForeignKey(TABLE_7_CONSTRAINTS.getForeignKeys().get(0));
        TABLE_7_CONSTRAINTS.getForeignKeys().get(1).getReferences().get(0).setForeignKey(TABLE_7_CONSTRAINTS.getForeignKeys().get(1));
        TABLE_7_DTO.setColumns(TABLE_7_COLUMNS_DTO);
        TABLE_7_DTO.setConstraints(TABLE_7_CONSTRAINTS_DTO);
        TABLE_7_CONSTRAINTS_DTO.getForeignKeys().get(0).getReferences().get(0).setForeignKey(TABLE_7_CONSTRAINTS_FOREIGN_KEY_BRIEF_0_DTO);
        TABLE_7_CONSTRAINTS_DTO.getForeignKeys().get(1).getReferences().get(0).setForeignKey(TABLE_7_CONSTRAINTS_FOREIGN_KEY_BRIEF_1_DTO);
        VIEW_4.setDatabase(DATABASE_2);
        IDENTIFIER_5.setDatabase(DATABASE_2);
        /* DATABASE 3 */
        DATABASE_3.setSubsets(new LinkedList<>());
        DATABASE_3.setTables(new LinkedList<>(List.of(TABLE_8)));
        DATABASE_3.setViews(new LinkedList<>(List.of(VIEW_5)));
        DATABASE_3.setIdentifiers(new LinkedList<>(List.of(IDENTIFIER_6)));
        DATABASE_3.setAccesses(new LinkedList<>(List.of(DATABASE_3_USER_1_WRITE_ALL_ACCESS)));
        DATABASE_3_DTO.setTables(new LinkedList<>(List.of(TABLE_8_DTO)));
        DATABASE_3_DTO.setViews(new LinkedList<>(List.of(VIEW_5_DTO)));
        DATABASE_3_DTO.setIdentifiers(new LinkedList<>(List.of(IDENTIFIER_6_DTO)));
        DATABASE_3_DTO.setAccesses(new LinkedList<>(List.of(DATABASE_3_USER_1_WRITE_ALL_ACCESS_DTO)));
        DATABASE_3_PRIVILEGED_DTO.setAccesses(new LinkedList<>(List.of(DATABASE_3_USER_1_WRITE_ALL_ACCESS_DTO)));
        TABLE_8.setDatabase(DATABASE_3);
        TABLE_8.setColumns(new LinkedList<>(TABLE_8_COLUMNS));
        TABLE_8.setConstraints(TABLE_8_CONSTRAINTS);
        TABLE_8_DTO.setColumns(new LinkedList<>(TABLE_8_COLUMNS_DTO));
        TABLE_8_DTO.setConstraints(TABLE_8_CONSTRAINTS_DTO);
        TABLE_8_PRIVILEGED_DTO.setColumns(new LinkedList<>(TABLE_8_COLUMNS_DTO));
        TABLE_8_PRIVILEGED_DTO.setDatabase(DATABASE_3_PRIVILEGED_DTO);
        VIEW_5.setDatabase(DATABASE_3);
        VIEW_5.setColumns(VIEW_5_COLUMNS);
        VIEW_5_DTO.setColumns(VIEW_5_COLUMNS_DTO);
        IDENTIFIER_6.setDatabase(DATABASE_3);
        /* DATABASE 4 */
        DATABASE_4.setSubsets(new LinkedList<>());
        DATABASE_4.setAccesses(new LinkedList<>(List.of(DATABASE_4_USER_1_READ_ACCESS, DATABASE_4_USER_2_WRITE_OWN_ACCESS, DATABASE_4_USER_3_WRITE_ALL_ACCESS)));
        DATABASE_4.setIdentifiers(new LinkedList<>(List.of(IDENTIFIER_7)));
        DATABASE_4_DTO.setTables(new LinkedList<>(List.of(TABLE_9_DTO)));
        DATABASE_4_DTO.setIdentifiers(new LinkedList<>(List.of(IDENTIFIER_7_DTO)));
        TABLE_9.setDatabase(DATABASE_4);
        TABLE_9.setColumns(TABLE_9_COLUMNS);
        TABLE_9.setConstraints(TABLE_9_CONSTRAINTS);
        TABLE_9_DTO.setColumns(TABLE_9_COLUMNS_DTO);
        TABLE_9_DTO.setConstraints(TABLE_9_CONSTRAINTS_DTO);
        IDENTIFIER_7.setStatus(IdentifierStatusType.DRAFT);
        IDENTIFIER_7.setDatabase(DATABASE_4);
    }

}
