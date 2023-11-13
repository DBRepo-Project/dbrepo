package at.tuwien.api.database;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.extern.jackson.Jacksonized;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class LoadFileDto {

    @NotBlank(message = "filepath is required")
    @Schema(example = "sample.csv")
    private String filepath;

}
