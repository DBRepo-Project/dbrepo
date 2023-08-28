package at.tuwien.api.ror;

import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class RorDto {

    private String id;

    private String name;

    private Integer established;
}
