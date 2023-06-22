package at.tuwien.api.user.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public enum ExternalResultType {

    @JsonProperty("person")
    PERSON("person"),

    @JsonProperty("organization")
    ORGANIZATION("organization");

    private String name;

    ExternalResultType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
