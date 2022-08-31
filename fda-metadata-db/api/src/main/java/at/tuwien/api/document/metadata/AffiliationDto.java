package at.tuwien.api.document.metadata;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;


@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AffiliationDto {

    @Parameter(name = "id", description = "The organizational or institutional id from the controlled vocabulary.")
    private String id;

    @Parameter(name = "name", description = "The name of the organisation or institution.")
    private String name;
}
