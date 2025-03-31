package at.ac.tuwien.ifs.dbrepo.config;

import at.ac.tuwien.ifs.dbrepo.core.api.keycloak.TokenDto;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    @Value("${dbrepo.credentialCacheTimeout}")
    private Integer credentialCacheTimeout;

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
