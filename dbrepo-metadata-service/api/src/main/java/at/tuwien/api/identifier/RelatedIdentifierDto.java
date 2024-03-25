package at.tuwien.api.identifier;

import at.tuwien.api.user.UserDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.validation.constraints.NotNull;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class RelatedIdentifierDto {

    @Id
    @NotNull
    @Field(name = "id", type = FieldType.Keyword)
    private Long id;

    @NotNull
    @Schema(example = "10.70124/dc4zh-9ce78")
    @Field(name = "value", type = FieldType.Keyword)
    private String value;

    @NotNull
    @Schema(example = "DOI")
    @Field(name = "type", type = FieldType.Keyword)
    private RelatedTypeDto type;

    @NotNull
    @Schema(example = "Cites")
    @Field(name = "relation", type = FieldType.Keyword)
    private RelationTypeDto relation;

    @ToString.Exclude
    @JsonIgnore
    @NotNull
    @org.springframework.data.annotation.Transient
    private UserDto creator;

}


