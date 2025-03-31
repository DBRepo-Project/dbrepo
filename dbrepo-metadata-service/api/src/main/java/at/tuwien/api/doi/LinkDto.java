package at.tuwien.api.doi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
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
public class LinkDto {

    @JsonProperty("URL")
    @Schema(example = "http://xplorestaging.ieee.org/ielx8/10824975/10824942/10825401.pdf?arnumber=10825401")
    private String url;

    @JsonProperty("content-type")
    @Schema(example = "unspecified")
    private String contentType;

    @JsonProperty("content-version")
    @Schema(example = "vor")
    private String contentVersion;

    @JsonProperty("intended-application")
    @Schema(example = "similarity-checking")
    private String intendedApplication;

}
