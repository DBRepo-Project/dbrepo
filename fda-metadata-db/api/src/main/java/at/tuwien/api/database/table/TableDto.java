package at.tuwien.api.database.table;

import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.api.user.UserDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.Instant;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TableDto {

    @NotNull
    @Parameter(name = "table id", example = "1")
    private Long id;

    @NotBlank
    @Parameter(name = "table name", example = "Weather Australia")
    private String name;

    @NotBlank
    @JsonProperty("internal_name")
    @Parameter(name = "table internal name", example = "weather_australia")
    private String internalName;

    @NotBlank
    @Parameter(name = "topic name", example = "fda.c1.d1.t1")
    private String topic;

    @NotBlank
    @Parameter(name = "table description", example = "Predict next-day rain in Australia")
    private String description;

    @Parameter(name = "table creation time")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "Europe/Vienna")
    private Instant created;

    @NotNull
    @Parameter(name = "table columns")
    private ColumnDto[] columns;

}
