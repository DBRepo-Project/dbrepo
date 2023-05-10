package at.tuwien.api.database;

import lombok.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DatabaseGiveAccessDto {

    @NotBlank(message = "username is required")
    private String username;

    @NotNull(message = "access type is required")
    private AccessTypeDto type;


}
