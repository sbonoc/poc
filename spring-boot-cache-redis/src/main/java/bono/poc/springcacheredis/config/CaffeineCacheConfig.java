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
import org.springframework.context.annotation.Profile;

@Slf4j
@Configuration
@EnableCaching
@Profile("Caffeine")
@ConfigurationProperties(prefix = "spring.cache.caffeine")
public class CaffeineCacheConfig extends BaseCacheConfig {

    @Getter
    @Setter
    String spec;

    @Bean
    CacheManager cacheManager() {
        log.info("==== SETTING UP CAFFEINE CACHE ====");
        log.debug("Caffeine specifications {}", spec);
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.from(spec));
        super.clearAllCaches(cacheManager);
        return cacheManager;
    }

}
