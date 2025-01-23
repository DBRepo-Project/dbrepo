
package at.tuwien.api.database;

import at.tuwien.api.user.UserBriefDto;
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
    private UUID huserid;

    @NotNull
    @JsonIgnore
    private Long hdbid;

    @NotNull
    private UserBriefDto user;

    @NotNull
    @Schema(example = "read")
    private AccessTypeDto type;

}
