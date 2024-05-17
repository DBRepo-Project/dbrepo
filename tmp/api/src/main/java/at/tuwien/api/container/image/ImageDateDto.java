package at.tuwien.api.container.image;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class ImageDateDto {

    @NotNull
    private Long id;

    @NotBlank
    @JsonProperty("database_format")
    @Schema(example = "%d.%c.%Y")
    private String databaseFormat;

    @NotBlank
    @JsonProperty("unix_format")
    @Schema(example = "dd.MM.YYYY")
    private String unixFormat;

    @NotNull
    @JsonProperty("has_time")
    @Schema(example = "false")
    private Boolean hasTime;


    @NotNull
    @Schema(example = "2021-03-12T15:26:21Z")
    @JsonProperty("created_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant createdAt;

}
