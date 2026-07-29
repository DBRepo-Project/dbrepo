package at.ac.tuwien.ifs.dbrepo.core.api.replication;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class TupleReplicationTimestampDto {

    @Schema(description = "URL of the site where the tuple originated")
    private String siteUrl;

    @Schema(description = "Unique identifier for the replication operation")
    private String replicationId;

    @Schema(description = "ID of the database containing the table")
    private UUID databaseId;

    @Schema(description = "ID of the table containing the tuple")
    private UUID tableId;

    @Schema(description = "Timestamp when the row/tuple replication started")
    private Instant rowStart;

    @Schema(description = "Timestamp when the row/tuple replication ended (null if still active)")
    private Instant rowEnd;
}
