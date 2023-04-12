package at.tuwien.repository.elastic;

import at.tuwien.api.identifier.IdentifierDto;
import at.tuwien.api.user.UserDto;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserIdxRepository extends ElasticsearchRepository<UserDto, Long> {
}