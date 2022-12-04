package at.tuwien.repositories;

import at.tuwien.entities.user.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {

    @Query("select t from Token t where t.creator = :userid")
    List<Token> findMine(@Param("userid") Long userid);

    Optional<Token> findByTokenHash(String tokenHash);


    Optional<Token> findByValidTokenHash(@Param("hash") String tokenHash);

}
