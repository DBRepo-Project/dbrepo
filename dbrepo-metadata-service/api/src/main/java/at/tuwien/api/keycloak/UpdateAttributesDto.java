package at.tuwien.api.keycloak;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class UpdateAttributesDto {

    @NotNull
    private UserAttributesDto attributes;

}
