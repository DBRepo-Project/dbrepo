package at.tuwien.api.identifier;

import at.tuwien.api.database.LanguageTypeDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class IdentifierTitleDto {

    @Id
    @NotNull
    @Field(name = "id", type = FieldType.Keyword)
    private Long id;

    @Schema(example = "Airquality Demonstrator")
    @Field(name = "title", type = FieldType.Keyword)
    private String title;

    @Schema(example = "en")
    @Field(name = "language", type = FieldType.Keyword)
    private LanguageTypeDto language;

    @JsonProperty("type")
    @Field(name = "type", type = FieldType.Keyword)
    private TitleTypeDto titleType;

}
