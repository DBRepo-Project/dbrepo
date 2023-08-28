
package at.tuwien.entities.identifier;

import lombok.Getter;

@Getter
public enum DescriptionType {

    ABSTRACT("Abstract"),

    METHODS("Methods"),

    SERIES_INFORMATION("SeriesInformation"),

    TABLE_OF_CONTENTS("TableOfContents"),

    TECHNICAL_INFO("TechnicalInfo"),

    OTHER("Other");

    private String name;

    DescriptionType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
