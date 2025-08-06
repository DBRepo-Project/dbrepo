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
    @Schema(description = "The id", example = "fc29f89c-86a8-4020-9e36-4d954736c6cc")
    private UUID id;

    @NotBlank
    @Schema(description = "The user-friendly name", example = "Air Quality")
    private String name;

    @JsonProperty("dashboard_uid")
    @Schema(description = "The grafana dashboard unique id", example = "abcdef")
    private String dashboardUid;

    @NotBlank
    @JsonProperty("exchange_name")
    @Schema(description = "The exchange name", example = "dbrepo")
    private String exchangeName;

    @JsonProperty("exchange_type")
    @Schema(description = "The exchange type", example = "topic")
    private String exchangeType;

    @NotBlank
    @JsonProperty("internal_name")
    @Schema(description = "The machine-friendly database name", example = "air_quality")
    private String internalName;

    @Schema(description = "The comment", example = "Air Quality")
    private String description;

    @NotNull
    private List<TableDto> tables;

    @NotNull
    private List<ViewDto> views;

    @NotNull
    @JsonProperty("is_public")
    @Schema(description = "The visibility; if true, The will be displayed publicly and is searchable", example = "true")
    private Boolean isPublic;

    @NotNull
    @JsonProperty("is_schema_public")
    @Schema(description = "The insights; if true, The schema will be displayed publicly and is searchable", example = "true")
    private Boolean isSchemaPublic;

    @NotNull
    @JsonProperty("is_dashboard_enabled")
    @Schema(description = "If true, the dashboard will be managed", example = "true")
    private Boolean isDashboardEnabled;

    private ContainerDto container;

    @NotNull
    private List<DatabaseAccessDto> accesses;

    @NotNull
    @Schema(description = "The list of identifiers", example = "[]")
    private List<IdentifierDto> identifiers;

    @NotNull
    @Schema(description = "The list of subsets", example = "[]")
    private List<IdentifierDto> subsets;

    @NotNull
    private UserBriefDto contact;

    @NotNull
    private UserBriefDto owner;

    @JsonProperty("preview_image")
    @Schema(description = "The preview image", example = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABkAAAAXCAMAAADJPRQhAAAAAXNSR0IB2cksfwAAAAlwSFlzAAALEwAACxMBAJqcGAAAAXRQTFRFAAAAAGaZAGaZAGaZAGaZAGaZAGaZAGaZAGaZAGaZAGaZAGaZAGaZAGaZAGaZAGaZAGaZAGaZAGaZAGaZAGaZAGaZAGaZAGaZAGaZAWaZOYiwb6nGfLHLX5+/GnWjAGaZPYux1OXu/////P3+pMnbEnGgAGaZudXj/v//UJa5AGaZaaXD+Pv89fn7utbjs9HhzOHrI3unAGaZbKfFKoCqibjQrM7eLoKsM4WtPIqxCmydAGaZAGaZvNfk1ebubajFCWucjrvS5vD12+nxvtjlDG2eAGaZN4ev0ePtG3ak4O3zAWeay+DqUZe5AGaZAGaZrs/fQ460GXWjFnOiA2iax97pU5i6AGaZAGaZ/f7+1+fvLYGrmcLW0+XtwNnm+/39YaHAAGaZVpq8pcnbjbvSTpW4WJu8T5W52OjvRY+0AGaZAGaZRY+1MIOsAGaZAGaZAGaZAGaZAGaZAGaZAGaZAGaZAGaZAGaZAGaZAGaZAGaZAGaZI9A4qQAAAHx0Uk5TAAE8k97224ksiAmb+v/0hwP+jkzy+5eu5/////////f////////j////vf////////+F//////////8o5v///////////33/////////Esr/////////D9r//////////9H//////////yHH//8I1tkMjwWB/Gj4V0PTRBXfRw8AAAFGSURBVHicbZDPK0RxFMXPkdAzMszCj0KUWdgQ2ZhsKCllpexYSGkWSkr8AUpKymKyErJRbJTEQjZKWckOC0WxQskzGua67/v9vhlqzuae8/287rv3EqBTGqoy/ogRCE8fgzctb9HgSaXFZ/Tdg1Uxs0Xy7YJfwRi/XNBOkg59qbDGR0F5rP0oTMoZobMRM+BrSIRNL9bFzITKnm1OM/5kXX3GlBI+2lzHtgfrGulrb9WdzQ3svLUu7i5x7f7TygSvjOsIj3RhYrsQA5dm7577Zj1PFXlm7tF9QgzzNPD9+vmndzzIoyD1yYGOOspD9ZW9rtu+hiHZRbBEIr4HdLXomtUB2sHIzTkMQZLbuauMc3NMUnAE09zIX2wiu4YcwcxWJgSTsoo/BLPkelCnRFbwjwBz1O5JWQ5znmBeUkkuoQDBArmYT7/cX1c496CkMgAAAABJRU5ErkJggg==")
    private String previewImage;

    @JsonProperty("replica_urls")
    @Schema(example = "[\"https://replica1.example.com\", \"https://replica2.example.com\"]", nullable = true)
    private List<String> replicaUrls;

    @NotNull
    @Schema(description = "The created timestamp", example = "2022-01-01 08:00:00.000")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    private Instant created;

    /* lombok limitations prevent from convenient builder functions */

    @JsonProperty("last_retrieved")
    @Schema(description = "The timestamp The was last retrieved from the cache", example = "2025-01-23T12:09:01")
    private Instant lastRetrieved;

}
