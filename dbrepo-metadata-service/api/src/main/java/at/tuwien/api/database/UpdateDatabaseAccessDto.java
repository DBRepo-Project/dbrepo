package at.tuwien.api.database;

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
public class UpdateDatabaseAccessDto {

    @NotNull
    private AccessTypeDto type;


}
