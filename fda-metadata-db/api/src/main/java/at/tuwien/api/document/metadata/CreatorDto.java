package at.tuwien.api.document.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreatorDto {

    @JsonProperty("person_or_org")
    @NotNull(message = "person or organization is required")
    @Parameter(name = "person or organization", description = "The person or organization.")
    private PersonOrOrganizationDto personOrOrganization;

    @Parameter(name = "role", description = "The role of the person or organisation selected from a customizable " +
            "controlled vocabulary.")
    private String role;

    @Parameter(name = "affiliations", description = "Affilations if person_or_org.type is personal.")
    private List<AffiliationDto> affiliations;
}
