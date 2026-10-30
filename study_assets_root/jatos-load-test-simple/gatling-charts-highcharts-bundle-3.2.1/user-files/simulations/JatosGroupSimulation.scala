
import scala.concurrent.duration._
import scala.util.Random

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

import java.net.URLDecoder

class JatosGroupSimulation extends Simulation with CheckedJatos {


  val httpProtocol = http
    .baseUrl(baseUrl)
    .wsBaseUrl(wsBaseUrl)
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
    .exec(identify)
    .exec(session => session.set("componentUuid1", componentUuid1))
    .exec(session => session.set("componentUuid2", componentUuid2))
    .exec(
      http("Start").get(s"/publix/$studyCode").headers(header_html).check(status.is(200))
        // Get study result UUID from the JATOS_ID cookie and save it in the session
        .check(headerRegex("Set-Cookie", ".*studyResultUuid=([a-zA-Z0-9-]+)&.*").saveAs("studyResultUuid"))
    ).exec(failFast)
    .exec(session => {
      val studyResultUuid = session("studyResultUuid").as[String]
      println(s"studyResultUuid: $studyResultUuid")
      session
    })
    .exec(
      http("Get init data")
        .get("/publix/${studyResultUuid}/${componentUuid1}/initData")
        .headers(header_json)
        .check(status.is(200))
        .check(
          jsonPath("$.studyCode").is(studyCode),
          jsonPath("$.studyProperties.uuid").is(expectedStudyUuid),
          jsonPath("$.componentProperties.uuid").is(componentUuid1),
          jsonPath("$.componentList[*].uuid").findAll.transform(_.contains(componentUuid1)).is(true),
          jsonPath("$.componentList[*].uuid").findAll.transform(_.contains(componentUuid2)).is(true))
    ).exec(failFast)
    .exec(
      ws("Open batch channel").wsName("batchChannel").connect("/publix/${studyResultUuid}/batch/open")
    ).exec(session => if (session.isFailed) session else session.set("batchOpen", true)).exec(failFast)
    .exec(
      http("Heartbeat").post("/publix/${studyResultUuid}/heartbeat").headers(header_ajax).check(status.is(200))
    ).exec(failFast)
    .exec(
      ws("Join group").wsName("groupChannel").connect("/publix/${studyResultUuid}/group/join")
    ).exec(session => if (session.isFailed) session else session.set("groupOpen", true)).exec(failFast).pause(5 seconds)
    .exec(
      http("Reassign group").get("/publix/${studyResultUuid}/group/reassign").headers(header_ajax).check(status.is(200))
    ).exec(failFast).pause(5 seconds)
    .exec(
      http("Leave group").get("/publix/${studyResultUuid}/group/leave").headers(header_ajax).check(status.is(200))
    ).exec(failFast).pause(5 seconds)
    .exec(
      http("Post study session data")
        .post("/publix/${studyResultUuid}/studySessionData")
        .headers(header_ajax)
        .body(StringBody("""{"testRun":"${testRun}","participant":"${participant}","foo":"bar"}"""))
        .check(status.is(200))
    ).exec(failFast)
    .exec(
      http("Post results")
        .post("/publix/${studyResultUuid}/${componentUuid1}/resultData")
        .headers(header_ajax)
        .body(StringBody(resultBody(componentUuid1, 5000)))
        .check(status.is(200))
    ).exec(failFast)
    .exec(
      http("Finish study").get("/publix/${studyResultUuid}/end").headers(header_ajax).check(status.is(200))
    ).exec(failFast)
    .exec(ws("Close batch channel").wsName("batchChannel").close).exec(session => session.set("batchOpen", false)).exec(failFast)

  setUp(scn.inject(atOnceUsers(users))).protocols(httpProtocol)
    .assertions(global.failedRequests.count.is(0L))
  //  setUp(scn.inject(rampUsersPerSec(0.1) to (0.3) during (600 seconds))).protocols(httpProtocol)
  //  setUp(scn.inject(constantConcurrentUsers(20) during (6000 seconds))).protocols(httpProtocol)
  //  setUp(scn.inject(rampConcurrentUsers(0) to (100) during (600 seconds))).protocols(httpProtocol)
}

