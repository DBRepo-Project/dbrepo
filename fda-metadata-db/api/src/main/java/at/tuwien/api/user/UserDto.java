package at.tuwien.api.user;

import at.tuwien.api.container.ContainerDto;
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
    @JsonProperty("sub")
    private String id;

    @NotNull
    @Schema(example = "jcarberry", description = "Only contains lowercase characters")
    private String username;

    @Schema(example = "Josiah Carberry")
    private String name;

    @JsonProperty("given_name")
    @Schema(example = "Josiah")
    private String firstname;

    @JsonProperty("family_name")
    @Schema(example = "Carberry")
    private String lastname;

    @EqualsAndHashCode.Exclude
    @org.springframework.data.annotation.Transient
    private List<ContainerDto> containers;

    @EqualsAndHashCode.Exclude
    @org.springframework.data.annotation.Transient
    private List<ContainerDto> databases;

    @EqualsAndHashCode.Exclude
    @org.springframework.data.annotation.Transient
    private List<ContainerDto> identifiers;

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
