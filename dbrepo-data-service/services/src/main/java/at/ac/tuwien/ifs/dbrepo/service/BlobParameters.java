package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.exception.StorageNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.StorageUnavailableException;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.ResponseInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Keeps S3 parameter streams open until JDBC execution finishes, then releases every response. */
@Slf4j
public final class BlobParameters implements AutoCloseable {
    private final StorageService storage;
    private final List<InputStream> streams = new ArrayList<>();

    public BlobParameters(StorageService storage) {
        this.storage = storage;
    }

    public InputStream open(String key) throws StorageNotFoundException, StorageUnavailableException {
        final InputStream stream = storage.getObject(key);
        streams.add(stream);
        return stream;
    }

    public void rethrowTransferFailure(SQLException failure) throws StorageUnavailableException {
        if (!streams.isEmpty()) {
            for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
                if (cause instanceof IOException) {
                    throw new StorageUnavailableException("Failed to transfer BLOB data", failure);
                }
            }
        }
    }

    @Override
    public void close() {
        for (InputStream stream : streams) {
            try {
                // Abort unread responses after binding/execution failure instead of draining a large object.
                if (stream instanceof ResponseInputStream<?> response) response.abort();
                else stream.close();
            } catch (IOException | RuntimeException error) {
                log.warn("Failed to release a BLOB parameter response", error);
            }
        }
    }
}
