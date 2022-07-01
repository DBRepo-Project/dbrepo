package at.tuwien.endpoint;

import at.tuwien.api.database.query.QueryDto;
import at.tuwien.entities.user.User;
import at.tuwien.mapper.UserMapper;
import at.tuwien.querystore.Query;
import at.tuwien.exception.*;
import at.tuwien.mapper.QueryMapper;
import at.tuwien.service.*;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.security.Principal;
import java.util.List;

@Log4j2
@RestController
@RequestMapping("/api/container/{id}/database/{databaseId}/query")
public class StoreEndpoint extends AbstractEndpoint {

    private final UserMapper userMapper;
    private final QueryMapper queryMapper;
    private final UserService userService;
    private final StoreService storeService;

    @Autowired
    public StoreEndpoint(TableService tableService, UserMapper userMapper, QueryMapper queryMapper,
                         UserService userService, StoreService storeService, DatabaseService databaseService,
                         IdentifierService identifierService) {
        super(tableService, databaseService, identifierService);
        this.userMapper = userMapper;
        this.queryMapper = queryMapper;
        this.userService = userService;
        this.storeService = storeService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#databaseId, 'QUERY_VIEW_ALL')")
    @Operation(summary = "Find queries")
    public ResponseEntity<List<QueryDto>> findAll(@NotNull @PathVariable("id") Long id,
                                                  @NotNull @PathVariable("databaseId") Long databaseId,
                                                  @NotNull Principal principal)
            throws QueryStoreException,
            DatabaseNotFoundException, ImageNotSupportedException, ContainerNotFoundException, NotAllowedException {
        if (!hasQueryPermission(databaseId, null, "QUERY_VIEW_ALL", principal)) {
            log.error("Missing view all queries permission");
            throw new NotAllowedException("Missing view all queries permission");
        }
        final List<Query> queries = storeService.findAll(id, databaseId);
        return ResponseEntity.ok(queryMapper.queryListToQueryDtoList(queries));
    }

    @GetMapping("/{queryId}")
    @Transactional(readOnly = true)
    @Operation(summary = "Find some query")
    public ResponseEntity<QueryDto> find(@NotNull @PathVariable("id") Long id,
                                         @NotNull @PathVariable("databaseId") Long databaseId,
                                         @NotNull @PathVariable Long queryId,
                                         @NotNull Principal principal)
            throws DatabaseNotFoundException, ImageNotSupportedException,
            QueryStoreException, QueryNotFoundException, ContainerNotFoundException, UserNotFoundException,
            NotAllowedException {
        if (!hasQueryPermission(databaseId, queryId, "QUERY_VIEW", principal)) {
            log.error("Missing view query permission");
            throw new NotAllowedException("Missing view query permission");
        }
        final Query query = storeService.findOne(id, databaseId, queryId);
        final QueryDto dto = queryMapper.queryToQueryDto(query);
        final User creator = userService.find(query.getCreatedBy());
        dto.setCreator(userMapper.userToUserDto(creator));
        return ResponseEntity.ok(dto);
    }
}
