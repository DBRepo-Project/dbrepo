package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.HistorySnapshotDto.*;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.ReplicationJournalDto;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;
import java.util.Objects;
import java.util.function.Consumer;

/** A retry always reuses the saved snapshot ID and the receiver's durable staging area. */
final class HistorySnapshotTransfer {
    private final RestTemplate source;
    private final RestTemplate target;

    HistorySnapshotTransfer(RestTemplate source, RestTemplate target) {
        this.source = source;
        this.target = target;
    }

    void transfer(UUID snapshotId, UUID sourceDatabase, UUID sourceTable, String targetSite,
                  UUID targetDatabase, UUID targetTable, int pageSize, Checkpoint base, Runnable requireActive,
                  Consumer<ReplicationJournalDto.Event> deliver) {
        final String sourcePath = "/api/v1/database/" + sourceDatabase + "/replication/snapshots";
        final String targetPath = targetSite + "/api/v1/database/" + targetDatabase + "/replication/snapshots";
        requireActive.run();
        final Envelope envelope = sourceManifest(sourcePath, snapshotId, sourceTable, base);
        final Manifest manifest = envelope.manifest();
        if (!snapshotId.equals(manifest.snapshotId()) || !sourceDatabase.equals(manifest.sourceDatabaseId())
                || !sourceTable.equals(manifest.sourceTableId()) || !Objects.equals(base, manifest.base())
                || manifest.chunks() < 0 || manifest.boundary() < 0) {
            throw new IllegalStateException("Source returned a conflicting snapshot identity");
        }
        requireActive.run();
        request(target, targetPath + "/imports", HttpMethod.POST, new Import(targetTable, envelope), Receipt.class);
        for (long index = 0; index < manifest.chunks(); index++) {
            requireActive.run();
            final Chunk chunk = request(source, sourcePath + "/" + snapshotId + "/chunks/" + index,
                    HttpMethod.GET, null, Chunk.class);
            if (!snapshotId.equals(chunk.snapshotId()) || chunk.index() != index) {
                throw new IllegalStateException("Source returned a conflicting snapshot chunk");
            }
            requireActive.run();
            request(target, targetPath + "/" + snapshotId + "/chunks/" + index,
                    HttpMethod.PUT, chunk, Receipt.class);
        }
        requireActive.run();
        final Receipt receipt = request(target, targetPath + "/" + snapshotId + "/reconcile",
                HttpMethod.POST, Map.of("tableId", targetTable), Receipt.class);
        if (!snapshotId.equals(receipt.snapshotId()) || !targetTable.equals(receipt.tableId())
                || !receipt.historyVerified() || !receipt.currentReconciled()
                || receipt.boundary() != manifest.boundary() || !envelope.sha256().equals(receipt.manifestDigest())) {
            throw new IllegalStateException("Replica has not verified and reconciled the complete source snapshot");
        }
        catchUp(sourceDatabase, sourceTable, manifest.boundary(), pageSize, requireActive, deliver);
    }

    Checkpoint checkpoint(String targetSite, UUID database, UUID table) {
        final ResponseEntity<Checkpoint> response = target.exchange(targetSite + "/api/v1/database/" + database
                + "/replication/snapshots/table/" + table + "/checkpoint-with-inbox", HttpMethod.GET, HttpEntity.EMPTY, Checkpoint.class);
        requireSuccess(response);
        return response.getBody();
    }

    private Envelope sourceManifest(String sourcePath, UUID snapshotId, UUID sourceTable, Checkpoint base) {
        try {
            return request(source, sourcePath + "/" + snapshotId, HttpMethod.GET, null, Envelope.class);
        } catch (HttpClientErrorException.NotFound | HttpServerErrorException.ServiceUnavailable absentOrUnfinished) {
            // The saved checkpoint is reused after a timeout or an interrupted unpublished export.
            return request(source, sourcePath, HttpMethod.POST,
                    new Create(snapshotId, sourceTable, base), Envelope.class);
        }
    }

    private void catchUp(UUID databaseId, UUID tableId, long boundary, int pageSize, Runnable requireActive,
                         Consumer<ReplicationJournalDto.Event> deliver) {
        long after = boundary;
        Long through = null;
        do {
            requireActive.run();
            final String path = "/api/v1/database/" + databaseId + "/replication/journal?after=" + after
                    + "&limit=" + pageSize + (through == null ? "" : "&through=" + through);
            final ReplicationJournalDto page = request(source, path, HttpMethod.GET, null, ReplicationJournalDto.class);
            if (page.through() < after || through != null && page.through() != through || page.events() == null) {
                throw new IllegalStateException("Source journal boundary changed during catch-up");
            }
            through = page.through();
            long cursor = after;
            for (var event : page.events()) {
                final var payload = event.payload();
                if (payload == null || payload.getEventId() == null || payload.getEventSequence() == null
                        || payload.getEventSequence() != cursor + 1 || payload.getDatabase() == null
                        || !databaseId.equals(payload.getDatabase().getId()) || payload.getTable() == null
                        || payload.getTable().getId() == null || payload.getEventSequence() > through) {
                    throw new IllegalStateException("Source journal contains a gap or conflicting event identity");
                }
                cursor = payload.getEventSequence();
                if (tableId.equals(payload.getTable().getId())) {
                    requireActive.run();
                    deliver.accept(event);
                }
            }
            if (page.nextAfter() != cursor || cursor == after && cursor != through) {
                throw new IllegalStateException("Source journal did not prove progress to the captured boundary");
            }
            after = cursor;
        } while (after < through);
    }

    private <T> T request(RestTemplate client, String path, HttpMethod method, Object body, Class<T> type) {
        final ResponseEntity<T> response = client.exchange(path, method, body == null ? HttpEntity.EMPTY : new HttpEntity<>(body), type);
        requireSuccess(response);
        if (response.getBody() == null) {
            throw new IllegalStateException("Snapshot request returned no response body");
        }
        return response.getBody();
    }

    private void requireSuccess(ResponseEntity<?> response) {
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RestClientResponseException("Snapshot request failed", response.getStatusCode().value(),
                    response.getStatusCode().toString(), response.getHeaders(), null, null);
        }
    }
}
