package at.tuwien.api.database.table;

import lombok.*;

import jakarta.validation.constraints.NotNull;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class TupleUpdateDto {

    @NotNull
    private Map<String, Object> data;

    @NotNull
    private Map<String, Object> keys;

}