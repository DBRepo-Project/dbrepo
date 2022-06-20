package at.tuwien.api.identifier;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

@Data
@Getter
@Setter
@Builder
public class IdentifierDto {

    private Long id;

    @NotNull
    @Parameter(name = "container id", example = "1")
    private Long cid;

    @NotNull
    @Parameter(name = "database id", example = "1")
    private Long dbid;

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
    private VisibilityTypeDto visibility;

    @Parameter(name = "doi", example = "Digital Object Identifier")
    private String doi;

    @NotNull
    @JsonProperty("publication_year")
    @Parameter(name = "publication year", example = "2022")
    private Short publicationYear;

    @NotNull
    @Parameter(name = "creators")
    private List<CreatorDto> creators;

    private Instant created;

    @JsonProperty("last_modified")
    private Instant lastModified;

}
