package at.ac.tuwien.ifs.dbrepo.core.api.identifier;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public enum IdentifierStatusTypeDto {

    @JsonProperty("draft")
    DRAFT("draft"),

    @JsonProperty("published")
    PUBLISHED("published");

    private final String name;

    IdentifierStatusTypeDto(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
