package at.tuwien.api.document.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum IdentifierTypeDto {

    @JsonProperty("orcid")
    ORCID("orcid"),

    @JsonProperty("gnd")
    GND("gnd"),

    @JsonProperty("isni")
    ISNI("isni"),

    @JsonProperty("ror")
    ROR("ror");

    private final String type;

    IdentifierTypeDto(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }
}
