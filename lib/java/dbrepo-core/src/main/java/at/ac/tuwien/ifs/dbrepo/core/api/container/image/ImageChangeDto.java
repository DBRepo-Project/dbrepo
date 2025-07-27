package at.ac.tuwien.ifs.dbrepo.core.api.container.image;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class ImageChangeDto {

    @NotBlank
    @Schema(description = "The URL of the registry without protocol", example = "docker.io/library")
    private String registry;

    @NotNull
    @JsonProperty("default_port")
    @Min(value = 1024, message = "only user ports are allowed 1024-65535")
    @Max(value = 65535, message = "only user ports are allowed 1024-65535")
    @Schema(description = "The default image port to access the database", example = "3306")
    private Integer defaultPort;

    @NotBlank
    @JsonProperty("driver_class")
    @Schema(description = "The driver class name", example = "org.mariadb.jdbc.Driver")
    private String driverClass;

    @NotBlank
    @Schema(description = "The SQL dialect class name", example = "org.hibernate.dialect.MariaDBDialect")
    private String dialect;

    @NotBlank
    @JsonProperty("jdbc_method")
    @Schema(description = "The method used by JDBC", example = "mariadb")
    private String jdbcMethod;

}
