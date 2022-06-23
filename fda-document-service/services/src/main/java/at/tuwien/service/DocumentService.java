package at.tuwien.service;

import at.tuwien.api.document.record.CreateDraftDto;
import at.tuwien.api.document.record.DraftDto;
import at.tuwien.exception.DraftRecordCreateException;

import java.security.Principal;

public interface DocumentService {

    DraftDto findById(String id, Principal principal) throws DraftRecordCreateException;

    DraftDto create(CreateDraftDto data, Principal principal) throws DraftRecordCreateException;

    DraftDto reserveDoi(String id, Principal principal) throws DraftRecordCreateException;

    void delete(String id, Principal principal) throws DraftRecordCreateException;
}
