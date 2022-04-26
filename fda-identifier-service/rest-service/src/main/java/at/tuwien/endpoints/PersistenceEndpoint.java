package at.tuwien.endpoints;

import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.exception.IdentifierNotFoundException;
import at.tuwien.mapper.IdentifierMapper;
import at.tuwien.service.IdentifierService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Log4j2
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/pid")
public class PersistenceEndpoint {

    private final IdentifierMapper identifierMapper;
    private final IdentifierService identifierService;

    @Autowired
    public PersistenceEndpoint(IdentifierMapper identifierMapper, IdentifierService identifierService) {
        this.identifierMapper = identifierMapper;
        this.identifierService = identifierService;
    }

    @GetMapping("/{pid}")
    @Transactional(readOnly = true)
    @Operation(summary = "Find some identifier")
    public ResponseEntity<IdentifierDto> find(@Valid @PathVariable("pid") Long pid) throws IdentifierNotFoundException {
        return ResponseEntity.ok(identifierMapper.identifierToIdentifierDto(identifierService.find(pid)));
    }

}
