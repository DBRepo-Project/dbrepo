package at.tuwien.service.impl;

import at.tuwien.api.document.file.FileDto;
import at.tuwien.config.InvenioConfig;
import at.tuwien.exception.FileUploadException;
import at.tuwien.exception.CommitFileUploadException;
import at.tuwien.exception.DraftRecordCreateException;
import at.tuwien.gateway.DocumentGateway;
import at.tuwien.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
    public FileDto uploadFile(String id, MultipartFile file, Principal principal)
            throws DraftRecordCreateException, CommitFileUploadException, FileUploadException,
            org.apache.tomcat.util.http.fileupload.FileUploadException {
        /* get token */
        /* remote */
        final FileDto document = documentGateway.uploadFile(id, file, invenioConfig.getDebugToken());
        log.info("Deposited draft file content for record with id {}", id);
        log.debug("Deposited draft file content for record {}", document);
        return document;
    }

}
