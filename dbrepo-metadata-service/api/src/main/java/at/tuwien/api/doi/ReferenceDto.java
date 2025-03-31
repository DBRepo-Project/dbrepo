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
public class ReferenceDto {

    @NotNull
    @Schema(example = "ref1")
    private String key;

    @JsonProperty("doi-asserted-by")
    @Schema(example = "publisher")
    private String doiAssertedBy;

    @JsonProperty("DOI")
    @Schema(example = "10.1038/sdata.2016.18")
    private String doi;

    @Schema(example = "2024")
    private String year;

    @JsonProperty("article-title")
    @Schema(example = "The Dryad Data Repository: a Singapore Framework metadata Architecture in a DSpace Environment")
    private String articleTitle;

    @JsonProperty("volume-title")
    @Schema(example = "Proceedings of the 2008 International Conference on Dublin Core and Metadata Applications")
    private String volumeTitle;

    @JsonProperty("journal-title")
    @Schema(example = "Libraries Research Publications")
    private String journalTitle;

    @Schema(example = "Witt")
    private String author;

    @JsonProperty("first-page")
    @Schema(example = "157")
    private String firstPage;

}
