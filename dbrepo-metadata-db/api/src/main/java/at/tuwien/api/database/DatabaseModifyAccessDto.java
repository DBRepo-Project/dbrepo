package at.tuwien.api.database;

import lombok.*;

import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DatabaseModifyAccessDto {

    @NotNull(message = "access type is required")
    private AccessTypeDto type;

}
