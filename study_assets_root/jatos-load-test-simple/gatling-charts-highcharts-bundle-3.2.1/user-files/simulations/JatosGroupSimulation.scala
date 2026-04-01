
import scala.concurrent.duration._
import scala.util.Random

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

import java.net.URLDecoder

class JatosGroupSimulation extends Simulation {

  val host = "www.example.com"
  val studyCode = "bOtlm8bSLXI"
  val componentUuid1 = "887e0913-d6ad-4620-b791-d85650ce5de6"
  val componentUuid2 = "2997d569-25a6-4ebb-9372-d87369181438"

  val httpProtocol = http
    .baseUrl(s"https://$host")
    .wsBaseUrl(s"wss://$host")
    .inferHtmlResources()
    .acceptHeader("*/*")
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .doNotTrackHeader("1")
    .userAgentHeader("Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:69.0) Gecko/20100101 Firefox/69.0")

  val header_html = Map(
    "Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
    "Upgrade-Insecure-Requests" -> "1")

  val header_json = Map(
    "Accept" -> "application/json, text/javascript, */*; q=0.01",
    "X-Requested-With" -> "XMLHttpRequest")

  val header_text = Map("Content-Type" -> "text/plain")

  val header_ajax = Map(
    "Content-Type" -> "text/plain; charset=UTF-8",
    "X-Requested-With" -> "XMLHttpRequest")


  val scn = scenario("JatosGroupSimulation")
    .exec(session => session.set("componentUuid1", componentUuid1))
    .exec(session => session.set("componentUuid2", componentUuid2))
    .exec(
      http("Start").get(s"/publix/$studyCode").headers(header_html)
        // Get study result UUID from the JATOS_ID cookie and save it in the session
        .check(headerRegex("Set-Cookie", ".*studyResultUuid=([a-zA-Z0-9-]+)&.*").saveAs("studyResultUuid"))
    )
    .exec(session => {
      val studyResultUuid = session("studyResultUuid").as[String]
      println(s"studyResultUuid: $studyResultUuid")
      session
    })
    .exec(
      http("Get init data").get("/publix/${studyResultUuid}/${componentUuid1}/initData").headers(header_json)
    )
    .exec(
      ws("Open batch channel").wsName("batchChannel").connect("/publix/${studyResultUuid}/batch/open")
    )
    .exec(
      http("Heartbeat").post("/publix/${studyResultUuid}/heartbeat").headers(header_ajax)
    )
    .exec(
      ws("Join group").wsName("groupChannel").connect("/publix/${studyResultUuid}/group/join")
    ).pause(5 seconds)
    .exec(
      http("Reassign group").get("/publix/${studyResultUuid}/group/reassign").headers(header_ajax)
    ).pause(5 seconds)
    .exec(
      http("Leave group").get("/publix/${studyResultUuid}/group/leave").headers(header_ajax)
    ).pause(5 seconds)
    .exec(
      http("Post study session data").post("/publix/${studyResultUuid}/studySessionData").headers(header_ajax).body(StringBody("""{"foo":"bar"}"""))
    )
    .exec(
      http("Post results").post("/publix/${studyResultUuid}/${componentUuid1}/resultData").headers(header_ajax).body(StringBody("""{"foo":"bar"}"""))
    )
    .exec(
      http("Finish study").get("/publix/${studyResultUuid}/end").headers(header_ajax)
    )
    .exec(ws("Close batch channel").wsName("batchChannel").close)

  setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)
  //  setUp(scn.inject(rampUsersPerSec(0.1) to (0.3) during (600 seconds))).protocols(httpProtocol)
  //  setUp(scn.inject(constantConcurrentUsers(20) during (6000 seconds))).protocols(httpProtocol)
  //  setUp(scn.inject(rampConcurrentUsers(0) to (100) during (600 seconds))).protocols(httpProtocol)
}

