package bono.poc.springcacheredis

import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder

import scala.concurrent.duration._

class SpringCacheRedisSimulation extends Simulation {

  val httpProtocol: HttpProtocolBuilder = http
    .baseUrl("http://localhost:8080") // Here is the root for all relative URLs
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8") // Here are the common headers
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")

  val scn: ScenarioBuilder = scenario("Get from Products API")
    .exec(http("Get without Cache")
      .get("/product-api-without-cache/1"))
    .exec(http("Get with Cache")
      .get("/product-api-using-caffeine-cache/1"))

  setUp(
    scn.inject(
      nothingFor(1.seconds),
      constantUsersPerSec(300).during(30.seconds)
    ).protocols(httpProtocol)
  )
}
