package at.tuwien.api.database;

import at.tuwien.api.container.ContainerBriefDto;
import at.tuwien.api.container.image.ImageDto;
import at.tuwien.api.database.table.TableBriefDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.api.user.UserBriefDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class DatabaseBriefDto {

    @NotNull
    private Long id;

    @NotBlank
    @Schema(example = "Air Quality")
    private String name;

    @NotBlank
    @JsonProperty("exchange_name")
    @Schema(example = "dbrepo")
    private String exchangeName;

    @JsonProperty("exchange_type")
    @Schema(example = "topic")
    private String exchangeType;

    private IdentifierDto identifier;

    @NotBlank
    @JsonProperty("internal_name")
    @Schema(example = "air_quality")
    private String internalName;

    @Schema(example = "Air Quality")
    private String description;

    @org.springframework.data.annotation.Transient
    private List<TableBriefDto> tables;

    @org.springframework.data.annotation.Transient
    private List<ViewBriefDto> views;

    @JsonProperty("is_public")
    @Schema(example = "true")
    private Boolean isPublic;

    @org.springframework.data.annotation.Transient
    private ImageDto image;

    private ContainerBriefDto container;

    @org.springframework.data.annotation.Transient
    private List<DatabaseAccessDto> accesses;

    @NotNull
    private UserBriefDto creator;

    @NotNull
    private UserBriefDto owner;

    @NotNull
    @Schema(example = "2021-03-12T15:26:21Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;

}
