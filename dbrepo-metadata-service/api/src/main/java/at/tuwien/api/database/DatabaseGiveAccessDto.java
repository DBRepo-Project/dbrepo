package at.tuwien.api.database;

import lombok.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class DatabaseGiveAccessDto {

    @NotBlank(message = "user id is required")
    private UUID userId;

    @NotNull(message = "access type is required")
    private AccessTypeDto type;


}
