
package at.tuwien.api.database;

import at.tuwien.api.user.UserDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.validation.constraints.NotNull;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
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

    @NotNull
    @Schema(example = "2021-03-12T15:26:21Z")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;

}
