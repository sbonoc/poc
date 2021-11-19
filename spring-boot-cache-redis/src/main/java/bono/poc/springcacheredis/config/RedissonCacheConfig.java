package bono.poc.springcacheredis.config;

import bono.poc.springcacheredis.service.ProductService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.redisson.spring.cache.CacheConfig;
import org.redisson.spring.cache.RedissonSpringCacheManager;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@EnableCaching
@Profile("Redisson")
@ConfigurationProperties(prefix = "redisson.cache")
public class RedissonCacheConfig extends BaseCacheConfig {

    @Getter
    @Setter
    CacheConfig config;

    @Bean
    CacheManager cacheManager(RedissonClient redissonClient) {
        log.info("==== SETTING UP REDISSON CACHE ====");
        Map<String, CacheConfig> config = new HashMap<>();
        config.put(ProductService.CACHE_NAME, this.config);
        log.debug(
                "Configuring RedissonSpringCacheManager for {} with ttl={}, maxIdleTime={}, maxSize={}",
                ProductService.CACHE_NAME, this.config.getTTL(), this.config.getMaxIdleTime(), this.config.getMaxSize()
        );
        RedissonSpringCacheManager cacheManager = new RedissonSpringCacheManager(redissonClient, config);
        super.clearAllCaches(cacheManager);
        return cacheManager;
    }

}
