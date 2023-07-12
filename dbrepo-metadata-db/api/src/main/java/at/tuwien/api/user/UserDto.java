package at.tuwien.api.user;

import at.tuwien.api.container.ContainerDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Id;
import lombok.*;

import jakarta.validation.constraints.NotNull;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.elasticsearch.annotations.Document;

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
    private UUID id;

    @NotNull
    @Schema(example = "jcarberry", description = "Only contains lowercase characters")
    private String username;

    @Schema(example = "Josiah Carberry")
    private String name;

    @Schema(example = "0000-0002-1825-0097")
    private String orcid;

    @JsonProperty("given_name")
    @Schema(example = "Josiah")
    private String firstname;

    @JsonProperty("family_name")
    @Schema(example = "Carberry")
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
