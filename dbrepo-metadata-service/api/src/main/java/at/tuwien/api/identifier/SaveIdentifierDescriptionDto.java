package at.tuwien.api.identifier;

import at.tuwien.api.database.LanguageTypeDto;
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
public class SaveIdentifierDescriptionDto {

    @NotNull
    @Schema(example = "35bd84d8-b181-43c8-b786-4d024e4f843c")
    private UUID id;

    @NotBlank
    @Schema(example = "Air quality reports at Stephansplatz, Vienna")
    private String description;

    @Schema(example = "en")
    private LanguageTypeDto language;

    @Schema(example = "Abstract")
    @JsonProperty("type")
    private DescriptionTypeDto descriptionType;

}
