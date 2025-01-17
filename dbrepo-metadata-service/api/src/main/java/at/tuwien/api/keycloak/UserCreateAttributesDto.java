package at.tuwien.api.keycloak;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class UserCreateAttributesDto {

    @JsonProperty("CUSTOM_ID")
    private String ldapId;

}
