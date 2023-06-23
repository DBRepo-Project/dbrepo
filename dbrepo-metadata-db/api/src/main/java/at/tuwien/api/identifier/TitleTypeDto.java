
package at.tuwien.api.identifier;

import lombok.Getter;

@Getter
public enum TitleTypeDto {

    ALTERNATIVE_TITLE("AlternativeTitle"),

    SUBTITLE("Subtitle"),

    TRANSLATED_TITLE("TranslatedTitle"),

    OTHER("Other");

    private String name;

    TitleTypeDto(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
