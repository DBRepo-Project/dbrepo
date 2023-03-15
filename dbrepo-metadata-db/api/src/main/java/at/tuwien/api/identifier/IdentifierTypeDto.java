package at.tuwien.api.identifier;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum IdentifierTypeDto {

    @JsonProperty("database")
    DATABASE("database"),

    @JsonProperty("subset")
    SUBSET("subset");

    private String name;

    IdentifierTypeDto(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
