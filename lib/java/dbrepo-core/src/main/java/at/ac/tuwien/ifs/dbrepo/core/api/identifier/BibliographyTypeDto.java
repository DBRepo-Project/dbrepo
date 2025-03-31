package at.ac.tuwien.ifs.dbrepo.core.api.identifier;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public enum BibliographyTypeDto {

    @JsonProperty("apa")
    APA("apa"),

    @JsonProperty("ieee")
    IEEE("ieee"),

    @JsonProperty("bibtex")
    BIBTEX("bibtex");

    private final String name;

    BibliographyTypeDto(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
