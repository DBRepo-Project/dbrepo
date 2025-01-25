package at.tuwien.api.identifier;

import at.tuwien.api.database.LanguageTypeDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.annotation.Id;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class IdentifierTitleDto {

    @NotNull
    @Schema(example = "4")
    private Long id;

    @Schema(example = "Airquality Demonstrator")
    private String title;

    @Schema(example = "en")
    private LanguageTypeDto language;

    @JsonProperty("type")
    private TitleTypeDto titleType;

}
