
import scala.concurrent.duration._
import scala.util.Random

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

import java.net.URLDecoder

class JatosFileUploadSimulation extends Simulation {

  val httpProtocol = http
//    .baseUrl("https://cortex.jatos.org")
//    .wsBaseUrl("wss://cortex.jatos.org")
    .baseUrl("http://localhost:9000")
    .wsBaseUrl("ws://localhost:9000")
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



  val scn = scenario("JatosFileUploadSimulation")
    .exec(session => session.set("componentUuid1", "50e32e16-1831-495b-9826-f05e1eeccc87"))
    .exec(session => session.set("componentUuid2", "cf187900-9e44-44b0-9e3d-779ba80ceaed"))

// ### 1. Component ###
  .exec(
    http("Start").get("/publix/6BEbRemybjS").check(bodyString.saveAs("BODY")).headers(header_html)
  ).exec(getCookieValue(CookieKey("JATOS_IDS_0"))
  ).exec(session => {
    val cookie = session("JATOS_IDS_0").as[String]
    val cookieParas = parseUrlParameters(cookie)
    val studyResultUuid = cookieParas("studyResultUuid")
    println(s"JATOS_IDS_0: $studyResultUuid")
    session.set("studyResultUuid", studyResultUuid)
  }).exec(
    http("Get init data").get("/publix/${studyResultUuid}/${componentUuid1}/initData").headers(header_json)
  ).exec(
    ws("Open batch channel").connect("/publix/${studyResultUuid}/batch/open")
  ).exec(
    http("Heartbeat").post("/publix/${studyResultUuid}/heartbeat").headers(header_text)
  ).pause(5 seconds).exec(
//    http("File upload").post("/publix/${studyResultUuid}/${componentUuid1}/files/1577217737269.video").bodyPart(RawFileBodyPart("file", "1577217737269.video").fileName("1577217737269.video")).asMultipartForm
    http("File upload").post("/publix/${studyResultUuid}/${componentUuid1}/files/ngrok").bodyPart(RawFileBodyPart("file", "ngrok").fileName("ngrok")).asMultipartForm
  ).exec(
    http("Post study session data").post("/publix/${studyResultUuid}/studySessionData").headers(header_ajax).body(StringBody("""{"foo":"bar"}"""))
  ).exec(
    http("Post result").post("/publix/${studyResultUuid}/${componentUuid1}/resultData").headers(header_ajax).body(StringBody(Random.alphanumeric.take(100000).mkString("")))
  ).exec(ws("Close batch channel").close)

// ### 2. Component ###
  .exec(
    http("Next component").get("/publix/${studyResultUuid}/${componentUuid2}/start&message=load%20test%20message%20%C2%A7%24%25%26").headers(header_html)
  ).exec(
    http("Get init data").get("/publix/${studyResultUuid}/${componentUuid2}/initData").headers(header_json)
  ).exec(
    ws("Open batch channel").connect("/publix/${studyResultUuid}/batch/open")
  ).exec(
    http("Heartbeat").post("/publix/${studyResultUuid}/heartbeat").headers(header_text)
  ).exec(
    http("Download file").get("/publix/${studyResultUuid}/files/ngrok").headers(header_ajax)
//    http("Download file").get("/publix/${studyResultUuid}/files/1577217737269.video").headers(header_ajax)
  ).pause(5 seconds).exec(
    http("Finish study").get("/publix/${studyResultUuid}/end").headers(header_ajax)
  ).exec(ws("Close batch channel").close)

  def parseUrlParameters(url: String) = {
    url.split("&").map(v => {
      val m = v.split("=", 2).map(s => URLDecoder.decode(s, "UTF-8"))
      m(0) -> m(1)
    }).toMap
  }

  setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)
//  setUp(scn.inject(rampUsersPerSec(0.2) to (0.2) during (600 seconds))).protocols(httpProtocol)
//  setUp(scn.inject(constantConcurrentUsers(4) during (6000 seconds))).protocols(httpProtocol)
//  setUp(scn.inject(rampConcurrentUsers(1) to (10) during (600 seconds))).protocols(httpProtocol)
}

