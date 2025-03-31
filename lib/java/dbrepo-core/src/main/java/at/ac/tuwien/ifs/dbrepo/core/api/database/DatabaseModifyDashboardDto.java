package at.ac.tuwien.ifs.dbrepo.core.api.database;

import jakarta.validation.constraints.NotNull;
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
public class DatabaseModifyDashboardDto {

    @NotNull
    private String uid;

}
