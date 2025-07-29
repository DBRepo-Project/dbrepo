package at.ac.tuwien.ifs.dbrepo.core.api.identifier;

import at.ac.tuwien.ifs.dbrepo.core.api.database.LanguageTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.LicenseDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

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
public class CreateIdentifierDto {

    @NotNull
    @JsonProperty("database_id")
    @Schema(description = "The id", example = "d002e8d5-8db4-4ff7-ab3a-bc3f52d9ec44")
    private UUID databaseId;

    @JsonProperty("query_id")
    @Schema(description = "The subset id, is only set when type=`subset`", example = "null")
    private UUID queryId;

    @JsonProperty("view_id")
    @Schema(description = "The view id, is only set when type=`view`", example = "null")
    private UUID viewId;

    @JsonProperty("table_id")
    @Schema(description = "The table id, is only set when type=`table`", example = "null")
    private UUID tableId;

    @NotNull
    @Schema(description = "The identifier type", example = "database")
    private IdentifierTypeDto type;

    @Schema(description = "The doi persistent identifier with optional https://doi.org/ prefix", example = "10.1111/11111111")
    private String doi;

    @NotNull
    @NotEmpty
    private List<CreateIdentifierTitleDto> titles = new LinkedList<>();

    @NotNull
    @NotEmpty
    private List<CreateIdentifierDescriptionDto> descriptions = new LinkedList<>();

    @NotNull
    private List<CreateIdentifierFunderDto> funders = new LinkedList<>();

    @NotNull
    private List<LicenseDto> licenses = new LinkedList<>();

    @JsonProperty("publication_day")
    @Schema(description = "The day of publication", example = "15")
    private Integer publicationDay;

    @JsonProperty("publication_month")
    @Schema(description = "The month of publication", example = "12")
    private Integer publicationMonth;

    @NotBlank
    @Schema(description = "The publisher", example = "TU Wien")
    private String publisher;

    @Schema(description = "The language", example = "en")
    private LanguageTypeDto language;

    @NotNull
    @JsonProperty("publication_year")
    @Schema(description = "The year of publication", example = "2022")
    private Integer publicationYear;

    @NotNull
    @NotEmpty
    private List<CreateIdentifierCreatorDto> creators = new LinkedList<>();

    @NotNull
    @JsonProperty("related_identifiers")
    private List<CreateRelatedIdentifierDto> relatedIdentifiers = new LinkedList<>();

}
