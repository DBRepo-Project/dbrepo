package at.tuwien.api.maintenance;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class BannerMessageDto {

    @NotNull
    @Schema(example = "ae3f795b-a3da-4ebe-bdc4-21a8ce631e6f")
    private UUID id;

    @NotNull
    @Schema(example = "WARNING")
    private BannerMessageTypeDto type;

    @NotBlank
    @Schema(example = "Maintenance starts on 8am on Monday")
    private String message;

    @Schema(example = "https://example.com")
    private String link;

    @JsonProperty("link_text")
    @Schema(example = "More")
    private String linkText;

    @JsonProperty("display_start")
    @Schema(example = "2021-03-12T15:26:21Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant displayStart;

    @JsonProperty("display_end")
    @Schema(example = "2021-03-12T15:26:21Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant displayEnd;

}
