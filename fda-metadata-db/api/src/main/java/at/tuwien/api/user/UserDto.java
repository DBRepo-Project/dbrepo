package at.tuwien.api.user;

import at.tuwien.api.container.ContainerDto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.Parameter;
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

    @Parameter(name = "id")
    private Long id;

    @Parameter(name = "user authorities")
    private List<GrantedAuthorityDto> authorities;

    @NotNull
    @Parameter(name = "user name")
    private String username;

    @NotNull
    @Parameter(name = "titles before the first name")
    private String titlesBefore;

    @NotNull
    @Parameter(name = "titles after the last name")
    private String titlesAfter;

    @NotNull
    @Parameter(name = "first name")
    private String firstname;

    @NotNull
    @Parameter(name = "last name")
    private String lastname;

    @NotNull
    @Parameter(name = "list of containers")
    private List<ContainerDto> containers;

    @NotNull
    @Parameter(name = "list of databases")
    private List<ContainerDto> databases;

    @NotNull
    @Parameter(name = "list of identifiers")
    private List<ContainerDto> identifiers;

    @NotNull
    @ToString.Exclude
    @JsonIgnore
    @Parameter(name = "password hash")
    private String password;

    @NotNull
    @Parameter(name = "mail address")
    private String email;

    @NotNull
    @Parameter(name = "mail address verified")
    private Boolean emailVerified;

}
