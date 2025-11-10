package at.ac.tuwien.ifs.dbrepo.core.api.identifier;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;
import java.util.LinkedList;
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
    @Schema(example = "1413e257-f62a-4881-ac38-ae9a76e21b9c")
    private UUID queryId;

    @JsonProperty("table_id")
    @Schema(example = "67d23209-aec8-489a-8f86-71fd0fe09ce7")
    private UUID tableId;

    @JsonProperty("view_id")
    @Schema(example = "8f860779-3ab1-4f83-ae6c-b89ebbf1dcc4")
    private UUID viewId;

    @NotNull
    @Schema(example = "database")
    private IdentifierTypeDto type;

    @NotNull
    private List<CreatorBriefDto> creators = new LinkedList<>();

    @NotNull
    private List<IdentifierTitleDto> titles = new LinkedList<>();

    @NotNull
    private List<IdentifierDescriptionDto> descriptions = new LinkedList<>();

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
    @Schema(description = "The owner username", example = "foobar")
    private String ownedBy;

    @EqualsAndHashCode.Exclude
    @LastModifiedDate
    @Column(columnDefinition = "TIMESTAMP")
    private Instant created;

}
