package at.tuwien.api.identifier;

import at.tuwien.api.database.LanguageTypeDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class IdentifierSaveTitleDto {

    private Long id;

    @Schema(example = "Airquality Demonstrator")
    private String title;

    @Schema(example = "en")
    private LanguageTypeDto language;

    @JsonProperty("type")
    @Schema(example = "Subtitle")
    private TitleTypeDto titleType;

}
