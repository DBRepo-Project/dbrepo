package at.tuwien.api.database.table.constraints.foreignKey;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public enum ReferenceTypeDto {

    @JsonProperty("restrict")
    RESTRICT("RESTRICT"),

    @JsonProperty("cascade")
    CASCADE("CASCADE"),

    @JsonProperty("set_null")
    SET_NULL("SET NULL"),

    @JsonProperty("no_action")
    NO_ACTION("NO ACTION"),

    @JsonProperty("set_default")
    SET_DEFAULT("SET DEFAULT");

    private final String type;

    ReferenceTypeDto(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return this.type;
    }
}
