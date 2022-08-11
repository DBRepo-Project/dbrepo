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
public class IdentifierDto {

    @NotNull(message = "scheme is required")
    @Parameter(name = "scheme", description = "The identifier scheme.")
    private IdentifierTypeDto scheme;

    @NotNull(message = "identifier is required")
    @Parameter(name = "identifier", description = "Actual value of the identifier.")
    private String identifier;
}
