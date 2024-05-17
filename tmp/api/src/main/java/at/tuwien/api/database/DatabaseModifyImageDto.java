package at.tuwien.api.database;

import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class DatabaseModifyImageDto {

    private String key;

}
