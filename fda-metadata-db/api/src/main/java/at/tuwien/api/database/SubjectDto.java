package at.tuwien.api.database;

import at.tuwien.api.user.UserDto;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.NotNull;
import java.time.Instant;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubjectDto {

    @Parameter(name = "subject id")
    private Long id;

    @NotNull
    @ToString.Exclude
    @Parameter(name = "subject creator")
    private UserDto creator;

    @NotNull
    @Parameter(name = "subject name")
    private String name;

    @NotNull
    @ToString.Exclude
    @Parameter(name = "subject database")
    private DatabaseDto database;

    @NotNull
    @Parameter(name = "subject created")
    private Instant created;

    @Parameter(name = "subject last modified")
    private Instant lastModified;

}
