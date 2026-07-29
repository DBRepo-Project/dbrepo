package at.ac.tuwien.ifs.dbrepo.core.api.replication;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleWithTimestampsDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class DataReplicationDto {

    @Schema(description = "Tuple including versioning timestamps")
    private TupleWithTimestampsDto tuple;

    private DatabaseDto database;

    private TableDto table;
}
