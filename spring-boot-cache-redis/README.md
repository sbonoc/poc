# Sring Boot using Spring Cache + Spring Data Redis (Redisson)

## Why this PoC?

Redis is a well-known in-memory data store, broadly used as cache (among other usages) but in the end is another
component in the architecture adding latency to the overall response time that is also sensitive to the load of the
system as any other one.

In this PoC I want to:

* Use Redis as central cache
* Use the application's JVM memory as local cache

The objective is to:

* Reduce network activity
* Reduce response time
* See how to configure the different cache implementation for Spring Cache

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
    with [Caffeine](https://github.com/ben-manes/caffeine) implementation

## How to run it?

Just run `./gradlew bootRun --args='--spring.profiles.active=$PROFILE'`, this task will start the docker-compose and the
Spring Boot application.

`$PROFILE` possible values are:

* `Caffeine` (Default)
* `Redisson`

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

In this PoC we use the cache in `ProductService` class, see source
code [here](https://github.com/sbonoc/poc/blob/master/spring-boot-cache-redis/src/main/java/bono/poc/springcacheredis/service/ProductService.java)
.

The class is annotated with `@CacheConfig(cacheNames = {ProductService.CACHE_NAME})`,
where `CACHE_NAME="ProductLocalCache"`, and has several methods but only one with `@Cacheable` annotation
called `getProductWithCache(String id)`.

### How-to get metrics from Spring Cache to Prometheus

1. Add the dependencies:

- `org.springframework.boot:spring-boot-starter-actuator` (Spring Boot Gradle plugin will manage the version for you)
- `micrometer-registry-prometheus` (see last
  version [here](https://mvnrepository.com/artifact/io.micrometer/micrometer-registry-prometheus))

2. Add `prometheus` to the property `management.endpoints.web.exposure.include` in your `application.properties`
   or `application.yml`. Optionally and very useful, you can add `metrics` too, so you can browse all metrics available
   via Spring Actuator's.

### Spring Cache with Caffeine

TODO

### Spring Cache with Redisson

The open-source version of Redisson does not support local cache so this option is not fulfilling our needs for this
PoC.
[Redisson.pro](https://redisson.pro/) is the paid option and it does provide local cache which is what we want.

In their Wiki you can see what are
the [different integrations with Spring](https://github.com/redisson/redisson/wiki/14.-Integration-with-frameworks), in
this case we are interested on using one of these two cache managers:

- org.redisson.spring.cache.RedissonSpringLocalCachedCacheManager
- org.redisson.spring.cache.RedissonClusteredSpringLocalCachedCacheManager

I've asked for a trial version ... as soon as I have an answer I will try and put here some analysis.

## Conclusion

TODO
