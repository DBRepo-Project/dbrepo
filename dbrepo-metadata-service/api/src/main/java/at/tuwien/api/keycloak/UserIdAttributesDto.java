package at.tuwien.api.keycloak;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class UserIdAttributesDto {

    @Schema(example = "s3cr3t")
    @JsonProperty("LDAP_ENTRY_DN")
    private String[] ldapEntryDn;

    @Schema(example = "false")
    @JsonProperty("LDAP_ID")
    private UUID[] ldapId;

}
