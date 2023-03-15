package at.tuwien.api.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserThemeSetDto {

    @NotNull
    @JsonProperty("theme_dark")
    @Schema(example = "true")
    private Boolean themeDark;
}
