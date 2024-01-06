package at.tuwien.mapper;

import io.minio.GetObjectArgs;
import org.mapstruct.Mapper;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.util.Collections;

@Mapper(componentModel = "spring")
public interface S3Mapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(S3Mapper.class);

    default GetObjectArgs s3ArgsToObjectArgs(String bucketName, String key) {
        return s3ArgsToObjectArgs(bucketName, key, null);
    }

    default GetObjectArgs s3ArgsToObjectArgs(String bucketName, String key, Long length) {
        final GetObjectArgs.Builder builder = GetObjectArgs.builder()
                .bucket(bucketName)
                .object(key);
        if (length != null) {
            builder.length(length);
        }
        return builder.build();
    }
}
