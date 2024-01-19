package at.tuwien.api.database;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class LicenseDto {

    @NotNull
    @Schema(example = "MIT")
    @Field(name = "identifier", type = FieldType.Keyword)
    private String identifier;

    @NotBlank
    @Schema(example = "https://opensource.org/licenses/MIT")
    @Field(name = "uri", type = FieldType.Keyword)
    private String uri;

}