package at.tuwien.api.identifier;

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
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class IdentifierBriefDto {

    @NotNull
    private Long id;

    @NotNull
    @JsonProperty("database_id")
    @Schema(example = "1")
    private Long databaseId;

    @JsonProperty("query_id")
    @Schema(example = "1")
    private Long queryId;

    @JsonProperty("table_id")
    @Schema(example = "1")
    private Long tableId;

    @JsonProperty("view_id")
    @Schema(example = "1")
    private Long viewId;

    @NotNull
    private IdentifierTypeDto type;

    @NotNull
    private List<IdentifierTitleDto> titles;

    @Schema(example = "10.1038/nphys1170")
    private String doi;

    @NotBlank
    @Schema(example = "TU Wien")
    private String publisher;

    @NotNull
    @JsonProperty("publication_year")
    @Schema(example = "2022")
    private Integer publicationYear;

    private IdentifierStatusTypeDto status;

    @NotNull
    @JsonProperty("created_by")
    private UUID createdBy;

}
