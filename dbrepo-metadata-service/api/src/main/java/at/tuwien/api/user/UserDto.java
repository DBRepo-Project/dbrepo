package at.tuwien.api.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.validation.constraints.NotNull;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserDto {

    @Id
    @NotNull
    @EqualsAndHashCode.Include
    @Schema(example = "1ffc7b0e-9aeb-4e8b-b8f1-68f3936155b4")
    @Field(name = "id", type = FieldType.Keyword)
    private UUID id;

    @NotNull
    @Schema(example = "jcarberry", description = "Only contains lowercase characters")
    @Field(name = "username", type = FieldType.Keyword)
    private String username;

    @Schema(example = "Josiah Carberry")
    @Field(name = "name", type = FieldType.Keyword)
    private String name;

    @JsonProperty("qualified_name")
    @Schema(example = "Josiah Carberry — @jcarberry")
    @Field(name = "qualified_name", type = FieldType.Keyword)
    private String qualifiedName;

    @JsonProperty("given_name")
    @Schema(example = "Josiah")
    @Field(name = "firstname", type = FieldType.Keyword)
    private String firstname;

    @JsonProperty("family_name")
    @Schema(example = "Carberry")
    @Field(name = "lastname", type = FieldType.Keyword)
    private String lastname;

    @NotNull
    private UserAttributesDto attributes;

}
