package at.tuwien.config;

import at.tuwien.api.PrivilegedObjectDto;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig<K, T extends PrivilegedObjectDto> {

    @Value("${dbrepo.credentialCacheTimeout}")
    private Long credentialCacheTimeout;

    @Bean
    public Cache<K, T> cache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(credentialCacheTimeout, TimeUnit.SECONDS)
                .build();
    }

}
