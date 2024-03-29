# Sring Boot using Spring Cache + Spring Data Redis (Redisson)

## Why this PoC?

Redis is a well-known in-memory data store, broadly used as cache (among other usages) but in the end is another
component in the architecture adding latency to the overall response time that is also sensitive to the load of the
system as any other one.

A better approach would be to cache within the application's memory and retrieved it from Redis only when is missing.
Getting the data from the memory directly will be a way much faster than going to Redis throught the network.

In this PoC I want to check what are the different Spring Cache implementations that I have found so far so that I can:

* Use Redis as central cache
* Use the application's JVM memory as local cache

The objective is to:

* Reduce network activity
* Reduce response time
* See how to configure the different cache implementation for Spring Cache.

## What's inside?

- Docker Compose for:
  - [Redis](https://hub.docker.com/_/redis)
  - [Redis exporter](https://hub.docker.com/r/oliver006/redis_exporter)
  - [Prometheus](https://hub.docker.com/r/prom/prometheus)
  - [Grafana](https://hub.docker.com/r/grafana/grafana)
    with [Dashboard for Redis](https://grafana.com/grafana/dashboards/14091) pre-configured
- [Gatling](https://gatling.io/) for load testing
- Spring Boot application using:
  - [Lombok](https://projectlombok.org/) for obvious reasons
  - [MapStruct](https://mapstruct.org/) to map DTOs/Models/Entities between different application layers easily
  - [Spring Data Redis](https://docs.spring.io/spring-data/data-redis/docs/current/reference/html/#reference)
    with [Redisson Spring Boot Starter](https://github.com/redisson/redisson/tree/master/redisson-spring-boot-starter)
  - [Spring Cache](https://docs.spring.io/spring-framework/docs/current/reference/html/integration.html#cache)
    that we will configure with different implementations in this PoC.

## How to run it?

Just run `./gradlew bootRun --args='--spring.profiles.active=$PROFILE'`, this task will start the docker-compose and the
Spring Boot application.

`$PROFILE` possible values are:
* `JdkConcurrentMap`
* `Caffeine` (Default)
* `EhCache`
* `Geode`

Here you have the URLs for each component:

* Spring Boot application: [`http://localhost:8080`](http://localhost:8080)
* Grafana: [`http://localhost:3000`](http://localhost:3000) with user `admin` and password `admin`
* Prometheus: [`http://localhost:9090`](http://localhost:9090)
* Redis Exporter: [`http://localhost:9121`](http://localhost:9121)
* Redis: `redis://localhost:6379`

After `bootRun` task you can run the Gatling load tests with `./gradlew gatlingRun` in the output you can find the path
to the HTML report.

## Step-by-step analysis

Our intention is to use Spring Cache as the abstraction layer for using and managing the cache and then later we see the
different implementations we can plug.

See original documentation from
Spring [here](https://docs.spring.io/spring-framework/docs/current/reference/html/integration.html#cache).

### How-to configure Spring Cache (the abstraction layer)

Just add the dependency `spring-boot-starter-cache` to your project. If you are using Gradle with the Spring Boot plugin
... it will take care of the version automatically, otherwise you can see last
version [here](https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-starter-cache).

Optionally, you can add the dependency `org.springframework.boot:spring-boot-starter-actuator` and expose the cache
endpoint to manage all caches via Spring Actuators' API which is very useful, see official documentation
[here](https://docs.spring.io/spring-boot/docs/current/actuator-api/htmlsingle/#caches). If you want to activate it, you
have to add `management.endpoints.web.exposure.include=caches` in your
`application.properties` or `application.yml`.

Once you have the dependencies you can start using the
[Annotations for caching](https://docs.spring.io/spring-framework/docs/current/reference/html/integration.html#cache-annotations)
but this will not work unless you configure the `CacheManager` bean with the desired implementation (explained in
further sections).

In this PoC I use the Spring Cache annotations in `ProductService` class, see source
code [here](https://github.com/sbonoc/poc/blob/master/spring-boot-cache-redis/src/main/java/bono/poc/springcacheredis/service/ProductService.java)
.

The class is annotated with `@CacheConfig(cacheNames = {ProductService.CACHE_NAME})`,
where `CACHE_NAME="ProductLocalCache"`, and has several methods but only one with `@Cacheable` annotation
called `getProductWithCache(String id)`.

### How-to expose Spring Cache metrics for Prometheus

1. Add the dependencies:

- `org.springframework.boot:spring-boot-starter-actuator` (Spring Boot Gradle plugin will manage the version for you)
- `micrometer-registry-prometheus` (see last
  version [here](https://mvnrepository.com/artifact/io.micrometer/micrometer-registry-prometheus))

2. Add `prometheus` to the property `management.endpoints.web.exposure.include` in your `application.properties`
   or `application.yml`. Optionally and very useful, you can add `metrics` too, so you can browse all metrics available
   via Spring Actuator's endpoints.

Once you've followed the points above you can see the metrics:

- [cache.eviction.weight](http://localhost:8080/actuator/metrics/cache.eviction.weight)
- [cache.evictions](http://localhost:8080/actuator/metrics/cache.evictions)
- [cache.gets](http://localhost:8080/actuator/metrics/cache.gets)
- [cache.puts](http://localhost:8080/actuator/metrics/cache.puts)
- [cache.size](http://localhost:8080/actuator/metrics/cache.size)

See official Spring Boot Actuator's documentation
[here](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.metrics.supported.cache)

### JDK ConcurrentMapCache implementation

This is the simplest implementation, is fast, no need for extra libraries, works but lacks useful capabilities to
consider it production-ready, like monitoring, eviction contracts, etc.

To use it is as simple as configure the `@Bean CacheManager` to use `ConcurrentMapCacheManager` implementation.
See `JdkConcurrentMapCacheConfig`
class [here](https://github.com/sbonoc/poc/blob/master/spring-boot-cache-redis/src/main/java/bono/poc/springcacheredis/config/JdkConcurrentMapCacheConfig.java)

#### Performance

![Method invocations performance](./readme-resources/method-jdkconcurrentmapcache.png)
![Gatling result for JDK ConcurrentMapCache](./readme-resources/gatling-jdkconcurrentmapcache.png)

### Caffeine implementation

Caffeine is the successor of Guava in Spring Cache, see official
documentation [here](https://github.com/ben-manes/caffeine/wiki).

1. Add the dependency `com.github.ben-manes.caffeine:caffeine` (Spring Boot Gradle plugin will manage the version for
   you)
2. Add desired configuration in the property `spring.cache.caffeine.spec` of your `application.properties`
   or `application.yml` file. See all possible values [here](https://github.com/ben-manes/caffeine/wiki/Specification).
3. If you want to have metrics, add `recordStats` to `spring.cache.caffeine.spec`.
4. Configure the `@Bean CacheManager` to use `CaffeineCacheManager` implementation. See `CaffeineCacheConfig` class
   [here](https://github.com/sbonoc/poc/blob/master/spring-boot-cache-redis/src/main/java/bono/poc/springcacheredis/config/CaffeineCacheConfig.java)
   .

#### Performance

![Method invocations performance](./readme-resources/method-caffeine.png)
![Gatling result for Caffeine](./readme-resources/gatling-caffeine.png)

### Redisson implementation

The open-source version of Redisson does not support local cache so this option is not fulfilling our needs for this
PoC.

[Redisson.pro](https://redisson.pro/) is the paid option and it does provide local cache which is what we want.

In their Wiki you can see what are
the [different integrations with Spring](https://github.com/redisson/redisson/wiki/14.-Integration-with-frameworks), in
this case, for what we want to achieve in this PoC, we would use one of these two cache managers:

- org.redisson.spring.cache.RedissonSpringLocalCachedCacheManager
- org.redisson.spring.cache.RedissonClusteredSpringLocalCachedCacheManager

### Ehcache implementation

Ehcache is probably the oldest and most used cache for Java which is following the `javax.cache` API as in the JSR-107
specification, see their [official website](https://www.ehcache.org/) for further information.

1. Add the dependency `org.ehcache:ehcache` (again, Spring Boot Gradle plugin will manage the version for you)
2. Add the dependency `com.sun.xml.bind:jaxb-ri` the latest `2.x.x` version ... this is needed because ehcache's config
   file is in XML and it uses the old `javax.xml.bind` which was deprecated in Java 9 and removed in Java 11, see
   [Java 11 release notes](https://www.oracle.com/java/technologies/javase/11-relnote-issues.html#JDK-8190378).
3. Create the file `ehcache.xml` in the `src/main/resources` folder and put your configuration following the official
   documentation [here](https://www.ehcache.org/documentation/3.9/xml.html)
4. If you want to have metrics add the setting `enable-statistics="true"` in the XML for the JSR-107 extension, see
   [ehcache.xml](src/main/resources/ehcache.xml).
5. Add the properties below to your `application.properties` or `application.yml`:

```
spring.cache.jcache.config=classpath:ehcache.xml
spring.cache.jcache.provider=org.ehcache.jsr107.EhcacheCachingProvider
```

#### Performance

![Method invocations performance](./readme-resources/method-ehcache.png)
![Gatling result for EhCache](./readme-resources/gatling-ehcache.png)

### Apache Geode (Gemfire) implementation

[Apache Geode](https://geode.apache.org/) is a very robust with low latency and high concurrency data management
platform.

1. Add the
   dependency `org.springframework.geode:spring-geode-starter:${latest_version}` ([check version in maven repo](https://mvnrepository.com/artifact/org.springframework.geode/spring-geode-starter))
   .
2. Add the
   dependency `org.springframework.geode:spring-geode-starter-actuator:${latest_version}` ([check version in maven repo](https://mvnrepository.com/artifact/org.springframework.geode/spring-geode-starter-actuator))
3. Create the file `cache.xml` in the `src/main/resources` folder and put your configuration following the official
   documentation [here](https://geode.apache.org/docs/guide/114/reference/topics/chapter_overview_cache_xml.html)
4. If you want to have metrics add the properties below to your `application.properties` or `application.yml` but there
   is one problem, they will not be exported to prometheus endpoints, they are only available via `/actuator/health`
   endpoint:

```
management.endpoint.health.show-details=always
spring.data.gemfire.stats.enable-time-statistics=true
```

#### Performance

![Method invocations performance](./readme-resources/method-geode.png)
![Gatling result for Geode](./readme-resources/gatling-geode.png)

## Conclusion

In order to make a decision we need some criteria.

Minimum requirements:

* Can be configured, so that at least we can control what's the size of the cache and the expiration.
* Can be manage via Spring Actuator, so that we can make operations easily and in a "Spring standard" way.
* Has metrics that can be exported to Prometheus, so we can monitor the cache.

| Implementation   | Configurable? | Can be managed via Actuator? | Has Prometheus metrics? | Does fulfill min requirements? |
|------------------|---------------|------------------------------|-------------------------|--------------------------------|
| JdkConcurrentMap | No            | No                           | No                      | No                             |
| Caffeine         | Yes           | Yes                          | Yes                     | Yes                            |
| EhCache          | Yes           | Yes                          | Yes                     | Yes                            |
| Geode            | Yes           | Yes                          | No                      | No                             |

JdkConcurrentMap & Geode are out because they don't fulfill the minimum requirements I consider.

Now, from the ones left, which one is the fastest?

| Implementation | 95th pct Gaussian Cache | 99th pct Gaussian Cache | Min getProductByIdWithCache latency | Max getProductByIdWithCache latency | Avg getProductByIdWithCache latency |
|----------------|-------------------------|-------------------------|-------------------------------------|-------------------------------------|-------------------------------------|
| Caffeine       | 5 ms                    | 8 ms                    | 52.3 µs                             | 2.05 ms                             | 1.49 ms                             |
| EhCache        | 6 ms                    | 13 ms                   | 35.3 µs                             | 3.55 ms                             | 2.21 ms                             |

Taking a look to the 95th & 99th percentile of the API and the average latency for the method `getProductByIdWithCache`,
Caffeine was faster so ... Caffeine is the winner!

However, I would not rely a lot on these numbers. To be 100% sure, I would repeat the test in the target infrastructure
with the real application where we want to use this cache.
