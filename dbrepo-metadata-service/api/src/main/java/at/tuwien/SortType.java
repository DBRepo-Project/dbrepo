package at.tuwien;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum SortType {

    @JsonProperty("asc")
    ASC("asc"),

    @JsonProperty("desc")
    DESC("desc");

    private String type;

    SortType(String type) {
        this.type = type;
    }

    public String toString() {
        return this.type;
    }
}
