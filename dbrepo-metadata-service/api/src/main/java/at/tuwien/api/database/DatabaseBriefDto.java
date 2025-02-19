package at.tuwien.api.database;

import at.tuwien.api.identifier.IdentifierBriefDto;
import at.tuwien.api.user.UserBriefDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class DatabaseBriefDto {

    @NotNull
    @Schema(example = "fc29f89c-86a8-4020-9e36-4d954736c6cc")
    private UUID id;

    @NotBlank
    @Schema(example = "Air Quality")
    private String name;

    @NotBlank
    @JsonProperty("internal_name")
    @Schema(example = "air_quality")
    private String internalName;

    @Schema(example = "Air Quality")
    private String description;

    @NotNull
    @JsonProperty("is_public")
    @Schema(example = "true")
    private Boolean isPublic;

    @NotNull
    @JsonProperty("is_schema_public")
    @Schema(example = "true")
    private Boolean isSchemaPublic;

    @NotNull
    private List<IdentifierBriefDto> identifiers;

    @NotNull
    private UserBriefDto contact;

    @NotNull
    @JsonProperty("owner_id")
    @Schema(example = "2f45ef7a-7f9b-4667-9156-152c87fe1ca5")
    private UUID ownerId;

    @JsonProperty("preview_image")
    private String previewImage;

}
