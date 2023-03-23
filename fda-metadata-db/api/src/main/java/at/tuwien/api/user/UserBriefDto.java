package at.tuwien.api.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.security.core.userdetails.UserDetails;

import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserBriefDto {

    @NotNull
    @JsonProperty("sub")
    private String id;

    @ToString.Exclude
    @org.springframework.data.annotation.Transient
    private List<GrantedAuthorityDto> authorities;

    @NotNull
    @JsonProperty("preferred_username")
    @Schema(example = "jcarberry", description = "Only contains lowercase characters")
    private String username;

    @Schema(example = "Josiah Carberry")
    private String name;

    @JsonProperty("titles_before")
    @Schema(example = "Prof.")
    private String titlesBefore;

    @JsonProperty("titles_after")
    private String titlesAfter;

    @JsonProperty("given_name")
    @Schema(example = "Josiah")
    private String firstname;

    @JsonProperty("family_name")
    @Schema(example = "Carberry")
    private String lastname;

    @Schema(example = "Brown University")
    private String affiliation;

    @Schema(example = "0000-0002-1825-0097")
    private String orcid;

    @JsonIgnore
    @JsonProperty("theme_dark")
    @Schema(example = "true")
    @org.springframework.data.annotation.Transient
    private Boolean themeDark;

    @JsonIgnore
    @JsonProperty("email_verified")
    @Schema(example = "true")
    @org.springframework.data.annotation.Transient
    private Boolean emailVerified;

}
