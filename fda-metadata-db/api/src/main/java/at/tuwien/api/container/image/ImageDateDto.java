package at.tuwien.api.container.image;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.Instant;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ImageDateDto {

    @NotNull
    @Parameter(required = true, example = "1")
    private Long id;

    @NotBlank
    @Parameter(required = true, example = "30.01.2022")
    private String example;

    @NotBlank
    @JsonProperty("database_format")
    @Parameter(required = true, example = "%d.%c.%Y")
    private String databaseFormat;

    @NotBlank
    @JsonProperty("unix_format")
    @Parameter(required = true, example = "dd.mm.YYYY")
    private String unixFormat;

    @JsonProperty("created_at")
    private Instant createdAt;

}
