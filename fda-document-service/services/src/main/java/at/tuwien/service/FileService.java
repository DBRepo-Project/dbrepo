package at.tuwien.service;


import at.tuwien.api.document.file.FileStartDto;
import at.tuwien.exception.DraftRecordCreateException;

import java.security.Principal;

public interface FileService {
    FileStartDto start(String id, Principal principal) throws DraftRecordCreateException;
}
