package at.tuwien.api.doi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class LicenseDto {

    private TimeRepresentationDto start;

    @JsonProperty("content-version")
    @Schema(example = "stm-asf")
    private String contentVersion;

    @JsonProperty("delay-in-days")
    @Schema(example = "0")
    private Integer delayInDays;

    @JsonProperty("URL")
    @Schema(example = "https://doi.org/10.15223/policy-029")
    private String url;

}
