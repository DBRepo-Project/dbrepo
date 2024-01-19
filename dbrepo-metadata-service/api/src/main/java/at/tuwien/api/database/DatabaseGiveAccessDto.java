package at.tuwien.api.database;

import lombok.*;

import jakarta.validation.constraints.NotNull;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class DatabaseGiveAccessDto {

    @NotNull(message = "access type is required")
    private AccessTypeDto type;


}
