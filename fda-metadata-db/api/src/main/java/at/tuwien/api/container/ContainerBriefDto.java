package at.tuwien.api.container;

import at.tuwien.api.user.UserBriefDto;
import at.tuwien.api.user.UserDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContainerBriefDto {

    @NotNull
    @Parameter(name = "id", example = "1")
    private Long id;

    @NotNull
    @Parameter(name = "container hash", example = "f829dd8a884182d0da846f365dee1221fd16610a14c81b8f9f295ff162749e50")
    private String hash;

    @NotBlank
    @Parameter(name = "container name", example = "Weather World")
    private String name;

    @Parameter(name = "container creator")
    private UserBriefDto creator;

    @NotBlank
    @JsonProperty("internal_name")
    @Parameter(name = "container internal name", example = "weather-world")
    private String internalName;

    @Parameter(name = "container created")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;
}
