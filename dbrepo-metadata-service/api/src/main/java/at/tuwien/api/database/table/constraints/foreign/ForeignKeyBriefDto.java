package at.tuwien.api.database.table.constraints.foreign;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class ForeignKeyBriefDto {

    @NonNull
    @Schema(example = "8")
    private Long id;
}
