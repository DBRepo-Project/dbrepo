package at.tuwien.api.identifier;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public enum IdentifierStatusTypeDto {

    @JsonProperty("draft")
    DRAFT("draft"),

    @JsonProperty("published")
    PUBLISHED("published");

    private String name;

    IdentifierStatusTypeDto(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
