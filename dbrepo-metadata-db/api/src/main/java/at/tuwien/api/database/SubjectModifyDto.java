package at.tuwien.api.database;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubjectModifyDto {

    private Long id;

    @NotNull
    @Schema(example = "air")
    private String name;

}
