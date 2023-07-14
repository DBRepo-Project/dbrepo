package at.tuwien.api.user.external.affiliation;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class ExternalAffiliationDto {

    @Schema(example = "Brown University")
    @JsonProperty("organization_name")
    private String organizationName;

    @Schema(example = "6752")
    @JsonProperty("ringggold_id")
    private Long ringgoldId;

    @Schema(example = "0000000419369094")
    @JsonProperty("isni_id")
    private Long isniId;

    @Schema(example = "10.13039/100006418")
    @JsonProperty("crossref_funder_id")
    private String crossrefFunderId;

}
