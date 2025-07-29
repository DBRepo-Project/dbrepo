package at.ac.tuwien.ifs.dbrepo.core.api.database;

import at.ac.tuwien.ifs.dbrepo.core.api.CacheableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.container.ContainerDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.identifier.IdentifierDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserBriefDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class DatabaseDto extends CacheableDto {

    @NotNull
    @Schema(example = "fc29f89c-86a8-4020-9e36-4d954736c6cc")
    private UUID id;

    @NotBlank
    @Schema(example = "Air Quality")
    private String name;

    @JsonProperty("dashboard_uid")
    @Schema(example = "abcdef")
    private String dashboardUid;

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

    @NotNull
    private List<TableDto> tables;

    @NotNull
    private List<ViewDto> views;

    @NotNull
    @JsonProperty("is_public")
    @Schema(example = "true")
    private Boolean isPublic;

    @NotNull
    @JsonProperty("is_schema_public")
    @Schema(example = "true")
    private Boolean isSchemaPublic;

    @NotNull
    @JsonProperty("is_dashboard_enabled")
    @Schema(example = "true")
    private Boolean isDashboardEnabled;

    private ContainerDto container;

    @NotNull
    private List<DatabaseAccessDto> accesses;

    @NotNull
    private List<IdentifierDto> identifiers;

    @NotNull
    private List<IdentifierDto> subsets;

    @NotNull
    private UserBriefDto contact;

    @NotNull
    private UserBriefDto owner;

    @JsonProperty("preview_image")
    private String previewImage;

    @JsonProperty("replica_urls")
    @Schema(example = "[\"https://replica1.example.com\", \"https://replica2.example.com\"]", nullable = true)
    private List<String> replicaUrls;

    @NotNull
    @Schema(example = "2022-01-01 08:00:00.000")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    private Instant created;

    /* lombok limitations prevent from convenient builder functions */

    @JsonProperty("last_retrieved")
    @Schema(example = "2025-01-23T12:09:01")
    private Instant lastRetrieved;

}
