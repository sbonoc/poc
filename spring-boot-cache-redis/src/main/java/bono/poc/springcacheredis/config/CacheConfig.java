package bono.poc.springcacheredis.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Objects;

@Slf4j
@Configuration
@EnableCaching
@ConfigurationProperties(prefix = "spring.cache.caffeine")
public class CacheConfig {

    @Getter
    @Setter
    public String spec;

    @Bean
    public CacheManager cacheManager() {
        log.debug("Caffeine specifications {}", spec);
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.from(spec));
        clearAllCaches(cacheManager);
        return cacheManager;
    }

    private void clearAllCaches(CaffeineCacheManager cacheManager) {
        log.debug("Clearing all caches");
        cacheManager.getCacheNames().forEach(name -> {
            log.debug("Clearing cache with name {}", name);
            Objects.requireNonNull(cacheManager.getCache(name)).clear();
        });
    }
}
