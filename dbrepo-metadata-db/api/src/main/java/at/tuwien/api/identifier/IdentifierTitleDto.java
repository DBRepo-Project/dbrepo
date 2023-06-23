package at.tuwien.api.identifier;

import at.tuwien.api.database.LanguageTypeDto;
import at.tuwien.api.database.LicenseDto;
import at.tuwien.api.user.UserDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.elasticsearch.annotations.Document;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class IdentifierTitleDto {

    private Long id;

    @NotNull
    @JsonIgnore
    @Schema(example = "1")
    private Long identifierId;

    @Schema(example = "Airquality Demonstrator")
    private String title;

    @Schema(example = "en")
    private LanguageTypeDto language;

    @JsonProperty("type")
    private TitleTypeDto titleType;

}
