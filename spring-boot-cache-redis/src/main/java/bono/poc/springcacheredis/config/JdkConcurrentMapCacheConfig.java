package bono.poc.springcacheredis.config;

import bono.poc.springcacheredis.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Slf4j
@Configuration
@EnableCaching
@Profile("JdkConcurrentMap")
public class JdkConcurrentMapCacheConfig extends BaseCacheConfig {

    @Bean
    public CacheManager cacheManager() {
        log.info("==== SETTING UP JDK CONCURRENT MAP CACHE ====");
        CacheManager cacheManager = new ConcurrentMapCacheManager(ProductService.CACHE_NAME);
        super.clearAllCaches(cacheManager);
        return cacheManager;
    }

}
