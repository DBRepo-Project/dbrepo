package at.tuwien.api.database;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubjectModifyDto {

    @Parameter(name = "subject id")
    private Long id;

    @NotNull
    @Parameter(name = "subject name")
    private String name;

}
