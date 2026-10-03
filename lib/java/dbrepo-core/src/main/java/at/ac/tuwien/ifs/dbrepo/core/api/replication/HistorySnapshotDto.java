package at.ac.tuwien.ifs.dbrepo.core.api.replication;

import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Versioned, credential-free wire format for immutable table-history artifacts. */
public final class HistorySnapshotDto {
    private HistorySnapshotDto() { }

    /** Captured before requesting a snapshot; a stream witness may have an unknown (null) epoch. */
    public record Checkpoint(UUID epoch, long boundary, UUID eventId) { }
    public record Create(@NotNull UUID snapshotId, @NotNull UUID tableId, Checkpoint base) { }
    public record Column(String name, int jdbcType, String sqlType, int precision, int scale,
                         boolean nullable, boolean signed, String collation) { }
    // Text cells preserve JDBC numeric/temporal representations; binary SQL types use base64. NULL stays NULL.
    public record Row(String replicationKey, String rowStart, String rowEnd, boolean current, List<String> cells) { }
    public record Manifest(int format, UUID snapshotId, String origin, UUID sourceDatabaseId, UUID sourceTableId,
                           UUID epoch, long boundary, UUID boundaryEventId, long legacyThrough, Checkpoint base,
                           List<Column> columns, int chunkRows, int chunkBytes, long chunks, long rows,
                           long currentKeys, String historyDigest, String currentKeysDigest) { }
    public record Envelope(Manifest manifest, String sha256) { }
    public record Import(@NotNull UUID targetTableId, @NotNull @Valid Envelope envelope) { }
    public record Chunk(UUID snapshotId, long index, String sha256, byte[] payload) { }
    public record Receipt(UUID snapshotId, UUID tableId, String status, long boundary, long legacyThrough,
                          String manifestDigest, boolean historyVerified, boolean currentReconciled) { }
    public record CurrentKey(String replicationKey, long chunkIndex, int rowIndex) { }
}
