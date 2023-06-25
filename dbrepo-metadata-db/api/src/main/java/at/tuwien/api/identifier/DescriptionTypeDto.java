
package at.tuwien.api.identifier;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public enum DescriptionTypeDto {

    @JsonProperty("Abstract")
    ABSTRACT("Abstract"),

    @JsonProperty("Methods")
    METHODS("Methods"),

    @JsonProperty("SeriesInformation")
    SERIES_INFORMATION("SeriesInformation"),

    @JsonProperty("TableOfContents")
    TABLE_OF_CONTENTS("TableOfContents"),

    @JsonProperty("TechnicalInfo")
    TECHNICAL_INFO("TechnicalInfo"),

    @JsonProperty("Other")
    OTHER("Other");

    private String name;

    DescriptionTypeDto(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
