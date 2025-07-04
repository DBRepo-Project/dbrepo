package at.ac.tuwien.ifs.dbrepo.core.api.identifier;

import at.ac.tuwien.ifs.dbrepo.core.api.database.LanguageTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.LicenseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserBriefDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class IdentifierDto {

    @NotNull
    @Schema(example = "b97cd56b-66ca-4354-9e6c-f47210cfaaec")
    private UUID id;

    @NotNull
    @JsonProperty("database_id")
    @Schema(example = "null")
    private UUID databaseId;

    @JsonProperty("query_id")
    @Schema(example = "null")
    private UUID queryId;

    @JsonProperty("table_id")
    @Schema(example = "null")
    private UUID tableId;

    @JsonProperty("view_id")
    @Schema(example = "null")
    private UUID viewId;

    @NotNull
    private LinksDto links;

    @NotNull
    @Schema(example = "database")
    private IdentifierTypeDto type;

    @NotNull
    private List<IdentifierTitleDto> titles = new LinkedList<>();

    @NotNull
    private List<IdentifierDescriptionDto> descriptions = new LinkedList<>();

    @NotNull
    private List<IdentifierFunderDto> funders = new LinkedList<>();

    @NotBlank
    @Schema(example = "SELECT `id`, `value`, `location` FROM `air_quality` WHERE `location` = \"09:STEF\"")
    private String query;

    @NotBlank
    @JsonProperty("query_normalized")
    @Schema(example = "SELECT `id`, `value`, `location` FROM `air_quality` WHERE `location` = \"09:STEF\"")
    private String queryNormalized;

    @JsonProperty("related_identifiers")
    private List<RelatedIdentifierDto> relatedIdentifiers = new LinkedList<>();

    @NotBlank
    @JsonProperty("query_hash")
    @Schema(description = "query hash in sha512")
    private String queryHash;

    @Schema(example = "2021-03-12T15:26:21Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant execution;

    @JsonProperty("result_hash")
    @Schema(example = "34fe82cda2c53f13f8d90cfd7a3469e3a939ff311add50dce30d9136397bf8e5")
    private String resultHash;

    @JsonProperty("result_number")
    @Schema(example = "1")
    private Long resultNumber;

    @Schema(example = "10.1038/nphys1170")
    private String doi;

    @NotBlank
    @Schema(example = "TU Wien")
    private String publisher;

    @NotNull
    private UserBriefDto owner;

    @JsonProperty("publication_day")
    @Schema(example = "15")
    private Integer publicationDay;

    @JsonProperty("publication_month")
    @Schema(example = "12")
    private Integer publicationMonth;

    @NotNull
    @JsonProperty("publication_year")
    @Schema(example = "2022")
    private Integer publicationYear;

    @NotNull
    private LanguageTypeDto language;

    @NotNull
    private List<LicenseDto> licenses = new LinkedList<>();

    @NotNull
    private List<CreatorDto> creators = new LinkedList<>();

    @NotNull
    @Schema(example = "draft")
    private IdentifierStatusTypeDto status;

}
