package at.tuwien.querystore;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum QueryType {

    @JsonProperty("query")
    QUERY("query"),

    @JsonProperty("view")
    VIEW("view");

    private String name;

    QueryType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
