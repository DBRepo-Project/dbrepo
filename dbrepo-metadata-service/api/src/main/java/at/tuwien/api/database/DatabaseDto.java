package at.tuwien.api.database;

import at.tuwien.api.CacheableDto;
import at.tuwien.api.container.ContainerDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.api.user.UserBriefDto;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class DatabaseDto extends CacheableDto {

    @NotNull
    @Schema(example = "3")
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

    /* lombok limitations prevent from convenient builder functions */

    @JsonProperty("last_retrieved")
    @Schema(example = "2025-01-23T12:09:01")
    private Instant lastRetrieved;

    @ToString.Exclude
    @Schema(example = "mariadb")
    private String jdbcMethod;

    @ToString.Exclude
    @Schema(example = "data-db")
    private String host;

    @ToString.Exclude
    @Schema(example = "3306")
    private Integer port;

    @ToString.Exclude
    @Schema(example = "username")
    private String username;

    @ToString.Exclude
    @JsonIgnore
    private String password;

    @ToString.Exclude
    @Schema(example = "air_quality")
    private String database;

}
