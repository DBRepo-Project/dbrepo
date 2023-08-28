
package at.tuwien.entities.identifier;

import lombok.Getter;

@Getter
public enum TitleType {

    ALTERNATIVE_TITLE("AlternativeTitle"),

    SUBTITLE("Subtitle"),

    TRANSLATED_TITLE("TranslatedTitle"),

    OTHER("Other");

    private String name;

    TitleType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
