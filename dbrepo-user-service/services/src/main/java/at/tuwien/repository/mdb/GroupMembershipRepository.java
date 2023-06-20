package at.tuwien.repository.mdb;

import at.tuwien.entities.user.GroupMembership;
import at.tuwien.entities.user.GroupMembershipKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupMembershipRepository extends JpaRepository<GroupMembership, GroupMembershipKey> {
}
