package at.tuwien.endpoint;

import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.api.user.UserBriefDto;
import at.tuwien.entities.auth.Realm;
import at.tuwien.entities.user.Role;
import at.tuwien.exception.*;
import at.tuwien.mapper.UserMapper;
import at.tuwien.service.RealmService;
import at.tuwien.service.RoleService;
import at.tuwien.service.UserService;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/user")
public class UserEndpoint {

    private final UserMapper userMapper;
    private final UserService userService;
    private final RealmService realmService;
    private final RoleService roleService;

    @Autowired
    public UserEndpoint(UserMapper userMapper, UserService userService, RealmService realmService,
                        RoleService roleService) {
        this.userMapper = userMapper;
        this.userService = userService;
        this.realmService = realmService;
        this.roleService = roleService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Timed(value = "user.list", description = "Time needed to list all users in the metadata database")
    @Operation(summary = "Find all users")
    public ResponseEntity<List<UserBriefDto>> findAll() {
        log.debug("endpoint find all users");
        final List<UserBriefDto> users = userService.findAll()
                .stream()
                .map(userMapper::userToUserBriefDto)
                .collect(Collectors.toList());
        log.trace("find all users resulted in users {}", users);
        return ResponseEntity.ok(users);
    }

    @PostMapping
    @Transactional
    @Timed(value = "user.create", description = "Time needed to create a user in the metadata database")
    @Operation(summary = "Create a user")
    public ResponseEntity<UserBriefDto> create(@NotNull @Valid @RequestBody SignupRequestDto data)
            throws UserNotFoundException, RemoteUnavailableException, RealmNotFoundException,
            UserAlreadyExistsException, RoleNotFoundException {
        log.debug("endpoint create a user, data={}", data);
        final Realm realm = realmService.find("dbrepo");
        final Role role = roleService.find("default-researcher-roles");
        final UserBriefDto dto = userMapper.userToUserBriefDto(userService.create(data, realm, role));
        log.trace("create user resulted in dto {}", dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dto);
    }

}
