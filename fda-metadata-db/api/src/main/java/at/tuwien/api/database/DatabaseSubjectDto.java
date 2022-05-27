package at.tuwien.api.database;

import at.tuwien.api.container.ContainerDto;
import at.tuwien.api.container.image.ImageDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.user.UserDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;
import org.apache.catalina.User;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DatabaseSubjectDto {

    @NotNull
    @Parameter(name = "subject id")
    private Long id;

    @NotNull
    @Parameter(name = "subject database id")
    private Long dbid;

    @NotNull
    @ToString.Exclude
    @Parameter(name = "subject creator")
    private UserDto creator;

    @NotNull
    @Parameter(name = "subject name")
    private String name;

    @NotNull
    @ToString.Exclude
    @Parameter(name = "subject database")
    private DatabaseDto database;

    @NotNull
    @Parameter(name = "subject created")
    private Instant created;

}
