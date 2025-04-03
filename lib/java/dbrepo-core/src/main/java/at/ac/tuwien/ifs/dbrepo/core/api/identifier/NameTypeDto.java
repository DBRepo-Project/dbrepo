package at.ac.tuwien.ifs.dbrepo.core.api.identifier;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public enum NameTypeDto {

    @JsonProperty("Personal")
    PERSONAL("Personal"),

    @JsonProperty("Organizational")
    ORGANIZATIONAL("Organizational");

    private final String name;

    NameTypeDto(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
