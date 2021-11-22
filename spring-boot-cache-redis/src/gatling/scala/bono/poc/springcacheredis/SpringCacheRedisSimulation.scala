package bono.poc.springcacheredis

import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder

import scala.concurrent.duration._
import scala.util.Random

class SpringCacheRedisSimulation extends Simulation {

  val httpProtocol: HttpProtocolBuilder = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")

  val productCatalogSize: Int = 5000
  val standardDeviation: Int = productCatalogSize / 15
  val mean: Int = productCatalogSize / 2

  val productIdFeeder: Iterator[Map[String, Long]] = Iterator.continually(
    // 70% of the numbers will be the mean +/- standardDeviation
    Map("productId" -> Math.round((Random.nextGaussian() * standardDeviation) + mean))
  )

  val scnConstant: ScenarioBuilder =
    scenario(s"Get productId=$mean with & without cache")
      .exec(http("Constant no-cache")
        .get(s"/product-api-without-cache/$mean")
      ).exec(http("Constant cache")
      .get(s"/product-api-with-cache/$mean"))

  val scnClearAllCaches: ScenarioBuilder =
    scenario("Clear all caches using Spring Actuator")
      .exec(http("Delete all caches")
        .delete("/actuator/caches"))

  val scnGaussian: ScenarioBuilder =
    scenario("Get productId=(normal distribution [gaussian]) with & without cache")
      .feed(productIdFeeder)
      .exec(http("Gaussian no-cache")
        .get("/product-api-without-cache/${productId}")
      ).exec(http("Gaussian cache")
      .get("/product-api-with-cache/${productId}"))

  val requestPerSecondMax: Double = 150d

  setUp(
    scnConstant.inject(
      constantUsersPerSec(requestPerSecondMax).during(30.seconds),
    ).protocols(httpProtocol)
      .andThen(
        scnClearAllCaches.inject(
          atOnceUsers(1)
        ).protocols(httpProtocol)
          .andThen(
            scnGaussian.inject(
              nothingFor(30.seconds),
              rampUsersPerSec(requestPerSecondMax / 10).to(requestPerSecondMax).during(1.minutes),
              constantUsersPerSec(requestPerSecondMax).during(5.minutes)
            ).protocols(httpProtocol)
          )
      )
  )

}
