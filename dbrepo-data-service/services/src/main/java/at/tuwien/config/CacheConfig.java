package at.tuwien.config;

import at.tuwien.api.container.internal.PrivilegedContainerDto;
import at.tuwien.api.database.DatabaseAccessDto;
import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.database.internal.PrivilegedViewDto;
import at.tuwien.api.database.table.internal.PrivilegedTableDto;
import at.tuwien.api.user.internal.PrivilegedUserDto;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    @Value("${dbrepo.credentialCacheTimeout}")
    private Long credentialCacheTimeout;

    @Bean
    public Cache<UUID, PrivilegedUserDto> userCache() {
        return new ExpiryCache<UUID, PrivilegedUserDto>().build();
    }

    @Bean
    public Cache<Long, PrivilegedViewDto> viewCache() {
        return new ExpiryCache<Long, PrivilegedViewDto>().build();
    }

    @Bean
    public Cache<Long, DatabaseAccessDto> accessCache() {
        return new ExpiryCache<Long, DatabaseAccessDto>().build();
    }

    @Bean
    public Cache<Long, PrivilegedTableDto> tableCache() {
        return new ExpiryCache<Long, PrivilegedTableDto>().build();
    }

    @Bean
    public Cache<Long, PrivilegedDatabaseDto> databaseCache() {
        return new ExpiryCache<Long, PrivilegedDatabaseDto>().build();
    }

    @Bean
    public Cache<Long, PrivilegedContainerDto> containerCache() {
        return new ExpiryCache<Long, PrivilegedContainerDto>().build();
    }

    class ExpiryCache<K, T> {

        Cache<K, T> build() {
            return Caffeine.newBuilder()
                    .expireAfterWrite(credentialCacheTimeout, TimeUnit.SECONDS)
                    .build();
        }

    }

}
