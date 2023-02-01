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
@EqualsAndHashCode
public class UserDto {

    @NotNull
    private Long id;

    @ToString.Exclude
    @org.springframework.data.annotation.Transient
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
    @Schema(description = "Roles of the user", example = "[ROLE_RESEARCHER]")
    @org.springframework.data.annotation.Transient
    private List<String> roles;

    @NotNull
    @JsonProperty("theme_dark")
    @Schema(example = "true")
    @org.springframework.data.annotation.Transient
    private Boolean themeDark;

    @EqualsAndHashCode.Exclude
    @org.springframework.data.annotation.Transient
    private List<ContainerDto> containers;

    @EqualsAndHashCode.Exclude
    @org.springframework.data.annotation.Transient
    private List<ContainerDto> databases;

    @EqualsAndHashCode.Exclude
    @org.springframework.data.annotation.Transient
    private List<ContainerDto> identifiers;

    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @org.springframework.data.annotation.Transient
    private String password;

    @NotNull
    @Schema(example = "jcarberry@brown.edu")
    @org.springframework.data.annotation.Transient
    private String email;

    @NotNull
    @JsonProperty("email_verified")
    @Schema(example = "true")
    @org.springframework.data.annotation.Transient
    private Boolean emailVerified;

}
