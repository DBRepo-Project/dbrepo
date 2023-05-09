package at.tuwien.api.identifier;

import at.tuwien.api.database.LanguageTypeDto;
import at.tuwien.api.database.LicenseDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Data
@Getter
@Setter
@Builder
public class IdentifierUpdateDto {

    @NotNull
    private Long cid;

    @NotNull
    private Long dbid;

    private Long qid;

    @Schema(example = "10.1038/nphys1170")
    private String doi;

    @NotNull
    private IdentifierTypeDto type;

    @NotBlank
    @Schema(example = "Airquality Stephansplatz, Vienna, Austria")
    private String title;

    @Schema(example = "Air quality reports at Stephansplatz, Vienna")
    private String description;

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
    private List<CreatorDto> creators;

    @JsonProperty("related_identifiers")
    private List<RelatedIdentifierCreateDto> relatedIdentifiers;

}
