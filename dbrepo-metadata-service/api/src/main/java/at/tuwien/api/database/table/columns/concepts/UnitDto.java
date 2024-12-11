package at.tuwien.api.database.table.columns.concepts;

import at.tuwien.api.database.table.columns.ColumnBriefDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.jackson.Jacksonized;
import org.springframework.data.annotation.Id;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class UnitDto {

    @NotNull
    private Long id;

    @NotBlank
    private String uri;

    private String name;

    private String description;

    @NotNull
    private List<ColumnBriefDto> columns;
}
