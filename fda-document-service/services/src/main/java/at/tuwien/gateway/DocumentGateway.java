package at.tuwien.gateway;

import at.tuwien.api.document.file.FileStartDto;
import at.tuwien.api.document.record.CreateDraftDto;
import at.tuwien.api.document.record.DraftDto;
import at.tuwien.exception.DraftRecordCreateException;

public interface DocumentGateway {
    DraftDto createDraft(CreateDraftDto data, String token) throws DraftRecordCreateException;

    DraftDto reserveDraftDoi(String id, String token) throws DraftRecordCreateException;

    DraftDto findDraft(String id, String token) throws DraftRecordCreateException;

    FileStartDto startUpload(String id, String token) throws DraftRecordCreateException;

    void delete(String id, String token) throws DraftRecordCreateException;
}
