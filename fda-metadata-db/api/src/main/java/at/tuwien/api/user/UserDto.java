package at.tuwien.api.user;

import at.tuwien.api.container.ContainerDto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {

    @NotNull
    private Long id;

    @ToString.Exclude
    private List<GrantedAuthorityDto> authorities;

    @NotNull
    @Schema(example = "jcarberry", description = "Only contains lowercase characters")
    private String username;

    @JsonProperty("titles_before")
    @Schema(example = "Prof.")
    private String titlesBefore;

    @JsonProperty("titles_after")
    private String titlesAfter;

    @Schema(example = "Josiah")
    private String firstname;

    @Schema(example = "Carberry")
    private String lastname;

    @Schema(example = "Brown University")
    private String affiliation;

    @Schema(example = "0000-0002-1825-0097")
    private String orcid;

    @NotNull
    @JsonProperty("theme_dark")
    @Schema(example = "true")
    private Boolean themeDark;

    private List<ContainerDto> containers;

    private List<ContainerDto> databases;

    private List<ContainerDto> identifiers;

    @ToString.Exclude
    @JsonIgnore
    private String password;

    @NotNull
    @Schema(example = "jcarberry@brown.edu")
    private String email;

    @NotNull
    @JsonProperty("email_verified")
    @Schema(example = "true")
    private Boolean emailVerified;

}
