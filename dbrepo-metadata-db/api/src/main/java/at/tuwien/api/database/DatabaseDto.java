package at.tuwien.api.database;

import at.tuwien.api.container.ContainerDto;
import at.tuwien.api.container.image.ImageDto;
import at.tuwien.api.database.table.TableBriefDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.api.user.UserBriefDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class DatabaseDto {

    @NotNull
    private Long id;

    @NotBlank
    @Schema(example = "Air Quality")
    private String name;

    @NotBlank
    @JsonProperty("exchange_name")
    @Schema(example = "dbrepo/air_quality")
    private String exchangeName;

    private IdentifierDto identifier;

    @NotBlank
    @JsonProperty("internal_name")
    @Schema(example = "weather_australia")
    private String internalName;

    @Schema(example = "Weather Australia 2009-2021")
    private String description;

    private List<TableBriefDto> tables;

    private List<ViewBriefDto> views;

    @JsonProperty("is_public")
    @Schema(example = "true")
    private Boolean isPublic;

    @org.springframework.data.annotation.Transient
    private ImageDto image;

    @org.springframework.data.annotation.Transient
    private ContainerDto container;

    private List<DatabaseAccessDto> accesses;

    @NotNull
    private UserBriefDto creator;

    @NotNull
    private UserBriefDto owner;

    @Schema(example = "2020-08-04 11:12:00")
    @org.springframework.data.annotation.Transient
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;

}
