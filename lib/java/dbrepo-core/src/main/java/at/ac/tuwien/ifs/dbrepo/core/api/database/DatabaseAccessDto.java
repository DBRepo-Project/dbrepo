
package at.ac.tuwien.ifs.dbrepo.core.api.database;

import at.ac.tuwien.ifs.dbrepo.core.api.user.UserBriefDto;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
public class DatabaseAccessDto {

    @NotNull
    @JsonIgnore
    @Schema(description = "The user id", example = "1ffc7b0e-9aeb-4e8b-b8f1-68f3936155b4")
    private UUID huserid;

    @NotNull
    @JsonIgnore
    @Schema(description = "The database id", example = "fc29f89c-86a8-4020-9e36-4d954736c6cc")
    private UUID hdbid;

    @NotNull
    private UserBriefDto user;

    @NotNull
    @Schema(example = "read")
    private AccessTypeDto type;

}
