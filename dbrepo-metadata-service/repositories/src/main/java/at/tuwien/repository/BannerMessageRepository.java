package at.tuwien.repository;

import at.tuwien.entities.maintenance.BannerMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BannerMessageRepository extends JpaRepository<BannerMessage, UUID> {

    List<BannerMessage> findByActive();

}
