package at.tuwien.repository.sdb;

import at.tuwien.api.user.UserDto;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserIdxRepository extends ElasticsearchRepository<UserDto, UUID> {
}