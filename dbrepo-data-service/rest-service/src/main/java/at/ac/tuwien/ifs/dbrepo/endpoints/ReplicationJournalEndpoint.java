package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.ReplicationJournalDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.MetadataServiceException;
import at.ac.tuwien.ifs.dbrepo.core.exception.RemoteUnavailableException;
import at.ac.tuwien.ifs.dbrepo.service.MetadataService;
import at.ac.tuwien.ifs.dbrepo.service.impl.ReplicationJournalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/database/{databaseId}/replication/journal")
@RequiredArgsConstructor
public class ReplicationJournalEndpoint {
    private final MetadataService metadata;
    private final ReplicationJournalService journal;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('system', 'replication')")
    public ReplicationJournalDto read(@PathVariable UUID databaseId,
                                      @RequestParam(defaultValue = "0") long after,
                                      @RequestParam(required = false) Long through,
                                      @RequestParam(defaultValue = "100") int limit)
            throws RemoteUnavailableException, MetadataServiceException, DatabaseNotFoundException, SQLException {
        try {
            return journal.read(metadata.getDatabase(databaseId), after, through, limit);
        } catch (IllegalArgumentException failure) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, failure.getMessage());
        }
    }
}
