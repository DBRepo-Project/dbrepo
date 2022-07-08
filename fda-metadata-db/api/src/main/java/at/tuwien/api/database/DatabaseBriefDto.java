package at.tuwien.api.database;

import at.tuwien.api.user.UserDto;
import com.fasterxml.jackson.annotation.JsonFormat;
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
public class DatabaseBriefDto {

    @NotNull(message = "database id is required")
    @Parameter(name = "database id", example = "1")
    private Long id;

    @NotBlank(message = "name is required")
    @Parameter(name = "database name", example = "Weather Australia")
    private String name;

    @Parameter(name = "database description", example = "Weather in Australia")
    private String description;

    @JsonProperty("is_public")
    @Parameter(name = "database visibility")
    private Boolean isPublic;

    @Parameter(name = "database engine", example = "mariadb:latest")
    private String engine;

    @Parameter(name = "database creator")
    private UserDto creator;

    @Parameter(name = "database creation time", example = "2020-08-04 11:12:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    private Instant created;

}
