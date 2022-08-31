package at.tuwien.api.document.record;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum FileTypeDto {

    @JsonProperty("public")
    PUBLIC("public"),

    @JsonProperty("restricted")
    RESTRICTED("restricted");

    private final String type;

    FileTypeDto(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }
}
