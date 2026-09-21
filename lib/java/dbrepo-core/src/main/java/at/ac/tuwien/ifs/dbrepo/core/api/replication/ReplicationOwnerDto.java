package at.ac.tuwien.ifs.dbrepo.core.api.replication;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class ReplicationOwnerDto {

    @NotBlank
    @JsonProperty("site_url")
    private String siteUrl;

    @NotBlank
    private String issuer;

    @NotBlank
    private String subject;

    @NotBlank
    private String username;

}
