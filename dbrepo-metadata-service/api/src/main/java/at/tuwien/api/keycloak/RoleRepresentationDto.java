package at.tuwien.api.keycloak;

import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

/**
 * https://www.keycloak.org/docs-api/22.0.1/rest-api/index.html#RoleRepresentation
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class RoleRepresentationDto {

    private UUID id;

    private String name;

    private String description;

    private Boolean scopeParamRequired;

    private Boolean composite;

    private Boolean clientRole;

    private UUID containerId;

}
