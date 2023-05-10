
package at.tuwien.api.database;

import at.tuwien.api.user.UserDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DatabaseAccessDto {

    @NotNull
    @JsonIgnore
    @ToString.Exclude
    private UUID huserid;

    @NotNull
    @JsonIgnore
    @ToString.Exclude
    private Long hdbid;

    @NotNull
    private UserDto user;

    @NotNull
    private AccessTypeDto type;

    @Schema(example = "2020-08-04 11:12:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;

}
