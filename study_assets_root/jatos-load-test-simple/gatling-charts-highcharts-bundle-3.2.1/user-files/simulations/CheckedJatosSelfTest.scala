import io.gatling.core.Predef._
import com.fasterxml.jackson.databind.ObjectMapper
import java.util.concurrent.atomic.AtomicInteger

class CheckedJatosSelfTest extends Simulation with CheckedJatos {
  val verified = new AtomicInteger(0)
  val reachedAfterFailure = new AtomicInteger(0)
  val scn = scenario("Payload and failure guard self-test")
    .exec(identify)
    .exec(session => session.set("studyResultUuid", "test-result").set("seq", 2))
    .exec(session => {
      val body = resultBody("component-test", appendBytes, exactSize = true)(session).toOption.get
      val parsed = new ObjectMapper().readTree(body)
      require(body.getBytes(java.nio.charset.StandardCharsets.UTF_8).length == appendBytes && body.endsWith("\n"))
      require(resultBody("component-test", 1, exactSize = true)(session).toOption.isEmpty)
      require(parsed.get("participant").asText == session("participant").as[String])
      require(parsed.get("testRun").asText == testRun)
      require(parsed.get("studyResultUuid").asText == "test-result")
      require(parsed.get("component").asText == "component-test")
      require(parsed.get("seq").asInt == 2)
      verified.incrementAndGet()
      session.markAsFailed
    })
    .exec(failFast)
    .exec(session => { reachedAfterFailure.incrementAndGet(); session })
  setUp(scn.inject(atOnceUsers(1)))
  after {
    require(verified.get == 1, "Payload verification did not execute")
    require(reachedAfterFailure.get == 0, "Failed participant continued")
    println("PASS: JSON payload fields, size, newline, and fail-fast guard (no network requests)")
  }
}
