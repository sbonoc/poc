package bono.poc.springcacheredis.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;

import java.util.Objects;

@Slf4j
public class BaseCacheConfig {

    protected void clearAllCaches(CacheManager cacheManager) {
        log.debug("Clearing all caches");
        cacheManager.getCacheNames().forEach(name -> {
            log.debug("Clearing cache with name {}", name);
            Objects.requireNonNull(cacheManager.getCache(name)).clear();
        });
    }

}
