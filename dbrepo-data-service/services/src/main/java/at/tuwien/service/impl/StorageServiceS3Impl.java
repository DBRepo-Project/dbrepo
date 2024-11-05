package at.tuwien.service.impl;

import at.tuwien.ExportResourceDto;
import at.tuwien.config.S3Config;
import at.tuwien.exception.MalformedException;
import at.tuwien.exception.StorageNotFoundException;
import at.tuwien.exception.StorageUnavailableException;
import at.tuwien.service.StorageService;
import lombok.extern.log4j.Log4j2;
import org.apache.spark.sql.*;
import org.apache.spark.sql.types.StructField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static scala.collection.JavaConverters.asScalaIteratorConverter;

@Log4j2
@Service
public class StorageServiceS3Impl implements StorageService {

    private final S3Config s3Config;
    private final S3Client s3Client;
    private final SparkSession sparkSession;

    @Autowired
    public StorageServiceS3Impl(S3Config s3Config, S3Client s3Client, SparkSession sparkSession) {
        this.s3Config = s3Config;
        this.s3Client = s3Client;
        this.sparkSession = sparkSession;
    }

    @Override
    public InputStream getObject(String bucket, String key) throws StorageNotFoundException,
            StorageUnavailableException {
        try {
            return s3Client.getObject(GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
        } catch (NoSuchKeyException e) {
            log.error("Failed to find object: not found: {}", e.getMessage());
            throw new StorageNotFoundException("Failed to find object: not found: " + e.getMessage(), e);
        } catch (S3Exception e) {
            log.error("Failed to find object: other error: {}", e.getMessage());
            throw new StorageUnavailableException("Failed to find object: other error: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] getBytes(String key) throws StorageNotFoundException, StorageUnavailableException {
        return getBytes(s3Config.getS3Bucket(), key);
    }

    @Override
    public byte[] getBytes(String bucket, String key) throws StorageNotFoundException, StorageUnavailableException {
        try {
            return getObject(bucket, key)
                    .readAllBytes();
        } catch (IOException e) {
            log.error("Failed to read bytes from input stream: {}", e.getMessage());
            throw new StorageNotFoundException("Failed to read bytes from input stream: " + e.getMessage(), e);
        }
    }

    @Override
    public ExportResourceDto getResource(String key) throws StorageNotFoundException, StorageUnavailableException {
        return getResource(s3Config.getS3Bucket(), key);
    }

    @Override
    public ExportResourceDto getResource(String bucket, String key) throws StorageNotFoundException,
            StorageUnavailableException {
        final InputStreamResource resource = new InputStreamResource(getObject(bucket, key));
        log.trace("return export resource with filename: {}", key);
        return ExportResourceDto.builder()
                .resource(resource)
                .filename(key)
                .build();
    }

    @Override
    public ExportResourceDto transformDataset(Dataset<Row> dataset) throws StorageUnavailableException {
        final List<Map<String, String>> inMemory = dataset.collectAsList()
                .stream()
                .map(row -> {
                    final Map<String, String> map = new LinkedHashMap<>();
                    for (int i = 0; i < dataset.columns().length; i++) {
                        map.put(dataset.columns()[i], row.get(i) != null ? String.valueOf(row.get(i)) : "");
                    }
                    return map;
                })
                .toList();
        log.debug("collected dataset with {} row(s)", inMemory.size());
        try {
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try (Writer w = new OutputStreamWriter(byteArrayOutputStream, Charset.defaultCharset())) {
                /* header */
                w.write(String.join(",", Arrays.stream(dataset.schema().fields()).map(StructField::name).toList()));
                w.write("\n");
                /* rows */
                for (Map<String, String> map : inMemory) {
                    w.write(String.join(",", map.values().stream().map(v -> {
                                if (v.contains(",")) {
                                    v = "\"" + v + "\"";
                                }
                                return v;
                            })
                            .toList()));
                    w.write("\n");
                }
                w.flush();
            }
            return ExportResourceDto.builder()
                    .filename("dataset.csv")
                    .resource(new InputStreamResource(new ByteArrayInputStream(byteArrayOutputStream.toByteArray())))
                    .build();
        } catch (IOException e) {
            log.error("Failed to transform in-memory dataset: {}", e.getMessage());
            throw new StorageUnavailableException("Failed to transform in-memory dataset: " + e.getMessage(), e);
        }
    }

    @Override
    public Dataset<Row> loadDataset(List<String> columns, String key, Boolean withHeader) throws StorageNotFoundException,
            StorageUnavailableException, MalformedException {
        final String path = "s3a://" + s3Config.getS3Bucket() + "/" + key;
        log.debug("read dataset from s3 path: {} using header: {}", path, withHeader);
        Dataset<Row> dataset;
        try {
            log.trace("spark session conf: {}", sparkSession.conf().getAllAsJava());
            dataset = sparkSession.read()
                    .option("header", withHeader)
                    .csv(path);
        } catch (Exception e) {
            if (e instanceof AnalysisException) {
                final AnalysisException exception = (AnalysisException) e;
                if (exception.getSimpleMessage().contains("PATH_NOT_FOUND")) {
                    log.error("Failed to find dataset {} in storage service: {}", key, e.getMessage());
                    throw new StorageNotFoundException("Failed to find dataset in storage service: " + e.getMessage());
                }
            }
            log.error("Failed to connect to storage service: {}", e.getMessage());
            throw new StorageUnavailableException("Failed to connect to storage service: " + e.getMessage());
        }
        if (!withHeader) {
            log.debug("no header provided: use table column names: {}", columns);
            dataset = dataset.toDF(asScalaIteratorConverter(columns.iterator())
                    .asScala()
                    .toSeq());
        }
        /* determine header order in dataset */
        if (dataset.schema().fields().length != columns.size()) {
            log.error("Failed to transform dataset: field length {} and header length arrays differ {}", dataset.schema().fields().length, columns.size());
            throw new MalformedException("Failed to transform dataset: field length and order length arrays differ");
        }
        /* reorder */
        final List<Column> columnOrder = columns.stream()
                .map(Column::new)
                .toList();
        log.trace("ordered columns: {}", columnOrder);
        return dataset.select(columnOrder.toArray(new Column[0]));
    }
}
