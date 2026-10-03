package at.ac.tuwien.ifs.dbrepo.core.api.replication;

import java.util.List;

/** A bounded page of retained events through an immutable committed prefix. */
public record ReplicationJournalDto(long through, long legacyThrough, long nextAfter,
                                    List<Event> events) {
    public record Event(String method, DataReplicationDto payload) { }
}
