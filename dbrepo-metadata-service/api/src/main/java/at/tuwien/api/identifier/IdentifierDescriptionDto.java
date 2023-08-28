package at.tuwien.api.identifier;

import at.tuwien.api.database.LanguageTypeDto;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
public class IdentifierDescriptionDto {

    @Id
    @NotNull
    @Field(name = "id", type = FieldType.Keyword)
    private Long id;

    @Schema(example = "Air quality reports at Stephansplatz, Vienna")
    @Field(name = "description", type = FieldType.Text)
    private String description;

    @Schema(example = "en")
    @Field(name = "language", type = FieldType.Keyword)
    private LanguageTypeDto language;

    @JsonProperty("type")
    @Schema(example = "Abstract")
    @Field(name = "type", type = FieldType.Keyword)
    private DescriptionTypeDto descriptionType;

}
