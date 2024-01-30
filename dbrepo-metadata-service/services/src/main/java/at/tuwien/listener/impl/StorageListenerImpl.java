package at.tuwien.listener.impl;

import at.tuwien.config.S3Config;
import at.tuwien.exception.FileStorageException;
import at.tuwien.listener.StorageListener;
import at.tuwien.service.StorageService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Log4j2
@Component
public class StorageListenerImpl implements StorageListener {

    final S3Config s3Config;
    final StorageService storageService;

    @Autowired
    public StorageListenerImpl(S3Config s3Config, StorageService storageService) {
        this.s3Config = s3Config;
        this.storageService = storageService;
    }

    @Override
    @Scheduled(fixedRateString = "${fda.s3.deleteStaleFilesRate}", timeUnit = TimeUnit.SECONDS)
    public void deleteStaleFiles() throws FileStorageException {
        storageService.deleteStaleFiles(s3Config.getS3ExportBucket());
        storageService.deleteStaleFiles(s3Config.getS3ImportBucket());
    }

}
