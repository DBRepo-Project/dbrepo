package at.ac.tuwien.ifs.dbrepo.core.api.database.table;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.TupleReplicationTimestampDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ReplicationSynchronisationDataDto {

    @Schema(description = "List of tuples with their data and timestamps")
    private List<TupleWithTimestampsDto> tuples;

    @Schema(description = "List of replication timestamps derived from tuple data")
    private List<TupleReplicationTimestampDto> replicationTimestamps;
}
