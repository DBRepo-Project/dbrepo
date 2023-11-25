package at.tuwien.entities.database.table.columns;

import lombok.Getter;
import lombok.ToString;

@Getter
public enum TableColumnType {

    CHAR("char"),

    VARCHAR("varchar"),

    BINARY("binary"),

    VARBINARY("varbinary"),

    TINYBLOB("tinyblob"),

    TINYTEXT("tinytext"),

    TEXT("text"),

    BLOB("blob"),

    MEDIUMTEXT("mediumtext"),

    MEDIUMBLOB("mediumblob"),

    LONGTEXT("longtext"),

    LONGBLOB("longblob"),

    ENUM("enum"),

    SET("set"),

    BIT("bit"),

    TINYINT("tinyint"),

    BOOL("bool"),

    SMALLINT("smallint"),

    MEDIUMINT("mediumint"),

    INT("int"),

    BIGINT("bigint"),

    FLOAT("float"),

    DOUBLE("double"),

    DECIMAL("decimal"),

    DATE("date"),

    DATETIME("datetime"),

    TIMESTAMP("timestamp"),

    TIME("time"),

    YEAR("year");

    private String name;

    TableColumnType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}