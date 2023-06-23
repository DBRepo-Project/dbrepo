package at.tuwien.api.identifier;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public enum NameTypeDto {

    @JsonProperty("Personal")
    PERSONAL("Personal"),

    @JsonProperty("Organizational")
    ORGANIZATIONAL("Organizational");

    private String name;

    NameTypeDto(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
