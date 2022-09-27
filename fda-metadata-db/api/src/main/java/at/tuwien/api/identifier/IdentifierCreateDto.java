package at.tuwien.api.identifier;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@Getter
@Setter
@Builder
public class IdentifierCreateDto {

    @NotNull
    private Long qid;

    @NotBlank
    @Schema(example = "Airquality Stephansplatz, Vienna, Austria")
    private String title;

    @NotBlank
    @Schema(example = "Air quality reports at Stephansplatz, Vienna")
    private String description;

    @NotNull
    @Schema(example = "everyone")
    private VisibilityTypeDto visibility;

    @Schema(example = "10.1038/nphys1170")
    private String doi;

    @JsonProperty("publication_day")
    @Schema(example = "15")
    private Integer publicationDay;

    @JsonProperty("publication_month")
    @Schema(example = "12")
    private Integer publicationMonth;

    @Schema(example = "TU Wien")
    private String publisher;

    @NotNull
    @JsonProperty("publication_year")
    @Schema(example = "2022")
    private Integer publicationYear;

    @NotNull
    private List<CreatorCreateDto> creators;

    @JsonProperty("related_identifiers")
    private List<RelatedIdentifierCreateDto> relatedIdentifiers;

}
