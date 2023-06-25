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
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class IdentifierCreateDto {

    @NotNull
    @Schema(example = "1")
    private Long dbid;

    @Schema(example = "null")
    private Long qid;

    @NotNull
    @Schema(example = "database")
    private IdentifierTypeDto type;

    private List<IdentifierCreateTitleDto> titles;

    private List<IdentifierCreateDescriptionDto> descriptions;

    @NotNull
    @Schema(example = "everyone")
    private VisibilityTypeDto visibility;

    @JsonProperty("publication_day")
    @Schema(example = "15")
    private Integer publicationDay;

    @JsonProperty("publication_month")
    @Schema(example = "12")
    private Integer publicationMonth;

    @Schema(example = "TU Wien")
    private String publisher;

    private LanguageTypeDto language;

    private LicenseDto license;

    @NotNull
    @JsonProperty("publication_year")
    @Schema(example = "2022")
    private Integer publicationYear;

    @NotNull
    @NotEmpty
    private List<CreatorCreateDto> creators;

    @JsonProperty("related_identifiers")
    private List<RelatedIdentifierCreateDto> relatedIdentifiers;

}
