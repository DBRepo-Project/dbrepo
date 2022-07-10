package at.tuwien.api.identifier;

import at.tuwien.api.user.UserDto;
import com.fasterxml.jackson.annotation.JsonFormat;
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

    @NotBlank
    @Parameter(name = "query")
    private String query;

    @NotBlank
    @JsonProperty("query_normalized")
    @Parameter(name = "query normalized")
    private String queryNormalized;

    @JsonProperty("related")
    @Parameter(name = "related identifiers")
    private List<RelatedIdentifierDto> related;

    @NotBlank
    @JsonProperty("query_hash")
    @Parameter(name = "query hash in sha512")
    private String queryHash;

    @NotNull
    @Parameter(name = "query execution time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "Europe/Vienna")
    private Instant execution;

    @NotBlank
    @JsonProperty("result_hash")
    @Parameter(name = "result hash in sha512")
    private String resultHash;

    @NotNull
    @JsonProperty("result_number")
    @Parameter(name = "query result number")
    private Long resultNumber;

    @NotNull
    @Parameter(name = "query result visibility")
    private VisibilityTypeDto visibility;

    @Parameter(name = "doi", example = "Digital Object Identifier")
    private String doi;

    @NotNull
    @Parameter(name = "database creator")
    private UserDto creator;

    @NotNull
    @JsonProperty("publication_year")
    @Parameter(name = "publication year", example = "2022")
    private Short publicationYear;

    @NotNull
    @Parameter(name = "creators")
    private List<CreatorDto> creators;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "Europe/Vienna")
    private Instant created;

    @JsonProperty("last_modified")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "Europe/Vienna")
    private Instant lastModified;

}
