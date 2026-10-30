import io.gatling.core.Predef._
import io.gatling.http.Predef._
import java.util.UUID

trait CheckedJatos {
  val host = sys.props.getOrElse("host", "www.example.com")
  val scheme = sys.props.getOrElse("scheme", "https")
  require(Set("http", "https").contains(scheme), "scheme must be http or https")
  require(host.nonEmpty && !host.contains("://") && !host.exists(c => c.isWhitespace || "/?#".contains(c)),
    "host must be a hostname or IP address with optional port, without scheme or path")
  val baseUrl = s"$scheme://$host"
  val wsBaseUrl = s"${if (scheme == "https") "wss" else "ws"}://$host"
  val studyCode = sys.props.getOrElse("studyCode", "obnfh5JzaLW")
  val componentUuid1 = sys.props.getOrElse("componentUuid1", "887e0913-d6ad-4620-b791-d85650ce5de6")
  val componentUuid2 = sys.props.getOrElse("componentUuid2", "2997d569-25a6-4ebb-9372-d87369181438")
  val testRun = sys.props.getOrElse("testRun", UUID.randomUUID().toString)
  val users = sys.props.getOrElse("users", "1").toInt
  val appendCount = sys.props.getOrElse("appendCount", "100").toInt
  val appendBytes = sys.props.getOrElse("appendBytes", "5000").toInt
  require(appendBytes > 0, "appendBytes must be positive")
  require(users > 0 && appendCount > 0)
  require(testRun.matches("[A-Za-z0-9._-]+"), "testRun must contain only letters, digits, dot, underscore, or hyphen")
  val expectedStudyUuid = sys.props.getOrElse("expectedStudyUuid", "d8c9a733-1cc1-4fa6-9cb1-f95faf57072f")

  val identify = exec(session => session
    .set("testRun", testRun)
    .set("participant", UUID.randomUUID().toString)
    .set("seq", 0)
    .set("batchOpen", false)
    .set("groupOpen", false))

  // Close established sockets on failure, preserve failure, and skip normal completion.
  val failFast = doIf(session => session.isFailed) {
    doIf(session => session("batchOpen").as[Boolean]) {
      exec(ws("Cleanup batch channel").wsName("batchChannel").close)
    }.doIf(session => session("groupOpen").as[Boolean]) {
      exec(ws("Cleanup group channel").wsName("groupChannel").close)
    }.exec(session => session.markAsFailed).exitHereIfFailed
  }

  // JSON Lines preserve each append as a separate, identifiable record.
  // Padding keeps the requested total payload size whenever the metadata fits.
  def resultBody(component: String, size: Int, exactSize: Boolean = false): io.gatling.core.session.Expression[String] = session => {
    val prefix = s"""{"testRun":"${session("testRun").as[String]}","participant":"${session("participant").as[String]}","studyResultUuid":"${session("studyResultUuid").as[String]}","component":"$component","seq":${session("seq").as[Int]},"padding":""""
    val suffix = "\"}\n"
    val metadataBytes = (prefix + suffix).getBytes(java.nio.charset.StandardCharsets.UTF_8).length
    if (exactSize && size < metadataBytes)
      io.gatling.commons.validation.Failure(s"appendBytes=$size is too small: this record requires at least $metadataBytes bytes for metadata")
    else
      io.gatling.commons.validation.Success(prefix + ("x" * math.max(0, size - metadataBytes)) + suffix)
  }
}
