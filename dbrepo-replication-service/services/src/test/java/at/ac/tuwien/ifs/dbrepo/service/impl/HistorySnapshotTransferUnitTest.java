package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.DataReplicationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.HistorySnapshotDto.*;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.ReplicationJournalDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HistorySnapshotTransferUnitTest {
    private final RestTemplate source = mock(RestTemplate.class);
    private final RestTemplate target = mock(RestTemplate.class);
    private final HistorySnapshotTransfer transfer = new HistorySnapshotTransfer(source, target);
    private final UUID database = UUID.randomUUID(), table = UUID.randomUUID(), targetDatabase = UUID.randomUUID();
    private final UUID targetTable = UUID.randomUUID(), snapshot = UUID.randomUUID();
    private final String local = "/api/v1/database/" + database + "/replication/snapshots";
    private final String remote = "https://target.example/api/v1/database/" + targetDatabase + "/replication/snapshots";
    private final Envelope envelope = new Envelope(new Manifest(1, snapshot, "https://origin.example", database, table,
            UUID.randomUUID(), 10, UUID.randomUUID(), 0, null, List.of(), 256, 4194304, 0, 0, 0, "history", "keys"), "manifest");

    @Test
    void retriesReuseAnImmutableSnapshotAndCatchUpThroughOneFixedBoundary() {
        ready();
        final var first = event(11, table);
        final var other = event(12, UUID.randomUUID());
        final var last = event(13, table);
        journal(10, null, new ReplicationJournalDto(13, 0, 12, List.of(first, other)));
        journal(12, 13L, new ReplicationJournalDto(13, 0, 13, List.of(last)));
        final var delivered = new ArrayList<ReplicationJournalDto.Event>();
        run(delivered);
        assertEquals(List.of(first, last), delivered);
        verify(source, never()).exchange(eq(local), eq(HttpMethod.POST), any(), eq(Envelope.class));
        verify(target, never()).exchange(contains("/checkpoint"), any(), any(), eq(Checkpoint.class));
    }

    @Test
    void missingJournalEventsCannotBeReportedAsComplete() {
        ready();
        journal(10, null, new ReplicationJournalDto(12, 0, 12, List.of(event(12, table))));
        assertThrows(IllegalStateException.class, () -> run(new ArrayList<>()));
    }

    @Test
    void missingAndUnfinishedExportsRetryTheSameSavedRequest() {
        ready();
        when(source.exchange(eq(local + "/" + snapshot), eq(HttpMethod.GET), any(), eq(Envelope.class)))
                .thenThrow(HttpClientErrorException.create(HttpStatus.NOT_FOUND, "absent", HttpHeaders.EMPTY, null, null))
                .thenThrow(HttpServerErrorException.create(HttpStatus.SERVICE_UNAVAILABLE, "building", HttpHeaders.EMPTY, null, null));
        when(source.exchange(eq(local), eq(HttpMethod.POST), any(), eq(Envelope.class)))
                .thenReturn(ResponseEntity.ok(envelope));
        journal(10, null, new ReplicationJournalDto(10, 0, 10, List.of()));
        run(new ArrayList<>());
        run(new ArrayList<>());
        verify(source, times(2)).exchange(eq(local), eq(HttpMethod.POST),
                argThat(entity -> new Create(snapshot, table, null).equals(entity.getBody())), eq(Envelope.class));
    }

    @Test
    void anUnreconciledHistoryArtifactDoesNotStartCatchUp() {
        ready();
        when(target.exchange(eq(remote + "/" + snapshot + "/reconcile"), eq(HttpMethod.POST), any(), eq(Receipt.class)))
                .thenReturn(ResponseEntity.ok(new Receipt(snapshot, targetTable, "VERIFIED", 10, 0, "manifest", true, false)));
        assertThrows(IllegalStateException.class, () -> run(new ArrayList<>()));
        verify(source, never()).exchange(contains("/journal"), any(), any(), eq(ReplicationJournalDto.class));
    }

    @Test
    void cancellationStopsBeforeNetworkAccess() {
        assertThrows(IllegalStateException.class, () -> transfer.transfer(snapshot, database, table, "https://target.example",
                targetDatabase, targetTable, 2, null, () -> { throw new IllegalStateException("cancelled"); }, event -> fail()));
        verifyNoInteractions(source, target);
    }

    @Test
    void sourceBoundaryCannotChangeBetweenPages() {
        ready();
        journal(10, null, new ReplicationJournalDto(12, 0, 11, List.of(event(11, table))));
        journal(11, 12L, new ReplicationJournalDto(13, 0, 12, List.of(event(12, table))));
        assertThrows(IllegalStateException.class, () -> run(new ArrayList<>()));
    }

    private void ready() {
        when(source.exchange(eq(local + "/" + snapshot), eq(HttpMethod.GET), any(), eq(Envelope.class)))
                .thenReturn(ResponseEntity.ok(envelope));
        final Receipt receipt = new Receipt(snapshot, targetTable, "RECONCILED", 10, 0, "manifest", true, true);
        when(target.exchange(eq(remote + "/imports"), eq(HttpMethod.POST), any(), eq(Receipt.class)))
                .thenReturn(ResponseEntity.ok(receipt));
        when(target.exchange(eq(remote + "/" + snapshot + "/reconcile"), eq(HttpMethod.POST), any(), eq(Receipt.class)))
                .thenReturn(ResponseEntity.ok(receipt));
    }

    private void journal(long after, Long through, ReplicationJournalDto page) {
        final String path = "/api/v1/database/" + database + "/replication/journal?after=" + after + "&limit=2"
                + (through == null ? "" : "&through=" + through);
        when(source.exchange(eq(path), eq(HttpMethod.GET), any(), eq(ReplicationJournalDto.class)))
                .thenReturn(ResponseEntity.ok(page));
    }

    private ReplicationJournalDto.Event event(long sequence, UUID tableId) {
        return new ReplicationJournalDto.Event("POST", DataReplicationDto.builder().eventId(UUID.randomUUID())
                .eventSequence(sequence).database(DatabaseDto.builder().id(database).build())
                .table(TableDto.builder().id(tableId).build()).build());
    }

    private void run(List<ReplicationJournalDto.Event> delivered) {
        transfer.transfer(snapshot, database, table, "https://target.example", targetDatabase, targetTable, 2, null, () -> {}, delivered::add);
    }
}
