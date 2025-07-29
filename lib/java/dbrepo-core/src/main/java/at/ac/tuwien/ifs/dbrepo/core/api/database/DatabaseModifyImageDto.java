package at.ac.tuwien.ifs.dbrepo.core.api.database;

import io.swagger.v3.oas.annotations.media.Schema;
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
public class DatabaseModifyImageDto {

    @Schema(description = "The key of the S3 binary object in the storage service", example = "file.csv")
    private String key;

}
