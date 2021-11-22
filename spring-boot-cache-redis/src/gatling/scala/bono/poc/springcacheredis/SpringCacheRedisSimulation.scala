package bono.poc.springcacheredis

import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder

import scala.concurrent.duration._
import scala.util.Random

class SpringCacheRedisSimulation extends Simulation {

  val httpProtocol: HttpProtocolBuilder = http
    .baseUrl("http://localhost:8080") // Here is the root for all relative URLs
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8") // Here are the common headers
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")

  val productCatalogSize: Int = 5000;
  val standardDeviation: Int = productCatalogSize / 15;
  val mean: Int = productCatalogSize / 2;

  val productIdFeeder: Iterator[Map[String, Long]] = Iterator.continually(
    // 70% of the numbers will be the mean +/- standardDeviation
    Map("productId" -> Math.round((Random.nextGaussian() * standardDeviation) + mean))
  )

  val scnGaussian: ScenarioBuilder =
    scenario("Get from Products API with normal distribution (gaussian) productId")
      .feed(productIdFeeder)
      .exec(http("Get without Cache")
        .get("/product-api-without-cache/${productId}")
      ).exec(http("Get with Cache")
      .get("/product-api-with-cache/${productId}"))

  setUp(
    scnGaussian.inject(
      nothingFor(1.seconds),
      constantUsersPerSec(100).during(5.minutes)
    ).protocols(httpProtocol),
  )
}
