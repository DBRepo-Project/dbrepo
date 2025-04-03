package at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.concepts;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@EqualsAndHashCode
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
