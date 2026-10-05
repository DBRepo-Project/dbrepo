package at.ac.tuwien.ifs.dbrepo.endpoints;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Retired result-transfer routes stay explicit so old senders cannot silently succeed. */
@RestController
@PreAuthorize("hasAuthority('replication')")
public class SubsetResultEndpoint {
    @RequestMapping({"/api/v1/database/{databaseId}/subset/{queryId}/result",
            "/api/v1/database/{databaseId}/subset/{queryId}/result/**"})
    @ResponseStatus(HttpStatus.GONE)
    public void gone() { }
}
