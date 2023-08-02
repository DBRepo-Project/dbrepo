package at.tuwien.api.container.image;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class ImageChangeDto {

    @NotBlank
    @Schema(example = "docker.io/library")
    private String registry;

    @Min(value = 1024, message = "only user ports are allowed 1024-65535")
    @Max(value = 65535, message = "only user ports are allowed 1024-65535")
    @Schema(example = "5432")
    private Integer defaultPort;

    @NotBlank
    @JsonProperty("driver_class")
    @Schema(example = "org.postgresql.Driver")
    private String driverClass;

    @NotBlank
    @Schema(example = "Postgres")
    private String dialect;

    @NotBlank
    @JsonProperty("jdbc_method")
    @Schema(example = "postgresql")
    private String jdbcMethod;

}
