package at.ac.tuwien.ifs.dbrepo.core.api.file;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(description = "The key of the S3 binary object in the storage service", example = "file.csv")
    private String s3Key;
}
