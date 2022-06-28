package at.tuwien.gateway;

import at.tuwien.api.document.file.FileDto;
import at.tuwien.api.document.record.CreateDraftDto;
import at.tuwien.api.document.record.RecordDto;
import at.tuwien.exception.FileUploadException;
import at.tuwien.exception.CommitFileUploadException;
import at.tuwien.exception.DraftRecordCreateException;
import org.springframework.web.multipart.MultipartFile;

public interface DocumentGateway {
    RecordDto createDraft(CreateDraftDto data, String token) throws DraftRecordCreateException;

    RecordDto reserveDraftDoi(String id, String token) throws DraftRecordCreateException;

    RecordDto findDraft(String id, String token) throws DraftRecordCreateException;

    RecordDto publishDraft(String id, String token) throws DraftRecordCreateException;

    FileDto uploadFile(String id, MultipartFile file, String token)
            throws DraftRecordCreateException, FileUploadException,
            org.apache.tomcat.util.http.fileupload.FileUploadException,
            CommitFileUploadException;

    void delete(String id, String token) throws DraftRecordCreateException;
}
