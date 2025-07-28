package at.ac.tuwien.ifs.dbrepo.core.api.identifier;

import at.ac.tuwien.ifs.dbrepo.core.api.database.LanguageTypeDto;
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
public class CreateIdentifierTitleDto {

    @NotBlank
    @Schema(description = "The title", example = "Airquality Demonstrator")
    private String title;

    @Schema(description = "The language", example = "en")
    private LanguageTypeDto language;

    @JsonProperty("type")
    @Schema(description = "The type", example = "Subtitle")
    private TitleTypeDto titleType;

}
