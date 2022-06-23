package at.tuwien.service.impl;

import at.tuwien.api.document.file.FileStartDto;
import at.tuwien.config.InvenioConfig;
import at.tuwien.exception.DraftRecordCreateException;
import at.tuwien.gateway.DocumentGateway;
import at.tuwien.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.Principal;

@Slf4j
@Service
public class InvenioFileServiceImpl implements FileService {

    private final DocumentGateway documentGateway;
    private final InvenioConfig invenioConfig;

    @Autowired
    public InvenioFileServiceImpl(DocumentGateway documentGateway, InvenioConfig invenioConfig) {
        this.documentGateway = documentGateway;
        this.invenioConfig = invenioConfig;
    }

    @Override
    public FileStartDto start(String id, Principal principal) throws DraftRecordCreateException {
        /* get token */
        /* remote */
        final FileStartDto document = documentGateway.startUpload(id, invenioConfig.getDebugToken());
        log.info("Started draft files with id {}", id);
        log.debug("started draft files {}", document);
        return document;
    }

}
