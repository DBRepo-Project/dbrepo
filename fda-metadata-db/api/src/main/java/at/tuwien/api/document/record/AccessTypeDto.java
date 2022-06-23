package at.tuwien.api.document.record;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum AccessTypeDto {

    @JsonProperty("public")
    PUBLIC("public"),

    @JsonProperty("restricted")
    RESTRICTED("restricted");

    private final String type;

    AccessTypeDto(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }
}
