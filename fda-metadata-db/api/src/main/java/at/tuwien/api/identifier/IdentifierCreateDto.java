package at.tuwien.api.identifier;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

@Data
@Getter
@Setter
@Builder
@Jacksonized
public class IdentifierCreateDto {

    @NotNull
    @Parameter(name = "query id", example = "1")
    private Long qid;

    @NotBlank
    @Parameter(name = "query title", example = "Select all weather events for 2012")
    private String title;

    @NotBlank
    @Parameter(name = "query description", example = "Returns a list of measurements for the year 2012")
    private String description;

    @NotNull
    @Parameter(name = "query result visibility")
    private VisibilityTypeDto visibility;

    @Parameter(name = "doi", example = "Digital Object Identifier")
    private String doi;

    @JsonProperty("publication_day")
    @Parameter(name = "publication day")
    private Integer publicationDay;

    @JsonProperty("publication_month")
    @Parameter(name = "publication month")
    private Integer publicationMonth;

    @NotNull
    @JsonProperty("publication_year")
    @Parameter(name = "publication year")
    private Integer publicationYear;

    @NotNull
    @Parameter(name = "creators")
    private List<CreatorCreateDto> creators;

    @JsonProperty("related_identifiers")
    @Parameter(name = "related identifiers")
    private List<RelatedIdentifierCreateDto> relatedIdentifiers;

}
