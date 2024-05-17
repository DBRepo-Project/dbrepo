package at.tuwien.api.database.table.columns.concepts;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class ConceptSaveDto {

    @NotBlank
    private String uri;

    @NotBlank
    private String name;

    @NotBlank
    private String description;

}
