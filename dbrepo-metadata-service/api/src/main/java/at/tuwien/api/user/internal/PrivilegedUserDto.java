package at.tuwien.api.user.internal;

import at.tuwien.api.PrivilegedObjectDto;
import at.tuwien.api.user.UserAttributesDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class PrivilegedUserDto extends PrivilegedObjectDto {

    @NotNull
    @Schema(example = "1ffc7b0e-9aeb-4e8b-b8f1-68f3936155b4")
    private UUID id;

    @NotBlank
    @Schema(example = "jcarberry", description = "Only contains lowercase characters")
    private String username;

    @NotBlank
    @Schema(example = "jcarberry")
    private String password;

    @Schema(example = "Josiah Carberry")
    private String name;

    @JsonProperty("qualified_name")
    @Schema(example = "Josiah Carberry — @jcarberry")
    private String qualifiedName;

    @JsonProperty("given_name")
    @Schema(example = "Josiah")
    private String firstname;

    @JsonProperty("family_name")
    @Schema(example = "Carberry")
    private String lastname;

    @NotNull
    private UserAttributesDto attributes;

    @JsonProperty("last_retrieved")
    private Instant lastRetrieved;

}
