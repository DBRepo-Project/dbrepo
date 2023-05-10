package at.tuwien.api.identifier;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

@Getter
public enum VisibilityTypeDto {

    @JsonProperty("everyone")
    EVERYONE("everyone"),

    @JsonProperty("self")
    SELF("self");

    private String name;

    VisibilityTypeDto(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}