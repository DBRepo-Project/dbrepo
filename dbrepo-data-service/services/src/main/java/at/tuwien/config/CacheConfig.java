package at.tuwien.config;

import at.ac.tuwien.ifs.dbrepo.core.api.container.ContainerDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseAccessDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.ViewDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableStatisticDto;
import at.ac.tuwien.ifs.dbrepo.core.api.keycloak.TokenDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserDto;
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
    private Integer credentialCacheTimeout;

    @Bean
    public Cache<UUID, UserDto> userCache() {
        return new ExpiryCache<UUID, UserDto>().build();
    }

    @Bean
    public Cache<UUID, ViewDto> viewCache() {
        return new ExpiryCache<UUID, ViewDto>().build();
    }

    @Bean
    public Cache<UUID, DatabaseAccessDto> accessCache() {
        return new ExpiryCache<UUID, DatabaseAccessDto>().build();
    }

    @Bean
    public Cache<UUID, TableDto> tableCache() {
        return new ExpiryCache<UUID, TableDto>().build();
    }

    @Bean
    public Cache<UUID, DatabaseDto> databaseCache() {
        return new ExpiryCache<UUID, DatabaseDto>().build();
    }

    @Bean
    public Cache<UUID, ContainerDto> containerCache() {
        return new ExpiryCache<UUID, ContainerDto>().build();
    }

    @Bean
    public Cache<UUID, TableStatisticDto> statisticCache() {
        return new ExpiryCache<UUID, TableStatisticDto>().build();
    }

    @Bean
    public Cache<String, TokenDto> tokenCache() {
        return new ExpiryCache<String, TokenDto>().build();
    }

    class ExpiryCache<K, T> {

        Cache<K, T> build() {
            return Caffeine.newBuilder()
                    .expireAfterWrite(credentialCacheTimeout, TimeUnit.SECONDS)
                    .build();
        }

    }

}
