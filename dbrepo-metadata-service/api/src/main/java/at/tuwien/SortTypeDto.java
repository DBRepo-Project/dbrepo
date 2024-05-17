package at.tuwien.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum SortTypeDto {

    @JsonProperty("asc")
    ASC("asc"),

    @JsonProperty("desc")
    DESC("desc");

    private String type;

    SortTypeDto(String type) {
        this.type = type;
    }

    public String toString() {
        return this.type;
    }
}
