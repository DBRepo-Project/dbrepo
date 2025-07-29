package at.ac.tuwien.ifs.dbrepo.core.api.identifier;

import at.ac.tuwien.ifs.dbrepo.core.api.database.LanguageTypeDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class CreateIdentifierDescriptionDto {

    @NotBlank
    @Schema(description = "The description value", example = "Air quality reports at Stephansplatz, Vienna")
    private String description;

    @Schema(description = "The language", example = "en")
    private LanguageTypeDto language;

    @JsonProperty("type")
    @Schema(description = "The type", example = "Abstract")
    private DescriptionTypeDto descriptionType;

}
