package at.tuwien.api.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
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
public class UserBriefDto {

    @NotNull
    private Long id;

    @NotNull
    @Schema(example = "user", description = "Only contains lowercase characters")
    private String username;

    @JsonIgnore
    @JsonProperty("titles_before")
    @Schema(example = "Prof.")
    private String titlesBefore;

    @JsonIgnore
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
