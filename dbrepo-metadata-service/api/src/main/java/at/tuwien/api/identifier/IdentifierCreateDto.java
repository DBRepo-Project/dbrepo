package at.tuwien.api.identifier;

import at.tuwien.api.database.LanguageTypeDto;
import at.tuwien.api.database.LicenseDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class IdentifierCreateDto {

    @NotNull
    @JsonProperty("database_id")
    @Schema(example = "1")
    private Long databaseId;

    @JsonProperty("query_id")
    @Schema(example = "null")
    private Long queryId;

    @JsonProperty("view_id")
    @Schema(example = "null")
    private Long viewId;

    @JsonProperty("table_id")
    @Schema(example = "null")
    private Long tableId;

    @NotNull
    @Schema(example = "database")
    private IdentifierTypeDto type;

    @Schema(example = "10.1111/11111111")
    private String doi;

    @NotNull
    @NotEmpty
    private List<IdentifierSaveTitleDto> titles;

    private List<IdentifierSaveDescriptionDto> descriptions;

    private List<IdentifierFunderSaveDto> funders;

    private List<LicenseDto> licenses;

    @JsonProperty("publication_day")
    @Schema(example = "15")
    private Integer publicationDay;

    @JsonProperty("publication_month")
    @Schema(example = "12")
    private Integer publicationMonth;

    @NotBlank
    @Schema(example = "TU Wien")
    private String publisher;

    private LanguageTypeDto language;

    @NotNull
    @JsonProperty("publication_year")
    @Schema(example = "2022")
    private Integer publicationYear;

    @NotNull
    @NotEmpty
    private List<CreatorSaveDto> creators;

    @JsonProperty("related_identifiers")
    private List<RelatedIdentifierSaveDto> relatedIdentifiers;

}
