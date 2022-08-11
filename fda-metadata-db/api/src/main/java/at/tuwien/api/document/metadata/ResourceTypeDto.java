package at.tuwien.api.document.metadata;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResourceTypeDto {

    /**
     * When interfacing with Datacite, this field is converted to a format compatible with 10. Resource Type (i.e.
     * type and subtype). DataCite allows free text for the subtype, however InvenioRDM requires this to come from a
     * customizable controlled vocabulary.
     */
    @NotNull(message = "id is required")
    @Parameter(name = "id", description = "The resource type id from the controlled vocabulary.")
    private String id;
}
