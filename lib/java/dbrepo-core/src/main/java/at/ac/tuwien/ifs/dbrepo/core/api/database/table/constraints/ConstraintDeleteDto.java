package at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints;

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
public class ConstraintDeleteDto {

    @NotBlank
    private String type;

    private String name;

    private String column;

    private String check;

}
