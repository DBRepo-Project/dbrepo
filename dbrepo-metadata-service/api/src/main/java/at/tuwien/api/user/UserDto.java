package at.tuwien.api.user;

import at.tuwien.api.container.ContainerDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.validation.constraints.NotNull;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
@Document(indexName = "user")
public class UserDto {

    @Id
    @NotNull
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

    @Schema(example = "http://orcid.org/0000-0002-1825-0097")
    @Field(name = "orcid", type = FieldType.Keyword)
    private String orcid;

    @JsonProperty("given_name")
    @Schema(example = "Josiah")
    @Field(name = "firstname", type = FieldType.Keyword)
    private String firstname;

    @JsonProperty("family_name")
    @Schema(example = "Carberry")
    @Field(name = "lastname", type = FieldType.Keyword)
    private String lastname;

    @NotNull
    @org.springframework.data.annotation.Transient
    private List<UserAttributeDto> attributes;

    @NotNull
    @org.springframework.data.annotation.Transient
    private List<ContainerDto> containers;

    @NotNull
    @org.springframework.data.annotation.Transient
    private List<ContainerDto> databases;

    @NotNull
    @org.springframework.data.annotation.Transient
    private List<ContainerDto> identifiers;

    @NotNull
    @org.springframework.data.annotation.Transient
    @Schema(example = "jcarberry@brown.edu")
    private String email;

    @NotNull
    @JsonProperty("email_verified")
    @org.springframework.data.annotation.Transient
    @Schema(example = "true")
    private Boolean emailVerified;

}
