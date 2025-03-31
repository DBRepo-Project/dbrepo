package at.ac.tuwien.ifs.dbrepo.core.api.identifier;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public enum IdentifierFunderTypeDto {

    @JsonProperty("Crossref Funder ID")
    CROSSREF_FUNDER_ID("Crossref Funder ID"),

    @JsonProperty("ROR")
    ROR("ROR"),

    @JsonProperty("GND")
    GND("GND"),

    @JsonProperty("ISNI")
    ISNI("ISNI"),

    @JsonProperty("Other")
    OTHER("Other");

    private final String name;

    IdentifierFunderTypeDto(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
