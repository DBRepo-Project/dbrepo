package at.tuwien.api.database.table.constraints.foreign;

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

    public static ReferenceTypeDto fromType(String type) {
        return switch (type) {
            case "RESTRICT" -> ReferenceTypeDto.RESTRICT;
            case "CASCADE" -> ReferenceTypeDto.CASCADE;
            case "SET NULL" -> ReferenceTypeDto.SET_NULL;
            case "NO ACTION" -> ReferenceTypeDto.NO_ACTION;
            case "SET DEFAULT" -> ReferenceTypeDto.SET_DEFAULT;
            default -> null;
        };
    }

    @Override
    public String toString() {
        return this.type;
    }
}
