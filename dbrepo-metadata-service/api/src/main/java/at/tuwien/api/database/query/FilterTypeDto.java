package at.tuwien.api.database.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public enum FilterTypeDto {

    @JsonProperty("where")
    WHERE("where"),

    @JsonProperty("or")
    OR("or"),

    @JsonProperty("and")
    AND("and");

    private String name;

    FilterTypeDto(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
