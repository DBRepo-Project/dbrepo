package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.CreateTableDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.ReplicaLocation;
import at.ac.tuwien.ifs.dbrepo.service.TableService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/replication/table")
@RequiredArgsConstructor
@Tag(name = "Table", description = "Table replication endpoints")
public class TableEndpoint {

    private final TableService tableService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> replicateTable(List<ReplicaLocation> replicas, CreateTableDto createTableDto) {

        tableService.handleTableReplication(replicas, createTableDto);

        Map<String, Object> response = Map.of(
                "status", "success",
                "message", "Database replication notification received successfully",
                "databaseInformation", createTableDto
        );

        return ResponseEntity.ok(response);
    }


}
