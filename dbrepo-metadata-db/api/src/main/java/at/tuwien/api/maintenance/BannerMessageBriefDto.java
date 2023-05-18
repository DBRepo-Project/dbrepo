package at.tuwien.api.maintenance;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class BannerMessageBriefDto {

    @NotNull
    private BannerMessageTypeDto type;

    @NotBlank
    @Schema(example = "Maintenance starts on 8am on Monday")
    private String message;

    @Schema(example = "https://example.com")
    private String link;

    @JsonProperty("link_text")
    @Schema(example = "More")
    private String linkText;

}
