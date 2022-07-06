package at.tuwien.service;

import at.tuwien.api.document.record.CreateDraftDto;
import at.tuwien.api.document.record.RecordDto;
import at.tuwien.exception.DraftRecordCreateException;
import at.tuwien.exception.UserInvenioTokenException;
import at.tuwien.exception.UserNotFoundException;

import java.security.Principal;

public interface DocumentService {

    RecordDto findById(String id, Principal principal)
            throws DraftRecordCreateException, UserNotFoundException, UserInvenioTokenException;

    RecordDto create(CreateDraftDto data, Principal principal)
            throws DraftRecordCreateException, UserNotFoundException, UserInvenioTokenException;

    RecordDto publish(String id, Principal principal)
            throws DraftRecordCreateException, UserNotFoundException, UserInvenioTokenException;

    RecordDto reserveDoi(String id, Principal principal)
            throws DraftRecordCreateException, UserNotFoundException, UserInvenioTokenException;

    void delete(String id, Principal principal) throws DraftRecordCreateException, UserNotFoundException;
}
