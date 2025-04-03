
package at.ac.tuwien.ifs.dbrepo.core.api.identifier;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public enum TitleTypeDto {

    @JsonProperty("AlternativeTitle")
    ALTERNATIVE_TITLE("AlternativeTitle"),

    @JsonProperty("Subtitle")
    SUBTITLE("Subtitle"),

    @JsonProperty("TranslatedTitle")
    TRANSLATED_TITLE("TranslatedTitle"),

    @JsonProperty("Other")
    OTHER("Other");

    private final String name;

    TitleTypeDto(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
