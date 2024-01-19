package at.tuwien.api.container.image;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
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
public class ImageBriefDto {

    @NotNull
    @Field(name = "id", type = FieldType.Keyword)
    private Long id;

    @NotBlank
    @Field(name = "name", type = FieldType.Keyword)
    @Schema(example = "mariadb")
    private String name;

    @NotBlank
    @Field(name = "version", type = FieldType.Keyword)
    @Schema(example = "10.5")
    private String version;

    @NotBlank
    @JsonProperty("jdbc_method")
    @Field(name = "jdbc_method")
    @Schema(example = "mariadb")
    private String jdbcMethod;

}
