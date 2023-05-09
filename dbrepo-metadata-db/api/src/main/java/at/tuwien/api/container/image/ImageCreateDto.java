package at.tuwien.api.container.image;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ImageCreateDto {

    @NotBlank
    @Schema(example = "mariadb")
    private String repository;

    @NotBlank
    @Parameter(example = "10.5")
    private String tag;

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
    @Parameter(required = true, example = "3006")
    private Integer defaultPort;

    private List<ImageEnvItemDto> environment;

}
