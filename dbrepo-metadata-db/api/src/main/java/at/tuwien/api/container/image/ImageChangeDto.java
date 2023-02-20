package at.tuwien.api.container.image;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.util.List;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ImageChangeDto {

    @Min(value = 1024, message = "only user ports are allowed 1024-65535")
    @Max(value = 65535, message = "only user ports are allowed 1024-65535")
    @Schema(example = "5432")
    private Integer defaultPort;

    private List<ImageEnvItemDto> environment;

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
