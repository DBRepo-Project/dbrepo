package at.tuwien.api.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.validation.constraints.NotNull;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class UserThemeSetDto {

    @NotNull
    @JsonProperty("theme_dark")
    @Schema(example = "true")
    private Boolean themeDark;
}
