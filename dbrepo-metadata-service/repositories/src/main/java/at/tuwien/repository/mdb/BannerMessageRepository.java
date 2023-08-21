package at.tuwien.repository.mdb;

import at.tuwien.entities.maintenance.BannerMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BannerMessageRepository extends JpaRepository<BannerMessage, Long> {

    List<BannerMessage> findByActive();

}
