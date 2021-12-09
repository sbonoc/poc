package bono.poc.springcacheredis.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.geode.cache.GemFireCache;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.gemfire.cache.GemfireCacheManager;

@Slf4j
@Configuration
@EnableCaching
@Profile("Geode")
public class GeodeConfig extends BaseCacheConfig {

    @Bean
    GemfireCacheManager cacheManager(GemFireCache cache) {
        log.info("==== SETTING UP GEODE (GEMFIRE) CACHE ====");
        GemfireCacheManager cacheManager = new GemfireCacheManager();
        cacheManager.setCache(cache);
        super.clearAllCaches(cacheManager);
        return cacheManager;
    }


}
