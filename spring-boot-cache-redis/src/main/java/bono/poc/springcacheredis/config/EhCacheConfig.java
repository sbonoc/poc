package bono.poc.springcacheredis.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Slf4j
@Configuration
@EnableCaching
@Profile("Ehcache")
public class EhCacheConfig extends BaseCacheConfig {

}
