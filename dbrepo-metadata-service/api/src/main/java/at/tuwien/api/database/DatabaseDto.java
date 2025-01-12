package at.tuwien.api.database;

import at.tuwien.api.container.ContainerBriefDto;
import at.tuwien.api.database.table.TableBriefDto;
import at.tuwien.api.identifier.IdentifierBriefDto;
import at.tuwien.api.user.UserBriefDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Getter
@Setter
@Builder
@EqualsAndHashCode
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
    @Schema(example = "dbrepo")
    private String exchangeName;

    @JsonProperty("exchange_type")
    @Schema(example = "topic")
    private String exchangeType;

    @NotBlank
    @JsonProperty("internal_name")
    @Schema(example = "air_quality")
    private String internalName;

    @Schema(example = "Air Quality")
    private String description;

    private List<TableBriefDto> tables;

    private List<ViewBriefDto> views;

    @NotNull
    @JsonProperty("is_public")
    @Schema(example = "true")
    private Boolean isPublic;

    @NotNull
    @JsonProperty("is_schema_public")
    @Schema(example = "true")
    private Boolean isSchemaPublic;

    @NotNull
    private ContainerBriefDto container;

    private List<DatabaseAccessDto> accesses;

    private List<IdentifierBriefDto> identifiers;

    private List<IdentifierBriefDto> subsets;

    @NotNull
    private UserBriefDto contact;

    @NotNull
    private UserBriefDto owner;

    @JsonProperty("preview_image")
    private String previewImage;

}
