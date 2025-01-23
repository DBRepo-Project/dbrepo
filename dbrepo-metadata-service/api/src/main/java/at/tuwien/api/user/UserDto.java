package at.tuwien.api.user;

import at.tuwien.api.CacheableDto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class UserDto extends CacheableDto {

    @NotNull
    @Schema(example = "1ffc7b0e-9aeb-4e8b-b8f1-68f3936155b4")
    private UUID id;

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

    /* lombok limitations prevent from convenient builder functions */

    @JsonProperty("last_retrieved")
    @Schema(example = "2025-01-23T12:09:01")
    private Instant lastRetrieved;

    @ToString.Exclude
    @Schema(example = "mariadb")
    private String jdbcMethod;

    @ToString.Exclude
    @Schema(example = "data-db")
    private String host;

    @ToString.Exclude
    @Schema(example = "3306")
    private Integer port;

    @ToString.Exclude
    @Schema(example = "username")
    private String username;

    @ToString.Exclude
    @JsonIgnore
    private String password;

    @ToString.Exclude
    @Schema(example = "air_quality")
    private String database;

}
