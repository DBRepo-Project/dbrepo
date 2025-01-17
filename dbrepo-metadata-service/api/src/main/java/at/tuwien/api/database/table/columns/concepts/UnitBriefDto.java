package at.tuwien.api.database.table.columns.concepts;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class UnitBriefDto {

    @NotNull
    private Long id;

    @NotBlank
    private String uri;

    private String name;

    private String description;
}
