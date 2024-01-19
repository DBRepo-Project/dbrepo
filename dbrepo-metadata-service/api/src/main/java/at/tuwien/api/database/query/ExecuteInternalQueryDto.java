package at.tuwien.api.database.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class ExecuteInternalQueryDto {

    @JsonProperty("container_id")
    private String containerId;

    private String query;

}
