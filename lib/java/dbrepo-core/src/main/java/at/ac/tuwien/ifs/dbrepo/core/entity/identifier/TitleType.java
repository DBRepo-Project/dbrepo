
package at.ac.tuwien.ifs.dbrepo.core.entity.identifier;

import lombok.Getter;

@Getter
public enum TitleType {

    ALTERNATIVE_TITLE("AlternativeTitle"),

    SUBTITLE("Subtitle"),

    TRANSLATED_TITLE("TranslatedTitle"),

    OTHER("Other");

    private final String name;

    TitleType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
