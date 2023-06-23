
package at.tuwien.api.identifier;

import lombok.Getter;

@Getter
public enum DescriptionTypeDto {

    ABSTRACT("Abstract"),

    METHODS("Methods"),

    SERIES_INFORMATION("SeriesInformation"),

    TABLE_OF_CONTENTS("TableOfContents"),

    TECHNICAL_INFO("TechnicalInfo"),

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
