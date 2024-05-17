package at.tuwien.api.container.image;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class ImageDto {

    @NotNull
    private Long id;

    @NotBlank
    @Schema(example = "docker.io/library")
    private String registry;

    @NotBlank
    @Schema(example = "mariadb")
    private String name;

    @NotBlank
    @Schema(example = "10.5")
    private String version;

    @NotBlank
    @JsonProperty("driver_class")
    @Schema(example = "org.mariadb.jdbc.Driver")
    private String driverClass;

    @JsonProperty("date_formats")
    private List<ImageDateDto> dateFormats;

    @NotBlank
    @Schema(example = "org.hibernate.dialect.MariaDBDialect")
    private String dialect;

    @NotBlank
    @JsonProperty("jdbc_method")
    @Schema(example = "mariadb")
    private String jdbcMethod;

    @NotNull
    @JsonProperty("default_port")
    @Schema(example = "3306")
    private Integer defaultPort;

}
