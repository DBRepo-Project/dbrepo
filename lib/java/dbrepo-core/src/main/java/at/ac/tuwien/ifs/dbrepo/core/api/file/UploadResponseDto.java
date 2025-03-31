package at.ac.tuwien.ifs.dbrepo.core.api.file;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@ToString
@Builder
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class UploadResponseDto {

    @NotBlank
    @JsonProperty("s3_key")
    String s3Key;
}
