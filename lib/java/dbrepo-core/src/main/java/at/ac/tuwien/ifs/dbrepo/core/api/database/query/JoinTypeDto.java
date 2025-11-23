package at.ac.tuwien.ifs.dbrepo.core.api.database.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public enum JoinTypeDto {

    @JsonProperty("inner")
    INNER("inner"),

    @JsonProperty("left")
    LEFT("left"),

    @JsonProperty("right")
    RIGHT("right"),

    @JsonProperty("cross")
    CROSS("cross");

    private final String name;

    JoinTypeDto(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
