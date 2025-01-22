package at.tuwien.config;

import at.tuwien.api.container.ContainerDto;
import at.tuwien.api.database.DatabaseAccessDto;
import at.tuwien.api.database.DatabaseDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.user.UserDto;
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
    public Cache<UUID, UserDto> userCache() {
        return new ExpiryCache<UUID, UserDto>().build();
    }

    @Bean
    public Cache<Long, ViewDto> viewCache() {
        return new ExpiryCache<Long, ViewDto>().build();
    }

    @Bean
    public Cache<Long, DatabaseAccessDto> accessCache() {
        return new ExpiryCache<Long, DatabaseAccessDto>().build();
    }

    @Bean
    public Cache<Long, TableDto> tableCache() {
        return new ExpiryCache<Long, TableDto>().build();
    }

    @Bean
    public Cache<Long, DatabaseDto> databaseCache() {
        return new ExpiryCache<Long, DatabaseDto>().build();
    }

    @Bean
    public Cache<Long, ContainerDto> containerCache() {
        return new ExpiryCache<Long, ContainerDto>().build();
    }

    class ExpiryCache<K, T> {

        Cache<K, T> build() {
            return Caffeine.newBuilder()
                    .expireAfterWrite(credentialCacheTimeout, TimeUnit.SECONDS)
                    .build();
        }

    }

}
