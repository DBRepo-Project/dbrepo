package at.tuwien;

import at.tuwien.test.BaseTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

@TestPropertySource(locations = "classpath:application.properties")
public abstract class BaseUnitTest extends BaseTest {

    public void genesis() {
        /* DATABASE 1 */
        DATABASE_1.setAccesses(List.of());
        TABLE_1.setDatabase(DATABASE_1);
        TABLE_1.setColumns(TABLE_1_COLUMNS);
        TABLE_1_FOREIGN_KEY_1.getReferences().add(TABLE_1_FOREIGN_KEY_REFERENCE);
        TABLE_1.getConstraints().getForeignKeys().add(TABLE_1_FOREIGN_KEY_1);
        TABLE_1.getConstraints().getChecks().add(TABLE_1_CHECK_1);
//        TABLE_1.getConstraints().getUniques().add(TABLE_1_UNIQUE_CONSTRAINT_1);
        TABLE_2.setDatabase(DATABASE_1);
        TABLE_2.setColumns(TABLE_2_COLUMNS);
        TABLE_2.setConstraints(TABLE_2_CONSTRAINTS);
        TABLE_3.setDatabase(DATABASE_1);
        TABLE_3.setColumns(TABLE_3_COLUMNS);
        TABLE_3.setConstraints(TABLE_3_CONSTRAINTS);
        TABLE_4.setDatabase(DATABASE_1);
        TABLE_4.setColumns(TABLE_4_COLUMNS);
        TABLE_4.setConstraints(TABLE_4_CONSTRAINTS);
        VIEW_1.setDatabase(DATABASE_1);
        VIEW_1.setColumns(VIEW_1_COLUMNS);
        VIEW_2.setDatabase(DATABASE_1);
        VIEW_2.setColumns(VIEW_2_COLUMNS);
        VIEW_3.setDatabase(DATABASE_1);
        VIEW_3.setColumns(VIEW_3_COLUMNS);
        IDENTIFIER_1.setDatabase(DATABASE_1);
        IDENTIFIER_2.setDatabase(DATABASE_1);
        IDENTIFIER_3.setDatabase(DATABASE_1);
        IDENTIFIER_4.setDatabase(DATABASE_1);
        /* DATABASE 2 */
        DATABASE_2.setAccesses(List.of());
        TABLE_5.setDatabase(DATABASE_2);
        TABLE_5.setColumns(TABLE_5_COLUMNS);
        TABLE_6.setDatabase(DATABASE_2);
        TABLE_6.setColumns(TABLE_6_COLUMNS);
        TABLE_7.setDatabase(DATABASE_2);
        TABLE_7.setColumns(TABLE_7_COLUMNS);
        VIEW_4.setDatabase(DATABASE_2);
        VIEW_4.setColumns(VIEW_4_COLUMNS);
        IDENTIFIER_5.setDatabase(DATABASE_2);
        /* DATABASE 3 */
        DATABASE_3.setAccesses(List.of());
        TABLE_8.setDatabase(DATABASE_3);
        TABLE_8.setColumns(TABLE_8_COLUMNS);
        VIEW_5.setDatabase(DATABASE_3);
        VIEW_5.setColumns(VIEW_5_COLUMNS);
        IDENTIFIER_6.setDatabase(DATABASE_3);
    }

}
