package at.tuwien.api.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
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
public class UserAttributesDto {

    @org.springframework.data.annotation.Transient
    @Schema(example = "light")
    private String theme;

    @Field(name = "orcid", type = FieldType.Keyword)
    @Schema(example = "https://orcid.org/0000-0002-1825-0097")
    private String orcid;

    @Field(name = "affiliation", type = FieldType.Keyword)
    @Schema(example = "Brown University")
    private String affiliation;

    @JsonIgnore
    @org.springframework.data.annotation.Transient
    @Schema(example = "*CC67043C7BCFF5EEA5566BD9B1F3C74FD9A5CF5D")
    private String mariadbPassword;

}
