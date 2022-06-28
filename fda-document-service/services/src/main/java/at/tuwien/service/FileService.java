package at.tuwien.service;


import at.tuwien.api.database.query.ImportDto;
import at.tuwien.api.document.file.FileDto;
import at.tuwien.exception.FileUploadException;
import at.tuwien.exception.CommitFileUploadException;
import at.tuwien.exception.DraftRecordCreateException;

import java.security.Principal;

public interface FileService {

    FileDto uploadFile(String id, ImportDto file, Principal principal)
            throws DraftRecordCreateException, CommitFileUploadException, FileUploadException;
}
