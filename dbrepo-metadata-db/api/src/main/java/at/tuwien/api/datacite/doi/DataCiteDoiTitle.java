package at.tuwien.api.datacite.doi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import jakarta.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DataCiteDoiTitle implements Serializable {

    @NotBlank
    private String title;

    private Type titleType;

    private String lang;

    public enum Type {

        @JsonProperty("AlternativeTitle")
        ALTERNATIVE_TITLE("AlternativeTitle"),

        @JsonProperty("Subtitle")
        SUBTITLE("Subtitle"),

        @JsonProperty("TranslatedTitle")
        TRANSLATED_TITLE("TranslatedTitle"),

        @JsonProperty("Other")
        OTHER("Other");

        private final String name;

        Type(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }
}
