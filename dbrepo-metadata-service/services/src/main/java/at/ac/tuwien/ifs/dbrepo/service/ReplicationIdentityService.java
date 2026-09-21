package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.ReplicationOwnerDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.NotAllowedException;

import java.util.Optional;

public interface ReplicationIdentityService {

    Optional<UserDto> resolve(ReplicationOwnerDto owner);

    void map(ReplicationOwnerDto owner, String localUsername) throws NotAllowedException;

}
