package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.config.S3Config;
import at.ac.tuwien.ifs.dbrepo.core.api.ExportResourceDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.i18n.Constants;
import at.ac.tuwien.ifs.dbrepo.service.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.SparkException;
import org.apache.spark.sql.*;
import org.apache.spark.sql.catalyst.ExtendedAnalysisException;
import org.apache.spark.sql.types.StructField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.thirdparty.org.apache.commons.codec.digest.DigestUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static scala.collection.JavaConverters.asScalaIteratorConverter;

@Slf4j
@Service
public class StorageServiceS3Impl implements StorageService {

    private final S3Config s3Config;
    private final S3Client s3Client;
    private final SparkSession sparkSession;

    private static final String S3_KEY = "s3_key";
    private static final String LOG_HEADER = "header";
    private static final String LOG_S3_BUCKET = "s3_bucket";
    private static final String HASH_SHA256 = "sha256";
    private static final String HASH_SHA1 = "sha1";
    private static final String HASH_MD5 = "md5";

    @Autowired
    public StorageServiceS3Impl(S3Config s3Config, S3Client s3Client, SparkSession sparkSession) {
        this.s3Config = s3Config;
        this.s3Client = s3Client;
        this.sparkSession = sparkSession;
    }

    @Override
    public void putObject(String key, byte[] content) throws StorageObjectExistsException {
        final long start = System.currentTimeMillis();
        try {
            final GetObjectResponse response = s3Client.getObject(GetObjectRequest.builder()
                            .key(key)
                            .bucket(s3Config.getS3Bucket())
                            .build())
                    .response();
            if (matchesAnyHash(response, content)) {
                log.debug("object with key {} already exists", key);
                throw new StorageObjectExistsException("Object already exists");
            }
        } catch (NoSuchKeyException e) {
            /* ignore */
        }
        s3Client.putObject(PutObjectRequest.builder()
                .key(key)
                .bucket(s3Config.getS3Bucket())
                .metadata(new HashMap<>() {{
                    put("sha256", DigestUtils.sha256Hex(content));
                    put("sha1", DigestUtils.sha1Hex(content));
                    put("md5", DigestUtils.md5Hex(content));
                }})
                .build(), RequestBody.fromBytes(content));
        log.atDebug()
                .setMessage("put object in bucket with key " + key + " in " + TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - start) + "ms")
                .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                .addKeyValue(Constants.ACTION, "s3_put_object")
                .log();
    }

    public boolean matchesAnyHash(GetObjectResponse response, byte[] content) {
        if (response == null) {
            log.trace("response is null");
            return false;
        }
        if (!response.hasMetadata() || response.metadata().isEmpty()) {
            log.trace("response has no metadata");
            return false;
        }
        if (response.metadata().containsKey(HASH_MD5) && response.metadata().get(HASH_MD5).equals(DigestUtils.md5Hex(content))) {
            log.trace("matches md5 hash: {}", response.metadata().get(HASH_MD5));
            return true;
        }
        if (response.metadata().containsKey(HASH_SHA1) && response.metadata().get(HASH_SHA1).equals(DigestUtils.sha1Hex(content))) {
            log.trace("matches sha1 hash: {}", response.metadata().get(HASH_SHA1));
            return true;
        }
        if (response.metadata().containsKey(HASH_SHA256) && response.metadata().get(HASH_SHA256).equals(DigestUtils.sha256Hex(content))) {
            log.trace("matches sha256 hash: {}", response.metadata().get(HASH_SHA256));
            return true;
        }
        return false;
    }

    @Override
    public InputStream getObject(String bucket, String key) throws StorageNotFoundException,
            StorageUnavailableException {
        try {
            final long start = System.currentTimeMillis();
            final InputStream object = s3Client.getObject(GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
            log.atDebug()
                    .setMessage("get object from bucket with key " + key + " in " + TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - start) + "ms")
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "s3_get_object")
                    .log();
            return object;
        } catch (NoSuchKeyException | NoSuchBucketException e) {
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
    public void deleteObject(String key) {
        final long start = System.currentTimeMillis();
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(s3Config.getS3Bucket())
                .key(key)
                .build());
        log.atDebug()
                .setMessage("delete object from bucket with key " + key + " in " + TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - start) + "ms")
                .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                .addKeyValue(Constants.ACTION, "s3_delete_object")
                .log();
    }

    @Override
    public ExportResourceDto transformDataset(Dataset<Row> dataset) throws StorageUnavailableException {
        log.trace("transforming dataset with {} column(s)", dataset.columns().length);
        long start = System.currentTimeMillis();
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
        log.atDebug()
                .setMessage("transformed dataset with " + inMemory.size() + " row(s) in " + TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - start) + "ms")
                .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                .addKeyValue(Constants.ACTION, "dataset_transform")
                .log();
        start = System.currentTimeMillis();
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
            final InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));
            log.atDebug()
                    .setMessage("transformed dataset to input stream resource in " + TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - start) + "ms")
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "dataset_export")
                    .log();
            return ExportResourceDto.builder()
                    .filename("dataset.csv")
                    .resource(resource)
                    .build();
        } catch (IOException e) {
            log.error("Failed to transform in-memory dataset: {}", e.getMessage());
            throw new StorageUnavailableException("Failed to transform in-memory dataset: " + e.getMessage(), e);
        }
    }

    @Override
    public Dataset<Row> loadDataset(List<String> columns, String key, String delimiter, Boolean withHeader)
            throws StorageNotFoundException, StorageUnavailableException, MalformedException, TableMalformedException {
        final String path = "s3a://" + s3Config.getS3Bucket() + "/" + key;
        log.atDebug()
                .setMessage("read dataset " + key + " using header: " + withHeader)
                .addKeyValue(S3_KEY, key)
                .addKeyValue(LOG_S3_BUCKET, s3Config.getS3Bucket())
                .addKeyValue(LOG_HEADER, withHeader)
                .log();
        Dataset<Row> dataset;
        try {
            dataset = sparkSession.read()
                    .option("delimiter", delimiter)
                    .option(LOG_HEADER, withHeader)
                    .csv(path)
                    .toDF(columns.toArray(new String[0]));
        } catch (Exception e) {
            if (e instanceof AnalysisException) {
                final AnalysisException exception = (AnalysisException) e;
                if (exception.getSimpleMessage().contains("PATH_NOT_FOUND")) {
                    log.atError()
                            .setMessage("Failed to find dataset " + key + " in storage service")
                            .addKeyValue(S3_KEY, key)
                            .setCause(e)
                            .log();
                    throw new StorageNotFoundException("Failed to find dataset in storage service: " + e.getMessage());
                }
                if (exception.getSimpleMessage().contains("UNRESOLVED_COLUMN")) {
                    log.atError()
                            .setMessage("Failed to resolve column from dataset in database")
                            .addKeyValue(S3_KEY, key)
                            .setCause(e)
                            .log();
                    throw new TableMalformedException("Failed to resolve column from dataset in database: " + e.getMessage());
                }
            } else if (e instanceof SparkException) {
                final SparkException exception = (SparkException) e;
                if (exception.getMessage().contains("FAILED_READ_FILE")) {
                    log.atError()
                            .setMessage("Failed to read dataset " + key + ": " + e.getMessage())
                            .addKeyValue(S3_KEY, key)
                            .setCause(e)
                            .log();
                    throw new StorageUnavailableException("Failed to read dataset " + key + ": " + e.getMessage());
                }
            } else if (e instanceof IllegalArgumentException) {
                log.atError()
                        .setMessage("Failed to map columns: " + e.getMessage())
                        .addKeyValue(S3_KEY, key)
                        .setCause(e)
                        .log();
                throw new MalformedException("Failed to map columns: " + e.getMessage());
            }
            log.atError()
                    .setMessage("Failed to connect to storage service: " + e.getMessage())
                    .addKeyValue(S3_KEY, key)
                    .setCause(e)
                    .log();
            throw new StorageUnavailableException("Failed to connect to storage service: " + e.getMessage());
        }
        if (!withHeader) {
            log.atDebug()
                    .setMessage("no header provided: use table column names")
                    .addKeyValue("columns", columns)
                    .log();
            try {
                dataset = dataset.toDF(asScalaIteratorConverter(columns.iterator())
                        .asScala()
                        .toSeq());
            } catch (IllegalArgumentException e) {
                log.error("Failed to map column to scala sequence: {}", e.getMessage());
                throw new MalformedException("Failed to map column to scala sequence: " + e.getMessage(), e);
            }
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
        try {
            return dataset.select(columnOrder.toArray(new Column[0]));
        } catch (Exception e) {
            if (e instanceof ExtendedAnalysisException exception) {
                log.atError()
                        .setMessage("Failed to resolve column from dataset in database")
                        .addKeyValue(S3_KEY, key)
                        .setCause(e)
                        .log();
                throw new TableMalformedException("Failed to resolve column from dataset in database: " + exception.getSimpleMessage());
            }
            log.atError()
                    .setMessage("Failed to select columns from dataset")
                    .addKeyValue(S3_KEY, key)
                    .setCause(e)
                    .log();
            throw new MalformedException("Failed to select columns from dataset: " + e.getMessage());
        }
    }
}
