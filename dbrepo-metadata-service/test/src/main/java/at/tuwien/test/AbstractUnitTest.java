package at.tuwien.test;

import org.springframework.test.context.TestPropertySource;

import java.util.LinkedList;
import java.util.List;

@TestPropertySource(locations = "classpath:application.properties")
public abstract class AbstractUnitTest extends BaseTest {

    public void genesis() {
        /* USER_1 */
        USER_1.setAccesses(new LinkedList<>());
        /* USER_2 */
        USER_2.setAccesses(new LinkedList<>());
        /* USER_3 */
        USER_3.setAccesses(new LinkedList<>());
        /* USER_4 */
        USER_4.setAccesses(new LinkedList<>());
        /* USER_4 */
        USER_5.setAccesses(new LinkedList<>());
        /* DATABASE 1 */
        DATABASE_1.setAccesses(new LinkedList<>(List.of(DATABASE_1_USER_1_READ_ACCESS, DATABASE_1_USER_2_WRITE_OWN_ACCESS, DATABASE_1_USER_3_WRITE_ALL_ACCESS)));
        DATABASE_1_PRIVILEGED_DTO.setAccesses(new LinkedList<>(List.of(DATABASE_1_USER_1_READ_ACCESS_DTO, DATABASE_1_USER_2_WRITE_OWN_ACCESS_DTO, DATABASE_1_USER_3_WRITE_ALL_ACCESS_DTO)));
        TABLE_1.setDatabase(DATABASE_1);
        TABLE_1.setColumns(new LinkedList<>(TABLE_1_COLUMNS));
        TABLE_1.setConstraints(TABLE_1_CONSTRAINTS);
        TABLE_1_PRIVILEGED_DTO.setColumns(new LinkedList<>(TABLE_1_COLUMNS_DTO));
        TABLE_1_PRIVILEGED_DTO.setDatabase(DATABASE_1_PRIVILEGED_DTO);
        DATABASE_1.setIdentifiers(new LinkedList<>(List.of(IDENTIFIER_1, IDENTIFIER_2, IDENTIFIER_3, IDENTIFIER_4)));
        DATABASE_1.setTables(new LinkedList<>(List.of(TABLE_1, TABLE_2, TABLE_3, TABLE_4)));
        DATABASE_1.setViews(new LinkedList<>(List.of(VIEW_1, VIEW_2, VIEW_3)));
        DATABASE_1_PRIVILEGED_DTO.setIdentifiers(new LinkedList<>(List.of(IDENTIFIER_1_DTO, IDENTIFIER_2_DTO, IDENTIFIER_3_DTO, IDENTIFIER_4_DTO)));
        DATABASE_1_PRIVILEGED_DTO.setTables(new LinkedList<>(List.of(TABLE_1_DTO, TABLE_2_DTO, TABLE_3_DTO, TABLE_4_DTO)));
        DATABASE_1_PRIVILEGED_DTO.setViews(new LinkedList<>(List.of(VIEW_1_DTO, VIEW_2_DTO, VIEW_3_DTO)));
        TABLE_1_DTO.setColumns(TABLE_1_COLUMNS_DTO);
        TABLE_1_DTO.setConstraints(TABLE_1_CONSTRAINTS_DTO);
        TABLE_2.setDatabase(DATABASE_1);
        TABLE_2.setColumns(new LinkedList<>(TABLE_2_COLUMNS));
        TABLE_2_CONSTRAINTS.getForeignKeys().get(0).getReferences().get(0).setForeignKey(TABLE_2_CONSTRAINTS.getForeignKeys().get(0));
        TABLE_2.setConstraints(TABLE_2_CONSTRAINTS);
        TABLE_2_PRIVILEGED_DTO.setColumns(new LinkedList<>(TABLE_2_COLUMNS_DTO));
        TABLE_2_DTO.setColumns(TABLE_2_COLUMNS_DTO);
        TABLE_2_DTO.setConstraints(TABLE_2_CONSTRAINTS_DTO);
        TABLE_3.setDatabase(DATABASE_1);
        TABLE_3.setColumns(new LinkedList<>(TABLE_3_COLUMNS));
        TABLE_3.setConstraints(TABLE_3_CONSTRAINTS);
        TABLE_3_DTO.setColumns(TABLE_3_COLUMNS_DTO);
        TABLE_3_DTO.setConstraints(TABLE_3_CONSTRAINTS_DTO);
        TABLE_4.setDatabase(DATABASE_1);
        TABLE_4.setColumns(new LinkedList<>(TABLE_4_COLUMNS));
        TABLE_4.setConstraints(TABLE_4_CONSTRAINTS);
        TABLE_4_DTO.setColumns(TABLE_4_COLUMNS_DTO);
        TABLE_4_DTO.setConstraints(TABLE_4_CONSTRAINTS_DTO);
        VIEW_1.setDatabase(DATABASE_1);
        VIEW_1.setColumns(VIEW_1_COLUMNS);
        VIEW_1.setIdentifiers(new LinkedList<>(List.of(IDENTIFIER_3)));
        VIEW_1_PRIVILEGED_DTO.setDatabase(DATABASE_1_PRIVILEGED_DTO);
        VIEW_2.setDatabase(DATABASE_1);
        VIEW_2.setColumns(VIEW_2_COLUMNS);
        VIEW_2_PRIVILEGED_DTO.setDatabase(DATABASE_1_PRIVILEGED_DTO);
        VIEW_3.setDatabase(DATABASE_1);
        VIEW_3.setColumns(VIEW_3_COLUMNS);
        VIEW_3_PRIVILEGED_DTO.setDatabase(DATABASE_1_PRIVILEGED_DTO);
        IDENTIFIER_1.setDatabase(DATABASE_1);
        IDENTIFIER_2.setDatabase(DATABASE_1);
        IDENTIFIER_3.setDatabase(DATABASE_1);
        IDENTIFIER_4.setDatabase(DATABASE_1);
        /* DATABASE 2 */
        DATABASE_2.setAccesses(new LinkedList<>(List.of(DATABASE_2_USER_2_WRITE_ALL_ACCESS, DATABASE_2_USER_3_READ_ACCESS)));
        DATABASE_2.setTables(new LinkedList<>(List.of(TABLE_5, TABLE_6, TABLE_7)));
        DATABASE_2.setViews(new LinkedList<>(List.of(VIEW_4)));
        DATABASE_2.setIdentifiers(new LinkedList<>(List.of(IDENTIFIER_5)));
        TABLE_5.setDatabase(DATABASE_2);
        TABLE_5.setColumns(new LinkedList<>(TABLE_5_COLUMNS));
        TABLE_5.setConstraints(TABLE_5_CONSTRAINTS);
        TABLE_5_DTO.setColumns(TABLE_5_COLUMNS_DTO);
        TABLE_5_DTO.setConstraints(TABLE_5_CONSTRAINTS_DTO);
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
        TABLE_7_CONSTRAINTS_DTO.getForeignKeys().get(0).getReferences().get(0).setForeignKey(TABLE_7_CONSTRAINTS_DTO.getForeignKeys().get(0));
        TABLE_7_CONSTRAINTS_DTO.getForeignKeys().get(1).getReferences().get(0).setForeignKey(TABLE_7_CONSTRAINTS_DTO.getForeignKeys().get(1));
        VIEW_4.setDatabase(DATABASE_2);
        IDENTIFIER_5.setDatabase(DATABASE_2);
        /* DATABASE 3 */
        DATABASE_3.setAccesses(new LinkedList<>(List.of(DATABASE_3_USER_1_WRITE_ALL_ACCESS)));
        DATABASE_3.setTables(new LinkedList<>(List.of(TABLE_8)));
        DATABASE_3.setViews(new LinkedList<>(List.of(VIEW_5)));
        DATABASE_3.setIdentifiers(new LinkedList<>(List.of(IDENTIFIER_6)));
        TABLE_8.setDatabase(DATABASE_3);
        TABLE_8.setColumns(new LinkedList<>(TABLE_8_COLUMNS));
        TABLE_8.setConstraints(TABLE_8_CONSTRAINTS);
        TABLE_8_DTO.setColumns(new LinkedList<>(TABLE_8_COLUMNS_DTO));
        TABLE_8_DTO.setConstraints(TABLE_8_CONSTRAINTS_DTO);
        TABLE_8_PRIVILEGED_DTO.setColumns(new LinkedList<>(TABLE_8_COLUMNS_DTO));
        TABLE_8_PRIVILEGED_DTO.setConstraints(TABLE_8_CONSTRAINTS_DTO);
        VIEW_5.setDatabase(DATABASE_3);
        VIEW_5.setColumns(VIEW_5_COLUMNS);
        IDENTIFIER_6.setDatabase(DATABASE_3);
        /* DATABASE 4 */
        DATABASE_4.setAccesses(new LinkedList<>(List.of(DATABASE_4_USER_1_READ_ACCESS, DATABASE_4_USER_2_WRITE_OWN_ACCESS, DATABASE_4_USER_3_WRITE_ALL_ACCESS)));
        DATABASE_4.setIdentifiers(new LinkedList<>(List.of(IDENTIFIER_7)));
        IDENTIFIER_7.setDatabase(DATABASE_4);
    }

}
