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
* See how to configure the different caches

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
  - [Spring Cache](https://docs.spring.io/spring-framework/docs/3.2.x/spring-framework-reference/html/cache.html)
    with [Caffeine](https://github.com/ben-manes/caffeine) implementation

## How to run it?

Just run `./gradlew bootRun`, this task will start the docker-compose and the Spring Boot application.

* Spring Boot application: `http://localhost:8080`
* Grafana: `http://localhost:3000` with user `admin` and password `admin`
* Prometheus: `http://localhost:9090`
* Redis Exporter: `http://localhost:9121`
* Redis: `redis://localhost:6379`

After `bootRun` task you can run the Gatling load tests with `./gradlew gatlingRun` in the output you can find the path
to the HTML report.

## Step-by-step analysis

TODO

## Conclusion

TODO
