package at.tuwien.api.database.query;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class ExportDto {

    @NotBlank(message = "location is required")
    @Schema(example = "/tmp/file.csv")
    private String location;
}
