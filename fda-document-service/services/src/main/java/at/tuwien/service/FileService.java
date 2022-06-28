package at.tuwien.service;


import at.tuwien.api.document.file.FileDto;
import at.tuwien.exception.FileUploadException;
import at.tuwien.exception.CommitFileUploadException;
import at.tuwien.exception.DraftRecordCreateException;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

public interface FileService {

    FileDto uploadFile(String id, MultipartFile file, Principal principal)
            throws DraftRecordCreateException, CommitFileUploadException, FileUploadException,
            org.apache.tomcat.util.http.fileupload.FileUploadException;
}
