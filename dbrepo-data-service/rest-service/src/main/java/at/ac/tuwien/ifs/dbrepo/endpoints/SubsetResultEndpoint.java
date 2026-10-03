package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.SubsetResultManifestDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.SubsetResultManifestDto.Progress;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.service.MetadataService;
import at.ac.tuwien.ifs.dbrepo.service.SubsetReplicationService;
import at.ac.tuwien.ifs.dbrepo.service.SubsetResultService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/database/{databaseId}/subset/{queryId}/result")
@PreAuthorize("hasAuthority('replication')")
public class SubsetResultEndpoint {
    private final MetadataService metadata;
    private final SubsetReplicationService replication;
    private final SubsetResultService results;

    public SubsetResultEndpoint(MetadataService metadata, SubsetReplicationService replication, SubsetResultService results) {
        this.metadata = metadata;
        this.replication = replication;
        this.results = results;
    }

    @PutMapping
    public Progress begin(@PathVariable UUID databaseId, @PathVariable UUID queryId,
                          @Valid @RequestBody SubsetResultManifestDto manifest) throws Exception {
        if (!queryId.equals(manifest.query().queryId())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Query ID mismatch");
        final Database database = metadata.getDatabase(databaseId);
        replication.receive(database, manifest.query());
        return results.begin(database, manifest);
    }

    @PutMapping(value = "/rows/{row}/chunks/{offset}", consumes = "application/octet-stream")
    public Progress append(@PathVariable UUID databaseId, @PathVariable UUID queryId,
                           @PathVariable long row, @PathVariable long offset,
                           @RequestHeader("X-Subset-Sender") String sender,
                           @RequestHeader("X-Subset-Database") UUID sourceId,
                           @RequestHeader("X-Row-Length") long rowLength,
                           @RequestHeader("X-Row-Hash") String rowHash,
                           @RequestHeader("X-Chunk-Hash") String chunkHash, HttpServletRequest request) throws Exception {
        final Database database = metadata.getDatabase(databaseId);
        replication.requireSender(database, sender, sourceId);
        final byte[] bytes = request.getInputStream().readNBytes(SubsetResultService.CHUNK_SIZE + 1);
        if (bytes.length > SubsetResultService.CHUNK_SIZE) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE);
        return results.append(database, queryId, row, offset, rowLength, rowHash, chunkHash, bytes);
    }

    @PostMapping("/publish")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void publish(@PathVariable UUID databaseId, @PathVariable UUID queryId,
                        @RequestHeader("X-Subset-Sender") String sender,
                        @RequestHeader("X-Subset-Database") UUID sourceId) throws Exception {
        final Database database = metadata.getDatabase(databaseId);
        replication.requireSender(database, sender, sourceId);
        results.publish(database, queryId);
    }
}
