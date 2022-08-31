package at.tuwien.api.database.table.columns;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public enum ColumnTypeDto {

    @JsonProperty("enum")
    ENUM("enum"),

    @JsonProperty("number")
    NUMBER("number"),

    @JsonProperty("decimal")
    DECIMAL("decimal"),

    @JsonProperty("string")
    STRING("string"),

    @JsonProperty("text")
    TEXT("text"),

    @JsonProperty("boolean")
    BOOLEAN("boolean"),

    @JsonProperty("date")
    DATE("date"),

    @JsonProperty("timestamp")
    TIMESTAMP("timestamp"),

    @JsonProperty("blob")
    BLOB("blob");

    private String type;

    ColumnTypeDto(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return this.type;
    }
}
