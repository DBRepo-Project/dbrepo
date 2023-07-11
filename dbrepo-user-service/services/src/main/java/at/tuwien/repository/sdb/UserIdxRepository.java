package at.tuwien.repository.sdb;

import at.tuwien.entities.user.User;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserIdxRepository extends ElasticsearchRepository<User, UUID> {
}