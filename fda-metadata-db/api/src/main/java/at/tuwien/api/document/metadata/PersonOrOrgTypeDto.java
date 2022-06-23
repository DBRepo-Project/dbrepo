package at.tuwien.api.document.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum PersonOrOrgTypeDto {

    @JsonProperty("personal")
    PERSONAL("personal"),

    @JsonProperty("organizational")
    ORGANIZATIONAL("organizational");

    private final String type;

    PersonOrOrgTypeDto(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }
}
