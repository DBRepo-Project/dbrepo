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
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class ImageDto {

    @NotNull
    @Schema(example = "1")
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

    @NotBlank
    @Schema(example = "org.hibernate.dialect.MariaDBDialect")
    private String dialect;

    @NotBlank
    @JsonProperty("jdbc_method")
    @Schema(example = "mariadb")
    private String jdbcMethod;

    @NotNull
    @JsonProperty("default")
    @Schema(example = "false")
    private Boolean isDefault;

    @NotNull
    @JsonProperty("default_port")
    @Schema(example = "3306")
    private Integer defaultPort;

    @NotNull
    @JsonProperty("data_types")
    private List<DataTypeDto> dataTypes;

    @NotNull
    private List<OperatorDto> operators;

}
