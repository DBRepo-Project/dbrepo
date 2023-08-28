package at.tuwien.api.datacite.doi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import jakarta.validation.constraints.NotBlank;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class DataCiteDoiTitle implements Serializable {

    @NotBlank
    @Field(name="title", type = FieldType.Text)
    private String title;

    @Field(name="title_type", type = FieldType.Keyword)
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
