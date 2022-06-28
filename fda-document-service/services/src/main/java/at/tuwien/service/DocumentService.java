package at.tuwien.service;

import at.tuwien.api.document.record.CreateDraftDto;
import at.tuwien.api.document.record.RecordDto;
import at.tuwien.exception.DraftRecordCreateException;

import java.security.Principal;

public interface DocumentService {

    RecordDto findById(String id, Principal principal) throws DraftRecordCreateException;

    RecordDto create(CreateDraftDto data, Principal principal) throws DraftRecordCreateException;

    RecordDto publish(String id, Principal principal) throws DraftRecordCreateException;

    RecordDto reserveDoi(String id, Principal principal) throws DraftRecordCreateException;

    void delete(String id, Principal principal) throws DraftRecordCreateException;
}
