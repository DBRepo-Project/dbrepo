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
public class PersonOrOrganizationDto {
    
    @NotNull(message = "type is required")
    @Parameter(name = "type", description = "The type of name. Either personal or organizational.")
    private PersonOrOrgTypeDto type;

    @JsonProperty("given_name")
    @Parameter(name = "given name", description = "Given name(s).")
    private String givenName;

    @JsonProperty("family_name")
    @Parameter(name = "family name", description = "Family name.")
    private String familyName;

    @Parameter(name = "name", description = "The full name of the organisation. For a person, this field is generated from given_name and family_name")
    private String name;

    @Parameter(name = "identifers", description = "Person or organisation identifiers.")
    private List<IdentifierDto> identifiers;
}
