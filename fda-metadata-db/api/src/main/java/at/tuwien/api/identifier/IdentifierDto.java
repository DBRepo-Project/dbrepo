package at.tuwien.api.identifier;

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
    private Long cid;

    @NotNull
    private Long dbid;

    @NotNull
    private Long qid;

    @NotBlank
    @Schema(example = "Airquality Stephansplatz, Vienna, Austria")
    private String title;

    @NotBlank
    @Schema(example = "Air quality reports at Stephansplatz, Vienna")
    private String description;

    @NotBlank
    @Schema(example = "SELECT `id`, `value`, `location` FROM `air_quality` WHERE `location` = \"09:STEF\"")
    private String query;

    @NotBlank
    @JsonProperty("query_normalized")
    @Schema(example = "SELECT `id`, `value`, `location` FROM `air_quality` WHERE `location` = \"09:STEF\"")
    private String queryNormalized;

    @JsonProperty("related")
    private List<RelatedIdentifierDto> related;

    @NotBlank
    @JsonProperty("query_hash")
    @Schema(description = "query hash in sha512")
    private String queryHash;

    @NotNull
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant execution;

    @NotBlank
    @JsonProperty("result_hash")
    private String resultHash;

    @NotNull
    @JsonProperty("result_number")
    @Schema(example = "1")
    private Long resultNumber;

    @NotNull
    @Schema(example = "everyone")
    private VisibilityTypeDto visibility;

    @Schema(example = "10.1038/nphys1170")
    private String doi;

    @Schema(example = "TU Wien")
    private String publisher;

    @NotNull
    private UserDto creator;

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
    private List<CreatorDto> creators;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;

    @JsonProperty("last_modified")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant lastModified;

}
