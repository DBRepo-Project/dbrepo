package at.ac.tuwien.ifs.dbrepo.core.api.database.table.constraints;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConstraintDeleteDto {

    @NotBlank
    private String type;

    private UUID id;

    private String name;

    private String column;

    private String check;

}
