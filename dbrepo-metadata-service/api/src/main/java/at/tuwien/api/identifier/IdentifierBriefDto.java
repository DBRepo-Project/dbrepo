package at.tuwien.api.identifier;

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
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class IdentifierBriefDto {

    @NotNull
    @Schema(example = "b97cd56b-66ca-4354-9e6c-f47210cfaaec")
    private UUID id;

    @NotNull
    @JsonProperty("database_id")
    @Schema(example = "fc29f89c-86a8-4020-9e36-4d954736c6cc")
    private UUID databaseId;

    @JsonProperty("query_id")
    @Schema(example = "1")
    private UUID queryId;

    @JsonProperty("table_id")
    @Schema(example = "1")
    private UUID tableId;

    @JsonProperty("view_id")
    @Schema(example = "1")
    private UUID viewId;

    @NotNull
    @Schema(example = "database")
    private IdentifierTypeDto type;

    @NotNull
    private List<CreatorBriefDto> creators;

    @NotNull
    private List<IdentifierTitleDto> titles;

    @NotNull
    private List<IdentifierDescriptionDto> descriptions;

    @Schema(example = "10.1038/nphys1170")
    private String doi;

    @NotBlank
    @Schema(example = "TU Wien")
    private String publisher;

    @NotNull
    @JsonProperty("publication_year")
    @Schema(example = "2022")
    private Integer publicationYear;

    @NotNull
    @Schema(example = "draft")
    private IdentifierStatusTypeDto status;

    @NotNull
    @JsonProperty("owned_by")
    @Schema(example = "2f45ef7a-7f9b-4667-9156-152c87fe1ca5")
    private UUID ownedBy;

}
