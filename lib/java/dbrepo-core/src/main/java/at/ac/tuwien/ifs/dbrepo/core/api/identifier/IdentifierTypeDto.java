package at.ac.tuwien.ifs.dbrepo.core.api.identifier;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public enum IdentifierTypeDto {

    @JsonProperty("database")
    DATABASE("database"),

    @JsonProperty("subset")
    SUBSET("subset"),

    @JsonProperty("table")
    TABLE("table"),

    @JsonProperty("view")
    VIEW("view");

    private final String name;

    IdentifierTypeDto(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
