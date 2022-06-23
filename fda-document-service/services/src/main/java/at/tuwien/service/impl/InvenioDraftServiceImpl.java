package at.tuwien.service.impl;

import at.tuwien.api.document.record.CreateDraftDto;
import at.tuwien.api.document.record.DraftDto;
import at.tuwien.config.InvenioConfig;
import at.tuwien.exception.DraftRecordCreateException;
import at.tuwien.gateway.DocumentGateway;
import at.tuwien.service.DocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.Principal;

@Slf4j
@Service
public class InvenioDraftServiceImpl implements DocumentService {

    private final DocumentGateway documentGateway;
    private final InvenioConfig invenioConfig;

    @Autowired
    public InvenioDraftServiceImpl(DocumentGateway documentGateway, InvenioConfig invenioConfig) {
        this.documentGateway = documentGateway;
        this.invenioConfig = invenioConfig;
    }

    @Override
    public DraftDto findById(String id, Principal principal) throws DraftRecordCreateException {
        /* get token */
        /* remote */
        return documentGateway.findDraft(id, invenioConfig.getDebugToken());
    }

    @Override
    public DraftDto create(CreateDraftDto data, Principal principal) throws DraftRecordCreateException {
        /* get token */
        /* remote */
        final DraftDto document = documentGateway.createDraft(data, invenioConfig.getDebugToken());
        log.info("Created draft record with id {}", document.getId());
        log.debug("created draft record {}", document);
        return document;
    }

    @Override
    public DraftDto reserveDoi(String id, Principal principal) throws DraftRecordCreateException {
        /* get token */
        /* remote */
        final DraftDto document = documentGateway.reserveDraftDoi(id, invenioConfig.getDebugToken());
        log.info("Reserved DOI {} for draft record with id {}", document.getPids().getDoi(), document.getId());
        log.debug("reserved PID {} for draft record with id {}", document.getPids(), document);
        return document;
    }

    @Override
    public void delete(String id, Principal principal) throws DraftRecordCreateException {
        /* get token */
        /* remote */
        documentGateway.delete(id, invenioConfig.getDebugToken());
        log.info("Deleted draft record with id {}", id);
    }

}
