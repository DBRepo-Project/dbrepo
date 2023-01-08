package at.tuwien.api.database;

import at.tuwien.api.container.ContainerBriefDto;
import at.tuwien.api.identifier.IdentifierDto;
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
public class DatabaseBriefDto {

    @NotNull(message = "database id is required")
    private Long id;

    @NotBlank(message = "name is required")
    @Schema(example = "Air Quality")
    private String name;

    @Schema(example = "Air Quality in Austria")
    private String description;

    private IdentifierDto identifier;

    @JsonProperty("is_public")
    @Schema(example = "true")
    private Boolean isPublic;

    @Schema(example = "mariadb:10.5")
    private String engine;

    private ContainerBriefDto container;

    private UserBriefDto creator;

    @Schema(example = "2020-08-04 11:12:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;

}
