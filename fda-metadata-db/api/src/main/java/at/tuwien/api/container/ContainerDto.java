package at.tuwien.api.container;

import at.tuwien.api.container.image.ImageDto;
import at.tuwien.api.database.DatabaseDto;
import at.tuwien.api.user.UserDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContainerDto {

    @NotNull
    @Parameter(name = "id", example = "1")
    private Long id;

    @NotNull
    @Parameter(name = "container hash", example = "f829dd8a884182d0da846f365dee1221fd16610a14c81b8f9f295ff162749e50")
    private String hash;

    @NotBlank
    @Parameter(name = "container name", example = "Weather World")
    private String name;

    @NotBlank
    @JsonProperty("internal_name")
    @Parameter(name = "container internal name", example = "weather-world")
    private String internalName;

    @NotNull
    @Parameter(name = "state", example = "RUNNING")
    private ContainerStateDto state;

    @NotNull
    @ToString.Exclude
    @Parameter(name = "databases")
    private List<DatabaseDto> databases;

    @NotNull
    @JsonProperty("ip_address")
    private String ipAddress;

    @NotNull
    @JsonProperty("is_public")
    @Parameter(name = "container public", example = "true")
    private Boolean isPublic;

    @NotNull
    @Parameter(name = "container image")
    private ImageDto image;

    @NotNull
    @Parameter(name = "container port")
    private Integer port;

    @NotNull
    @Parameter(name = "start time", example = "2021-03-12T15:26:21.678396092Z")
    private Instant created;

}
