package at.ac.tuwien.ifs.dbrepo.core.api.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class UserBriefDto {

    @Schema(example = "1ffc7b0e-9aeb-4e8b-b8f1-68f3936155b4")
    private UUID id;

    @NotNull
    @Schema(example = "jcarberry", description = "Only contains lowercase characters")
    private String username;

    @Schema(example = "Josiah Carberry")
    private String name;

    @JsonProperty("qualified_name")
    @Schema(example = "Josiah Carberry — @jcarberry")
    private String qualifiedName;

    @Schema(example = "0000-0002-1825-0097")
    private String orcid;

    @JsonProperty("given_name")
    @Schema(example = "Josiah")
    private String firstname;

    @JsonProperty("family_name")
    @Schema(example = "Carberry")
    private String lastname;

}
