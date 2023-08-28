package at.tuwien.api.container.image;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
public class ImageCreateDto {

    @NotBlank
    @Schema(example = "docker.io/library")
    private String registry;

    @NotBlank
    @Schema(example = "mariadb")
    private String name;

    @NotBlank
    @Parameter(example = "10.5")
    private String version;

    @NotBlank
    @JsonProperty("driver_class")
    @Parameter(example = "'org.mariadb.jdbc.Driver")
    private String driverClass;

    @NotBlank
    @Parameter(required = true, example = "org.hibernate.dialect.MariaDBDialect")
    private String dialect;

    @NotBlank
    @JsonProperty("jdbc_method")
    @Parameter(required = true, example = "mariadb")
    private String jdbcMethod;

    @NotNull
    @JsonProperty("default_port")
    @Min(value = 1024, message = "only user ports are allowed 1024-65535")
    @Max(value = 65535, message = "only user ports are allowed 1024-65535")
    @Parameter(required = true, example = "3006")
    private Integer defaultPort;

}
