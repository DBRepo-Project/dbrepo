package at.tuwien.api.datacite.doi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public enum DataCiteNameType {

    @JsonProperty("Personal")
    PERSONAL("Personal"),

    @JsonProperty("Organizational")
    ORGANIZATIONAL("Organizational");

    private String name;

    DataCiteNameType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
