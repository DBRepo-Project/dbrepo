package at.tuwien.api.doi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class DoiDto {

    @NotNull
    @Schema(example = "https://doi.org/10.5334/dsj-2022-004")
    private String id;

    @NotNull
    private TimeRepresentationDto indexed;

    private TimeRepresentationDto deposited;

    private TimeRepresentationDto issued;

    private TimeRepresentationDto published;

    @JsonProperty("DOI")
    @Schema(example = "10.5334/dsj-2022-004")
    private String doi;

    @NotNull
    @Schema(example = "dataset")
    private String type;

    private List<AuthorDto> author;

    @Schema(example = "Crossref")
    private String source;

    @Schema(example = "DBRepo: A Data Repository System for Research Data in Databases")
    private String title;

    @Schema(example = "10.1109")
    private String prefix;

    @Schema(example = "21")
    private String volume;

    @JsonProperty("is-referenced-by-count")
    @Schema(example = "0")
    private Integer isReferencedByCount;

    @JsonProperty("reference-count")
    @Schema(example = "28")
    private Integer referenceCount;

    @Schema(example = "IEEE")
    private String publisher;

    @Schema(example = "322-331")
    private String page;

    private String member;

    @Schema(example = "2024 IEEE International Conference on Big Data (BigData)")
    private String event;

    private List<ReferenceDto> reference;

    private Integer score;

    @JsonProperty("URL")
    private String url;

}
