package at.tuwien.api.container.image;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.persistence.Column;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ImageCreateDto {

    @NotBlank
    @Parameter(required = true, example = "postgres")
    private String repository;

    @NotBlank
    @Parameter(required = true, example = "latest")
    private String tag;

    @NotBlank
    @JsonProperty("driver_class")
    @Parameter(required = true, example = "org.postgresql.Driver")
    private String driverClass;

    @NotBlank
    @Parameter(required = true, example = "POSTGRES")
    private String dialect;

    @NotBlank
    @Parameter(required = true, example = "base64:aaaa")
    private String logo;

    @NotBlank
    @JsonProperty("jdbc_method")
    @Parameter(required = true, example = "postgresql")
    private String jdbcMethod;

    @NotNull
    @Parameter(required = true, example = "false")
    private Boolean local;

    @NotNull
    @JsonProperty("default_port")
    @Parameter(required = true, example = "5432")
    private Integer defaultPort;

    @Parameter(required = true, example = "[{\"key\":\"POSTGRES_USER\",\"value\":\"postgres\",\"type\":USERNAME},{\"key\":\"POSTGRES_PASSWORD\",\"value\":\"postgres\",\"type\":PASSWORD}]")
    private ImageEnvItemDto[] environment;

}
