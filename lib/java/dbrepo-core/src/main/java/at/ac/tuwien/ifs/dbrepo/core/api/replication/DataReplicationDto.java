package at.ac.tuwien.ifs.dbrepo.core.api.replication;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleWithTimestampsDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

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

    @Schema(description = "Stable source event identity, retained across retries")
    private UUID eventId;

    @Schema(description = "Source database journal sequence; new events follow committed transaction order. "
            + "Migrated legacy sequences do not establish historical completeness")
    private Long eventSequence;
}
