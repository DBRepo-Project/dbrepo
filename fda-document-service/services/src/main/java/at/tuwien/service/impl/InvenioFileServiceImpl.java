package at.tuwien.service.impl;

import at.tuwien.api.database.query.ImportDto;
import at.tuwien.api.document.file.FileDto;
import at.tuwien.entities.user.User;
import at.tuwien.exception.FileUploadException;
import at.tuwien.exception.CommitFileUploadException;
import at.tuwien.exception.DraftRecordCreateException;
import at.tuwien.exception.UserNotFoundException;
import at.tuwien.gateway.DocumentGateway;
import at.tuwien.service.FileService;
import at.tuwien.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.Principal;

@Slf4j
@Service
public class InvenioFileServiceImpl implements FileService {

    private final UserService userService;
    private final DocumentGateway documentGateway;

    @Autowired
    public InvenioFileServiceImpl(UserService userService, DocumentGateway documentGateway) {
        this.userService = userService;
        this.documentGateway = documentGateway;
    }

    @Override
    public FileDto uploadFile(String id, ImportDto file, Principal principal)
            throws DraftRecordCreateException, CommitFileUploadException, FileUploadException, UserNotFoundException {
        /* get token */
        final User user = userService.find(principal.getName());
        /* remote */
        final FileDto document = documentGateway.uploadFile(id, file, user.getInvenioToken());
        log.info("Deposited draft file content for record with id {}", id);
        log.debug("Deposited draft file content for record {}", document);
        return document;
    }

}
