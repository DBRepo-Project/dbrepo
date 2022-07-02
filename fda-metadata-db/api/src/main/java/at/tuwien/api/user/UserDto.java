package at.tuwien.api.user;

import at.tuwien.api.container.ContainerDto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {

    @Parameter(name = "id")
    private Long id;

    @ToString.Exclude
    @Parameter(name = "user authorities")
    private List<GrantedAuthorityDto> authorities;

    @NotNull
    @Parameter(name = "user name")
    private String username;

    @JsonProperty("titles_before")
    @Parameter(name = "titles before the first name")
    private String titlesBefore;

    @JsonProperty("titles_after")
    @Parameter(name = "titles after the last name")
    private String titlesAfter;

    @NotBlank
    @Parameter(name = "first name")
    private String firstname;

    @NotBlank
    @Parameter(name = "last name")
    private String lastname;

    @ToString.Exclude
    @Parameter(name = "list of containers")
    private List<ContainerDto> containers;

    @ToString.Exclude
    @Parameter(name = "list of databases")
    private List<ContainerDto> databases;

    @ToString.Exclude
    @Parameter(name = "list of identifiers")
    private List<ContainerDto> identifiers;

    @ToString.Exclude
    @JsonIgnore
    @Parameter(name = "password hash")
    private String password;

    @NotNull
    @Parameter(name = "mail address")
    private String email;

    @JsonProperty("email_verified")
    @Parameter(name = "mail address verified")
    private Boolean emailVerified;

}
