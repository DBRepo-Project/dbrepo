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
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class CreateIdentifierDto {

    @NotNull
    @JsonProperty("database_id")
    @Schema(example = "d002e8d5-8db4-4ff7-ab3a-bc3f52d9ec44")
    private UUID databaseId;

    @JsonProperty("query_id")
    @Schema(example = "null")
    private UUID queryId;

    @JsonProperty("view_id")
    @Schema(example = "null")
    private UUID viewId;

    @JsonProperty("table_id")
    @Schema(example = "null")
    private UUID tableId;

    @NotNull
    @Schema(example = "database")
    private IdentifierTypeDto type;

    @Schema(example = "10.1111/11111111")
    private String doi;

    @NotNull
    @NotEmpty
    private List<SaveIdentifierTitleDto> titles = new LinkedList<>();

    @NotNull
    @NotEmpty
    private List<SaveIdentifierDescriptionDto> descriptions = new LinkedList<>();

    @NotNull
    private List<SaveIdentifierFunderDto> funders = new LinkedList<>();

    @NotNull
    private List<LicenseDto> licenses = new LinkedList<>();

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
    private List<SaveIdentifierCreatorDto> creators = new LinkedList<>();

    @NotNull
    @JsonProperty("related_identifiers")
    private List<SaveRelatedIdentifierDto> relatedIdentifiers = new LinkedList<>();

}
