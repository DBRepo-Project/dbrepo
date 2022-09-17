package at.tuwien.api.identifier;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@Getter
@Setter
@Builder
@Jacksonized
public class IdentifierCreateDto {

    @NotNull
    @JsonProperty("container_id")
    @Schema(name = "container id", example = "1")
    private Long containerId;

    @NotNull
    @JsonProperty("database_id")
    @Schema(name = "database id", example = "1")
    private Long databaseId;

    @JsonProperty("query_id")
    @Schema(name = "query id", example = "1")
    private Long queryId;

    @NotBlank
    @Schema(name = "query title", example = "Select all weather events for 2012")
    private String title;

    @NotBlank
    @Schema(name = "query description", example = "Returns a list of measurements for the year 2012")
    private String description;

    @NotBlank
    @Schema(name = "publisher")
    private String publisher;

    @NotNull
    @Schema(name = "query result visibility")
    private VisibilityTypeDto visibility;

    @NotNull
    @Schema(name = "identifier type")
    private IdentifierTypeDto type;

    @Schema(name = "doi", example = "Digital Object Identifier")
    private String doi;

    @JsonProperty("publication_day")
    @Schema(name = "publication day")
    private Integer publicationDay;

    @JsonProperty("publication_month")
    @Schema(name = "publication month")
    private Integer publicationMonth;

    @NotNull
    @JsonProperty("publication_year")
    @Schema(name = "publication year")
    private Integer publicationYear;

    @NotNull
    @Schema(name = "creators")
    private List<CreatorCreateDto> creators;

    @JsonProperty("related_identifiers")
    @Schema(name = "related identifiers")
    private List<RelatedIdentifierCreateDto> relatedIdentifiers;

}
