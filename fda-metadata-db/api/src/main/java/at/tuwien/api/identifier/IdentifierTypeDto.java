package at.tuwien.api.identifier;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum IdentifierTypeDto {

    @JsonProperty("DATABASE")
    DATABASE("database"),

    @JsonProperty("SUBSET")
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
