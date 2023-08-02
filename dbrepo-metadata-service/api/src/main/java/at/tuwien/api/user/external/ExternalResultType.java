package at.tuwien.api.user.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public enum ExternalResultType {

    @JsonProperty("Personal")
    PERSONAL("Personal"),

    @JsonProperty("Organizational")
    ORGANIZATIONAL("Organizational");

    private String name;

    ExternalResultType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
