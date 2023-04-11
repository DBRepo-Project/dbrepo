package at.tuwien.api.container;

import at.tuwien.api.database.DatabaseBriefDto;
import at.tuwien.api.user.UserBriefDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
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
public class ContainerBriefDto {

    @NotNull
    private Long id;

    @NotNull
    @Schema(example = "f829dd8a884182d0da846f365dee1221fd16610a14c81b8f9f295ff162749e50")
    private String hash;

    @NotBlank
    @Schema(example = "Air Quality")
    private String name;

    @NotNull
    private UserBriefDto creator;

    @NotBlank
    @JsonProperty("internal_name")
    @Schema(example = "air-quality")
    private String internalName;

    @NotNull
    @Schema(example = "true")
    private Boolean running;

    @org.springframework.data.annotation.Transient
    private DatabaseBriefDto database;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;
}
