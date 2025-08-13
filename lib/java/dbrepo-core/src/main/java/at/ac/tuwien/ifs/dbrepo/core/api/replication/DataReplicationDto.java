package at.ac.tuwien.ifs.dbrepo.core.api.replication;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class DataReplicationDto {

    @Schema(description = "Tuple including versioning timestamps", example = "{ 'id': 1, 'inserted_at': '2025-01-01 10:00:00.000000', 'deleted_at': null }")
    private Map<String, Object> tuple;

    private DatabaseDto database;

    private TableDto table;
}


