package at.tuwien.api.identifier;

import at.tuwien.api.database.LanguageTypeDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class IdentifierSaveDescriptionDto {

    @NotBlank
    @Schema(example = "Air quality reports at Stephansplatz, Vienna")
    private String description;

    @Schema(example = "en")
    private LanguageTypeDto language;

    @Schema(example = "Abstract")
    @JsonProperty("type")
    private DescriptionTypeDto descriptionType;

}
