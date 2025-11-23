package at.ac.tuwien.ifs.dbrepo.api;

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
public class SubsetMetadata {

    @NotNull
    private Long resultCount;

    @NotBlank
    private String resultHash;

}
