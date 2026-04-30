package at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/* MYSQL 8 */
@Getter
@Schema
public enum ColumnTypeDto {

    @JsonProperty("char")
    CHAR("char"),

    @JsonProperty("varchar")
    VARCHAR("varchar"),

    @JsonProperty("binary")
    BINARY("binary"),

    @JsonProperty("varbinary")
    VARBINARY("varbinary"),

    @JsonProperty("real")
    REAL("real"),

    @JsonProperty("tinyblob")
    TINYBLOB("tinyblob"),

    @JsonProperty("tinytext")
    TINYTEXT("tinytext"),

    @JsonProperty("text")
    TEXT("text"),

    @JsonProperty("blob")
    BLOB("blob"),

    @JsonProperty("mediumtext")
    MEDIUMTEXT("mediumtext"),

    @JsonProperty("mediumblob")
    MEDIUMBLOB("mediumblob"),

    @JsonProperty("longtext")
    LONGTEXT("longtext"),

    @JsonProperty("longblob")
    LONGBLOB("longblob"),

    @JsonProperty("enum")
    ENUM("enum"),

    @JsonProperty("set")
    SET("set"),

    @JsonProperty("serial")
    SERIAL("serial"),

    @JsonProperty("bit")
    BIT("bit"),

    @JsonProperty("tinyint")
    TINYINT("tinyint"),

    @JsonProperty("bool")
    BOOL("bool"),

    @JsonProperty("smallint")
    SMALLINT("smallint"),

    @JsonProperty("mediumint")
    MEDIUMINT("mediumint"),

    @JsonProperty("int")
    INT("int"),

    @JsonProperty("bigint")
    BIGINT("bigint"),

    @JsonProperty("float")
    FLOAT("float"),

    @JsonProperty("double")
    DOUBLE("double"),

    @JsonProperty("decimal")
    DECIMAL("decimal"),

    @JsonProperty("date")
    DATE("date"),

    @JsonProperty("datetime")
    DATETIME("datetime"),

    @JsonProperty("timestamp")
    TIMESTAMP("timestamp"),

    @JsonProperty("time")
    TIME("time"),

    @JsonProperty("year")
    YEAR("year");

    private final String type;

    ColumnTypeDto(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return this.type;
    }
}
