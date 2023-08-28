package at.tuwien.api.database.query;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum QueryTypeDto {

    @JsonProperty("query")
    QUERY("query"),

    @JsonProperty("view")
    VIEW("view");

    private String name;

    QueryTypeDto(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
