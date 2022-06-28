package at.tuwien.gateway;

import at.tuwien.api.database.query.ImportDto;
import at.tuwien.api.document.file.FileDto;
import at.tuwien.api.document.record.CreateDraftDto;
import at.tuwien.api.document.record.RecordDto;
import at.tuwien.exception.FileUploadException;
import at.tuwien.exception.CommitFileUploadException;
import at.tuwien.exception.DraftRecordCreateException;


public interface DocumentGateway {
    RecordDto createDraft(CreateDraftDto data, String token) throws DraftRecordCreateException;

    RecordDto reserveDraftDoi(String id, String token) throws DraftRecordCreateException;

    RecordDto findDraft(String id, String token) throws DraftRecordCreateException;

    RecordDto publishDraft(String id, String token) throws DraftRecordCreateException;

    FileDto uploadFile(String id, ImportDto file, String token)
            throws DraftRecordCreateException, FileUploadException, CommitFileUploadException;

    void delete(String id, String token) throws DraftRecordCreateException;
}
