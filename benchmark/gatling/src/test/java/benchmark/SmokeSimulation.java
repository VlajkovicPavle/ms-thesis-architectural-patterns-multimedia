package benchmark;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.atOnceUsers;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.core.CoreDsl.jsonPath;
import static io.gatling.javaapi.core.CoreDsl.pause;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.RawFileBodyPart;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

public class SmokeSimulation extends Simulation {

  private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");
  private static final String VIDEO_FILE =
      System.getProperty("videoFile", "../data/videos/smoke-720p-10s.mp4");
  private static final String RENDITIONS = System.getProperty("renditions", "SD_360");
  private static final int POLL_ATTEMPTS = Integer.getInteger("pollAttempts", 120);

  private final HttpProtocolBuilder httpProtocol =
      http.baseUrl(BASE_URL).acceptHeader("application/json");

  private final ScenarioBuilder smoke =
      scenario("upload-rendition-poll-smoke")
          .exec(
              http("upload video")
                  .post("/api/v1/video")
                  .bodyPart(
                      RawFileBodyPart("file", VIDEO_FILE)
                          .fileName(Path.of(VIDEO_FILE).getFileName().toString())
                          .contentType("video/mp4"))
                  .asMultipartForm()
                  .check(status().is(201), jsonPath("$.id").saveAs("videoId")))
          .exec(
              http("request renditions")
                  .post("/api/v1/rendition")
                  .header("Content-Type", "application/json")
                  .body(StringBody(this::renditionRequestBody))
                  .check(status().in(200, 202)))
          .repeat(POLL_ATTEMPTS, "pollAttempt")
          .on(
              exec(
                      http("poll renditions")
                          .get("/api/v1/rendition/video/#{videoId}")
                          .check(status().is(200), jsonPath("$[*].status").findAll().saveAs("statuses")))
                  .exec(this::markIfFinished)
                  .doIf(session -> !Boolean.TRUE.equals(session.getBoolean("renditionsFinished")))
                  .then(pause(Duration.ofSeconds(1))))
          .exec(
              session ->
                  Boolean.TRUE.equals(session.getBoolean("renditionsFinished"))
                      ? session
                      : session.markAsFailed());

  public SmokeSimulation() {
    setUp(smoke.injectOpen(atOnceUsers(1)))
        .protocols(httpProtocol)
        .assertions(global().failedRequests().count().is(0L));
  }

  private String renditionRequestBody(io.gatling.javaapi.core.Session session) {
    String resolutionsJson =
        Arrays.stream(RENDITIONS.split(","))
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .map(value -> "\"" + value + "\"")
            .reduce((left, right) -> left + "," + right)
            .orElse("\"SD_360\"");
    return "{\"videoId\":\"" + session.getString("videoId") + "\",\"resolutions\":[" + resolutionsJson + "]}";
  }

  private io.gatling.javaapi.core.Session markIfFinished(io.gatling.javaapi.core.Session session) {
    List<String> statuses = session.getList("statuses");
    boolean finished = !statuses.isEmpty() && statuses.stream().allMatch("FINISHED"::equals);
    return session.set("renditionsFinished", finished);
  }
}
