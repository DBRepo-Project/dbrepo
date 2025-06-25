package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.analyse.SchemaAnalysisResultDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.AnalyseDataTypesException;
import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseUnavailableException;
import at.ac.tuwien.ifs.dbrepo.core.exception.StorageNotFoundException;
import at.ac.tuwien.ifs.dbrepo.service.AnalyseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@CrossOrigin(origins = "*")
@RequestMapping(path = "/api/analyse")
public class AnalyseEndpoint extends RestEndpoint {

    private final AnalyseService analyseService;

    @Autowired
    public AnalyseEndpoint(AnalyseService analyseService) {
        this.analyseService = analyseService;
    }

    @GetMapping("/schema/{key}")
    @PreAuthorize("hasAuthority('analyse-datatypes')")
    public ResponseEntity<SchemaAnalysisResultDto> analyseDatatypes(@PathVariable("key") String key)
            throws AnalyseDataTypesException, DatabaseUnavailableException, StorageNotFoundException {
        log.debug("endpoint analyse datatypes, key={}", key);
        return ResponseEntity.ok()
                .body(analyseService.determineDataTypes(key));
    }

}
