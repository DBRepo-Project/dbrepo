package at.tuwien.api.doi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
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
public class AuthorDto {

    @Schema(example = "Weise")
    private String family;

    @Schema(example = "Martin")
    private String given;

    @JsonProperty("ORCID")
    @Schema(example = "http://orcid.org/0000-0003-4216-302X")
    private String orcid;

    @Schema(example = "first")
    private String sequence;

    private List<AffiliationDto> affiliation;

}
