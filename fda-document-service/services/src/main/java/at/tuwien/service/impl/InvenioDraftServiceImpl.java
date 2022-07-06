package at.tuwien.service.impl;

import at.tuwien.api.document.record.CreateDraftDto;
import at.tuwien.api.document.record.RecordDto;
import at.tuwien.entities.user.User;
import at.tuwien.exception.DraftRecordCreateException;
import at.tuwien.exception.UserInvenioTokenException;
import at.tuwien.exception.UserNotFoundException;
import at.tuwien.gateway.DocumentGateway;
import at.tuwien.service.DocumentService;
import at.tuwien.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.Principal;

@Slf4j
@Service
public class InvenioDraftServiceImpl implements DocumentService {

    private final UserService userService;
    private final DocumentGateway documentGateway;

    @Autowired
    public InvenioDraftServiceImpl(UserService userService, DocumentGateway documentGateway) {
        this.userService = userService;
        this.documentGateway = documentGateway;
    }

    @Override
    public RecordDto findById(String id, Principal principal)
            throws DraftRecordCreateException, UserNotFoundException, UserInvenioTokenException {
        /* get token */
        final User user = findUserWithInvenioToken(principal);
        /* remote */
        return documentGateway.findDraft(id, user.getInvenioToken());
    }

    @Override
    public RecordDto create(CreateDraftDto data, Principal principal)
            throws DraftRecordCreateException, UserNotFoundException, UserInvenioTokenException {
        /* get token */
        final User user = findUserWithInvenioToken(principal);
        /* remote */
        final RecordDto document = documentGateway.createDraft(data, user.getInvenioToken());
        log.info("Created draft record with id {}", document.getId());
        log.debug("created draft record {}", document);
        return document;
    }

    @Override
    public RecordDto publish(String id, Principal principal)
            throws DraftRecordCreateException, UserNotFoundException, UserInvenioTokenException {
        /* get token */
        final User user = findUserWithInvenioToken(principal);
        /* remote */
        final RecordDto document = documentGateway.publishDraft(id, user.getInvenioToken());
        log.info("Published draft record with id {}", document.getId());
        log.debug("published draft record {}", document);
        return document;
    }

    @Override
    public RecordDto reserveDoi(String id, Principal principal) throws DraftRecordCreateException,
            UserNotFoundException, UserInvenioTokenException {
        /* get token */
        final User user = findUserWithInvenioToken(principal);
        /* remote */
        final RecordDto document = documentGateway.reserveDraftDoi(id, user.getInvenioToken());
        log.info("Reserved DOI {} for draft record with id {}", document.getPids().getDoi(), document.getId());
        log.debug("reserved PID {} for draft record with id {}", document.getPids(), document);
        return document;
    }

    @Override
    public void delete(String id, Principal principal) throws DraftRecordCreateException, UserNotFoundException {
        /* get token */
        final User user = userService.find(principal.getName());
        /* remote */
        documentGateway.delete(id, user.getInvenioToken());
        log.info("Deleted draft record with id {}", id);
    }

    private User findUserWithInvenioToken(Principal principal) throws UserNotFoundException, UserInvenioTokenException {
        final User user = userService.find(principal.getName());
        if (user.getInvenioToken() == null) {
            log.error("Failed to find invenio token for user with id {}", user.getId());
            throw new UserInvenioTokenException("Failed to find invenio token");
        }
        return user;
    }

}
