package at.tuwien.api.database;

import at.tuwien.api.container.ContainerBriefDto;
import at.tuwien.api.identifier.IdentifierBriefDto;
import at.tuwien.api.user.UserBriefDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class DatabaseBriefDto {

    @NotNull(message = "database id is required")
    private Long id;

    @NotBlank(message = "name is required")
    @Schema(example = "Air Quality")
    private String name;

    @Schema(example = "Air Quality in Austria")
    private String description;

    private IdentifierBriefDto identifier;

    @JsonProperty("is_public")
    @Schema(example = "true")
    private Boolean isPublic;

    @Schema(example = "mariadb:10.5")
    private String engine;

    @NotNull
    private UserBriefDto owner;

    @ToString.Exclude
    @org.springframework.data.annotation.Transient
    private ContainerBriefDto container;

    private UserBriefDto creator;

    @Schema(example = "2020-08-04 11:12:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;

}
