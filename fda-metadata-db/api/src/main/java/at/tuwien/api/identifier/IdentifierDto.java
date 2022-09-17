package at.tuwien.api.identifier;

import at.tuwien.api.database.LanguageTypeDto;
import at.tuwien.api.database.LicenseDto;
import at.tuwien.api.user.UserDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(name = "query")
    private String query;

    @NotBlank
    @Schema(name = "publisher")
    private String publisher;

    @Parameter(name = "database license")
    private LicenseDto license;

    @Parameter(name = "database language", example = "EN")
    private LanguageTypeDto language;

    @NotBlank
    @JsonProperty("query_normalized")
    @Schema(name = "query normalized")
    private String queryNormalized;

    @JsonProperty("related")
    @Schema(name = "related identifiers")
    private List<RelatedIdentifierDto> related;

    @NotBlank
    @JsonProperty("query_hash")
    @Schema(name = "query hash in sha512")
    private String queryHash;

    @NotNull
    @Schema(name = "query execution time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant execution;

    @NotBlank
    @JsonProperty("result_hash")
    @Schema(name = "result hash in sha512")
    private String resultHash;

    @NotNull
    @JsonProperty("result_number")
    @Schema(name = "query result number")
    private Long resultNumber;

    @NotNull
    @Schema(name = "query result visibility")
    private VisibilityTypeDto visibility;

    @NotNull
    @Schema(name = "identifier type")
    private IdentifierTypeDto type;

    @Schema(name = "doi", example = "Digital Object Identifier")
    private String doi;

    @NotNull
    @Schema(name = "database creator")
    private UserDto creator;

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
    private List<CreatorDto> creators;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;

    @JsonProperty("last_modified")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant lastModified;

}
