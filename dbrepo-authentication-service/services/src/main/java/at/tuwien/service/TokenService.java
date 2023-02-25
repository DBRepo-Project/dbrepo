package at.tuwien.service;

import at.tuwien.entities.user.Token;
import at.tuwien.exception.TokenNotEligableException;
import at.tuwien.exception.TokenNotFoundException;
import at.tuwien.exception.UserNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.ServletException;
import java.security.Principal;
import java.util.List;

@Service
public interface TokenService {

    /**
     * Finds all tokens in the metadata database for a given user principal.
     *
     * @param principal The user principal.
     * @return The list of tokens, if successful.
     * @throws UserNotFoundException The user does not exist in the metadata database.
     */
    List<Token> findAll(Principal principal) throws UserNotFoundException;

    /**
     * Creates a token for a given user principal.
     *
     * @param principal The user principal.
     * @return The created token, if successful.
     * @throws UserNotFoundException     The user does not exist in the metadata database.
     * @throws TokenNotEligableException The user is not eligable to mint developer tokens.
     */
    Token create(Principal principal) throws UserNotFoundException, TokenNotEligableException;

    /**
     * Finds a token by hash.
     *
     * @param tokenHash The token hash.
     * @return The token, if successful.
     * @throws TokenNotFoundException The token was not found in the metadata database.
     */
    Token findOne(String tokenHash) throws TokenNotFoundException;

    /**
     * Finds a token by id.
     *
     * @param id The token id.
     * @return The token, if successful.
     * @throws TokenNotFoundException The token was not found in the metadata database.
     */
    Token findOne(Long id) throws TokenNotFoundException;

    /**
     * Deletes a developer token in the metadata database by hash and user principal.
     *
     * @param tokenHash The token hash.
     * @param principal The user principal.
     * @throws TokenNotFoundException The token was not found in the metadata database.
     * @throws UserNotFoundException  The user does not exist in the metadata database.
     */
    void delete(String tokenHash, Principal principal) throws TokenNotFoundException, UserNotFoundException;

    /**
     * Checks if the developer token has not been marked as deleted
     *
     * @param jwt The token
     * @throws ServletException The jwt token is marked as invalid.
     */
    void check(String jwt) throws ServletException;
}
