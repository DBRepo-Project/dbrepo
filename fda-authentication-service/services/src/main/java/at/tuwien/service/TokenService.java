package at.tuwien.service;

import at.tuwien.entities.user.Token;
import at.tuwien.exception.TokenNotEligableException;
import at.tuwien.exception.TokenNotFoundException;
import at.tuwien.exception.UserNotFoundException;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.List;

@Service
public interface TokenService {

    List<Token> findAll(Principal principal) throws UserNotFoundException;

    Token create(Principal principal) throws UserNotFoundException, TokenNotEligableException;

    Token findOne(String tokenHash) throws TokenNotFoundException;

    void delete(String tokenHash, Principal principal) throws TokenNotFoundException, UserNotFoundException;
}
