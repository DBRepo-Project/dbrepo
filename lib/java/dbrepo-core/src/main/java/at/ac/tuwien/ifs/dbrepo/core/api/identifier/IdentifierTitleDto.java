package at.ac.tuwien.ifs.dbrepo.core.api.identifier;

import at.ac.tuwien.ifs.dbrepo.core.api.database.LanguageTypeDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

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
    @Schema(example = "70ce5164-fd74-413f-8712-f996b91defbf")
    private UUID id;

    @Schema(example = "Airquality Demonstrator")
    private String title;

    @Schema(example = "en")
    private LanguageTypeDto language;

    @JsonProperty("type")
    private TitleTypeDto titleType;

}
