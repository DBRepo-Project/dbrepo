package at.tuwien.api.database;

import lombok.*;

import jakarta.validation.constraints.NotBlank;
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

    @NotBlank(message = "username is required")
    private String username;

    @NotNull(message = "access type is required")
    private AccessTypeDto type;


}
