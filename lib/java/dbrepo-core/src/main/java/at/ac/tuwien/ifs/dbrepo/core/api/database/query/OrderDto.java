package at.ac.tuwien.ifs.dbrepo.core.api.database.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class OrderDto {

    @NotNull
    @JsonProperty("column_id")
    @Schema(example = "e891ba86-0258-41a6-a8d9-ff58bc10b618")
    private UUID columnId;

    @Schema(example = "asc")
    private OrderTypeDto direction;

}
